package nju.software.downloader.repository.asyncTasks;

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

/***
 * 下载任务核心类，该类作为下载、实时更新进度、速度的关键类。拥有5个状态：WAITTING,RUNNING,PAUSE,FINISHED,DELETE,
 * 一般一个DownloadTask对应唯一一个TaskInfo，一个TaskInfo可能再整个下载周期内和多个DownloadTask联系，因为任务暂停，重启等都会设计取消下载，重新下载
 */
public class DownloadTask implements Runnable,Comparable<DownloadTask>{
    private TaskListLiveData unfinishedTaskListLiveData;
    private TaskListLiveData finishedTaskListLiveData ;
    private DBTaskManager dbManager;
    private File saveDir ;
    private TaskInfo taskInfo ;
    private static final String LOG_TAG = DownloadTask.class.getSimpleName() ;
    private Thread runningThread ;
    //运行状态0标识等待，1标识执行，2标识暂停，3标识取消，4标识完成
    private volatile int status ;

    //任务总共五个状态
    public static final int WAITTING = 0 ;
    public static final int RUNNING = 1 ;
    public static final int PAUSE = 2 ;
    public static final int FINISHED = 3 ;
    public static final int DELETE = 4 ;

    public DownloadTask(DBTaskManager dbManager, File saveDir, TaskListLiveData unfinishedTaskListLiveData, TaskInfo taskInfo, TaskListLiveData finishedTaskListLiveData) {
        this.unfinishedTaskListLiveData = unfinishedTaskListLiveData;
        this.dbManager = dbManager;
        this.saveDir = saveDir ;
        this.finishedTaskListLiveData = finishedTaskListLiveData ;

        //相互引用
        this.taskInfo = taskInfo ;
        taskInfo.setDownloadTask(this);
        taskInfo.setSpeed(Constant.SPEED_OF_WAITTING);
        //初始为waitting态
        status = WAITTING ;
    }

    /**下载核心过程，需要注意两点：
     *  1、下载过程中实时检测任务状态量volatile status的变化，通过这个状态获取任务是否该被中断，从而实现暂停、取消
     * 2、跟新进度条，通过两个手段控制进度条的更新，减少更新频次，避免界面闪屏。一是固定间隔时间计算一次进度，二是进度低于阈值不更新
     */
    @Override
    public void run() {
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
                if (Thread.currentThread().isInterrupted()||status!=RUNNING) {
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


                if (((endTime=System.currentTimeMillis())-beginTime)> Constant.REFRESH_PROGRESS_INTERVAL) { // only if total length is known
                    //如果进度小于1，不更新
                    changeProgress = (int)num*100/fileLength ;
                    if(changeProgress<Constant.PERRESE_PROGRESS_LOW){
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
                    if (Thread.currentThread().isInterrupted()) {
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
            dbManager.update(taskInfo);
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

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * 取消线程池中的任务
     */
    public void pause() {
        if(runningThread!=null){
            runningThread.interrupt();
        }
        //释放引用
//        taskInfo.setDownloadTask(null);
//        runningThread = null ;
        status = DownloadTask.PAUSE;
    }


    public void setRunningThread(Thread runningThread) {
        this.runningThread = runningThread;
    }

    public void delete() {
        if(runningThread!=null){
            runningThread.interrupt();
        }
        //释放引用
        runningThread = null ;
//        taskInfo.setDownloadTask(null);
//        taskInfo = null ;
        status = DownloadTask.DELETE;
    }
    public void waittting(){
        if(runningThread!=null){
            runningThread.interrupt();
        }
        //释放引用
//        runningThread = null ;
        status = DownloadTask.WAITTING;
        taskInfo.setSpeed(Constant.SPEED_OF_WAITTING);
        unfinishedTaskListLiveData.updateValue(this.taskInfo);
    }
    boolean isWaitting(){
        return status==WAITTING ;
    }

    public boolean isRunning() {
        return status==RUNNING ;
    }
}
