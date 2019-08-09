package nju.software.downloader.repository.network;

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
import java.util.Comparator;

import nju.software.downloader.R;
import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.database.DBTaskManager;
import nju.software.downloader.util.Constant;

public class DownloadTask implements Runnable,Comparable<DownloadTask>{
    private static final String LOG_TAG = DownloadTask.class.getSimpleName() ;
    private TaskInfo taskInfo ;
    private TaskListLiveData unfinishedTaskListLiveData ;
    private TaskListLiveData finishedTaskListLiveData ;
    private DBTaskManager dbTaskManager ;
    private File saveDir ;

    DownloadTask(TaskInfo taskInfo,File saveDir, DBTaskManager taskManager,TaskListLiveData unfinishedTaskListLiveData,TaskListLiveData finishedTaskListLiveData){
        this.taskInfo = taskInfo ;
        this.state = WAITTING ;
        this.unfinishedTaskListLiveData = unfinishedTaskListLiveData ;
        this.finishedTaskListLiveData = finishedTaskListLiveData ;
        this.dbTaskManager = taskManager ;
        this.saveDir = saveDir ;

        taskInfo.setDownloadTask(this);
    }
    /**
     * 标识当前下载任务的状态，
     */
    private volatile int state ;

    /**
     * 任务的六种状态
     */
    private static final int WAITTING = 1 ;
    private static final int RUNNING = 1<<1 ;
    private static final int PAUSE = 1<<2 ;
    private static final int FINISH = 1<<3 ;
    private static final int CANCEL = 1<<4 ;


    @Override
    public void run() {
        state = RUNNING ;
        File saveFile = new File(saveDir,taskInfo.getFileName()) ;
        HttpURLConnection connection = null;
        InputStream input = null;


        RandomAccessFile rwd = null ;
        OutputStream output = null;
        try {
            String url = taskInfo.getUrl();
            URL _URL = new URL(url);
            connection = (HttpURLConnection) _URL.openConnection();

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
                    connection = (HttpURLConnection) _URL.openConnection();
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
            //状态设置为运行
            state = RUNNING ;


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
                if(!isRunning()){
                    //只要不再是运行态，说明线程需要让出
                    return;
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

                    if(!isRunning()){
                        return;
                    }
                    unfinishedTaskListLiveData.updateValue(taskInfo) ;
                    beginTime = System.currentTimeMillis() ;
                }
            }
            //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
            taskInfo.setProgress(100);
            taskInfo.setFinished(true);
            taskInfo.setSpeed(Constant.SPEED_OF_FINISHED);
            finish();
            unfinishedTaskListLiveData.delete(taskInfo);
            finishedTaskListLiveData.addValue(taskInfo);
            //更新数据库
            dbTaskManager.update(taskInfo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    boolean isRunning(){
        return state == RUNNING;
    }

    public void pause(){
        state = PAUSE ;
        taskInfo.setSpeed(Constant.SPEED_OF_PAUSE) ;
    }
    boolean isPause(){
        return state==PAUSE ;
    }

    public void cancel(){
        state = CANCEL ;
    }
    boolean isCancel(){
        return state==CANCEL ;
    }

    void finish(){
        state = FINISH ;
    }
    public boolean isFinished(){
        return state == FINISH;
    }

    public void waitting(){
        taskInfo.setSpeed(Constant.SPEED_OF_WAITTING) ;
        state = WAITTING ;
    }
    public boolean isWaitting(){
        return state==WAITTING;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    @Override
    public int hashCode() {
        return taskInfo.hashCode();
    }

    @Override
    public int compareTo(DownloadTask downloadTask) {
        TaskInfo taskInfo = downloadTask.taskInfo;
        return this.taskInfo.compareTo(taskInfo) ;
    }
}