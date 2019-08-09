package nju.software.downloader.repository.network.asyncTasks;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.database.DBTaskManager;
import nju.software.downloader.util.Constant;

public class DownloadTask implements Runnable,Comparable<DownloadTask>{
    private TaskListLiveData unfinishedTaskListLiveData;
    private TaskListLiveData finishedTaskListLiveData ;
    private DBTaskManager dbTaskManager ;
    private File saveDir ;
    private TaskInfo taskInfo ;

    //运行状态0标识等待，1标识执行，2标识暂停，3标识取消，4标识完成，状态要和taskInfo保持一致，自己不可以变
    private volatile int status ;

    //任务总共五个状态
    public static final int WAITTING = 0 ;
    public static final int RUNNING = 1 ;
    public static final int PAUSE = 2 ;
    public static final int FINISHED = 3 ;
    public static final int CANCEL = 4 ;


    private static final String LOG_TAG = DownloadTask.class.getSimpleName() ;

    public DownloadTask(DBTaskManager dbTaskManager, File saveDir, TaskInfo taskInfo, TaskListLiveData unfinishedTaskListLiveData, TaskListLiveData finishedTaskListLiveData) {
        this.dbTaskManager = dbTaskManager ;
        this.saveDir = saveDir ;

        this.unfinishedTaskListLiveData = unfinishedTaskListLiveData;
        this.finishedTaskListLiveData = finishedTaskListLiveData ;

        //相互引用
        this.taskInfo = taskInfo ;
        taskInfo.setDownloadTask(this);

        //初始为waitting态,
        status = WAITTING ;
    }

    @Override
    public void run() {
        //只能从WAIRTTING状态进入
        if(status!=WAITTING){
            return;
        }
        File saveFile = new File(saveDir,taskInfo.getFileName()) ;
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile rwd = null ;
        OutputStream output = null;
        try {
            URL url = new URL(taskInfo.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            long beginPosition = saveFile.length();
            //是否断点续传
            boolean isResume=false ;

            if(beginPosition>0){
                try {
                    connection.setRequestProperty("Range", "bytes=" + beginPosition + "-");
                    connection.connect();
                    Log.d(LOG_TAG, "网络连接成功");
                    connection.setRequestProperty("Range", "bytes=" + beginPosition + "-");
                    input = connection.getInputStream();
                    //断点续传
                    isResume = true ;
                    rwd = new RandomAccessFile(saveFile, "rwd");
                } catch (IllegalStateException e) {
                    //有可能不支持断点续传
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    input = connection.getInputStream() ;
                    isResume = false ;
                    output = new FileOutputStream(saveFile) ;
                }
            }else {
                input = connection.getInputStream() ;
                isResume = false ;
                output = new FileOutputStream(saveFile) ;
            }
            int fileLength  = connection.getContentLength();

            byte data[] = new byte[4096];
            //一次更新进度之内的下载量
            long num = 0;

            //总体下载量
            long total = beginPosition;

            //单个循环下载量
            int count;

            long beginTime = System.currentTimeMillis() ;
            long endTime ;

            //进度更新量
            int changeProgress;
            while ((count = input.read(data)) != -1) {
                if (status!=RUNNING) {
                    return ;
                }
                // allow canceling
                num += count;
                total += count ;
                if(isResume) {
                    rwd.write(data, 0, count);
                }else {
                    output.write(data, 0, count);
                }
                //跟新进度条，时间间隔
                if (((endTime=System.currentTimeMillis())-beginTime)> Constant.REFRESH_PROGRESS_INTERVAL) { // only if total length is known
                    //如果进度小于1，不更新
                    changeProgress = (int)num*100/fileLength ;
                    if(changeProgress<1){
                        continue;
                    }

                    long _Speed = num/(endTime-beginTime)*1000 ;
                    if(_Speed>Constant.GB){
                        taskInfo.setSpeed(_Speed/Constant.GB+"GB/s");
                    }else if(_Speed>Constant.MB){
                        taskInfo.setSpeed(_Speed/Constant.MB+"MB/s");
                    }else if(_Speed>Constant.KB){
                        taskInfo.setSpeed(_Speed/Constant.KB+"KB/s");
                    }else {
                        if(_Speed==0){
                            taskInfo.setSpeed("- B/s");
                        }else {
                            taskInfo.setSpeed(_Speed + "B/s");
                        }
                    }

                    taskInfo.setProgress((int) (total * 100 / fileLength));
                    num = 0;


                    //更新前再检查一遍
                    if (status!=RUNNING) {
                        return ;
                    }
                    unfinishedTaskListLiveData.updateValue(taskInfo) ;
                    beginTime = System.currentTimeMillis() ;
                }
            }
            //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
            taskInfo.setProgress((int) (total * 100 / fileLength));
            taskInfo.setFinished(true);
            taskInfo.setSpeed(Constant.SPEED_OF_FINISHED);
            unfinishedTaskListLiveData.delete(taskInfo);
            finishedTaskListLiveData.addValue(taskInfo);
            //更新数据库
            dbTaskManager.update(taskInfo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(LOG_TAG,"中途失去网络连接") ;
            e.printStackTrace();
            //把自己置为取消状态
            pause();
        }finally {
            try {
                if (rwd != null)
                    rwd.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }

    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    @Override
    public int compareTo(DownloadTask downloadTask) {
        return taskInfo.compareTo(downloadTask.taskInfo);
    }

    @Override
    public int hashCode() {
        return taskInfo.hashCode();
    }


    ///通过包装的TaskInfo来判断
    @Override
    public boolean equals(Object obj) {
        if(obj==null){
            return false ;
        }
        if(obj instanceof DownloadTask){
            return ((DownloadTask)obj).taskInfo.equals(taskInfo) ;
        }
        return false;
    }

    public int getStatus() {
        return status;
    }

    void setStatus(int status) {
        this.status = status;
    }

    /**
     * 取消线程池中的任务
     */
    public void pause() {
        status = DownloadTask.PAUSE;
    }



    public void cancel() {
        status = DownloadTask.CANCEL;

        //删除已下载文件
        File saveFile = new File(saveDir,taskInfo.getFileName()) ;
        if(saveFile.exists()){
            saveFile.delete() ;
        }
    }
    public void waittting(){
        status = DownloadTask.WAITTING;
        taskInfo.setSpeed(Constant.SPEED_OF_WAITTING);
        unfinishedTaskListLiveData.updateValue(this.taskInfo);
    }
    boolean isWaitting(){
        return status==WAITTING ;
    }

}
