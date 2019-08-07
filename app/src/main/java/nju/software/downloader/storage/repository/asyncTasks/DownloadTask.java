package nju.software.downloader.storage.repository.asyncTasks;

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
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.util.Constant;

public class DownloadTask implements Runnable,Comparable<DownloadTask>{
    private TaskListLiveData unfinishedTaskListLiveData;
    private TaskListLiveData finishedTaskListLiveData ;
    private TaskDao taskDao;
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

    public DownloadTask(TaskDao taskDao, File saveDir, TaskListLiveData unfinishedTaskListLiveData, TaskInfo taskInfo,TaskListLiveData finishedTaskListLiveData) {
        this.unfinishedTaskListLiveData = unfinishedTaskListLiveData;
        this.taskDao = taskDao;
        this.saveDir = saveDir ;
        this.finishedTaskListLiveData = finishedTaskListLiveData ;

        //相互引用
        this.taskInfo = taskInfo ;
        taskInfo.setDownloadTask(this);
        taskInfo.setSpeed(Constant.SPEED_OF_WAITTING);
        //初始为waitting态
        status = WAITTING ;
    }

    @Override
    public void run() {
        File saveFile = new File(saveDir,taskInfo.getFileName()) ;
        if(saveFile.exists()){
            try {
                resume(saveFile);
            }catch (IllegalStateException e){
                //不支持断点重传，
                downloadDirectly(saveFile);
            }
        }else {
            downloadDirectly(saveFile);
        }
    }

    private void resume(File saveFile) throws IllegalStateException {
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile rwd = null ;
        try {
            URL url = new URL(taskInfo.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }
            Log.d(LOG_TAG, "网络连接成功");

            // this will be useful to  display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            long beginPosition = saveFile.length();
            connection.setRequestProperty("Range", "bytes=" + beginPosition + "-");

            try {
                input = connection.getInputStream();
                //断点续传
            } catch (IllegalStateException e) {
                //有可能不支持断点续传
                throw e;
            }

            rwd = new RandomAccessFile(saveFile, "rwd");
            byte data[] = new byte[4096];
            //一次之内的下载量
            long num = 0;
            //总体下载量
            long total = beginPosition;
            //单个循环下载量
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling
                if (Thread.currentThread().isInterrupted()) {
                    this.status = 2 ;
                    taskInfo.setSpeed(Constant.SPEED_OF_PAUSE);
                    taskDao.update(taskInfo);
                    return;
                }
                num += count;
                rwd.write(data, 0, count);

                // 更新进度条,暂不更新数据库，等退出或者结束的时候一起更新,这样虽然可能导致进度条和真是下载长度不一致，但问题不大
                if (fileLength > 0 && num > fileLength / 20) { // only if total length is known
                    total += num;
                    taskInfo.setProgress((int) (total * 100 / fileLength));
                    num = 0;
                    unfinishedTaskListLiveData.updateValue(taskInfo);
                }
            }
            //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
            taskInfo.setProgress(100);
            taskInfo.setFinished(true);
            unfinishedTaskListLiveData.updateValue(taskInfo);

            //更新数据库
            taskDao.update(taskInfo);
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

    private void downloadDirectly(File saveFile){

        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            URL url = new URL(taskInfo.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }
            Log.d(LOG_TAG,"网络连接成功") ;

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            input = connection.getInputStream();
            //为了避免RandomAccessFile性能影响，如果之前没有下载过使用简单的顺序写入
            output = new FileOutputStream(saveFile) ;
            Log.d(LOG_TAG,"下载保存地址："+saveFile.getAbsolutePath()) ;

            byte[] data = new byte[4096];
            long num = 0 ;
            //总体下载量
            long total = 0;
            int count;
            long beginTime = System.currentTimeMillis() ;
            long endTime = beginTime ;
            while ((count = input.read(data)) != -1) {
                // allow canceling
                if (Thread.currentThread().isInterrupted()) {
                    taskDao.update(taskInfo);
                    return ;
                }
                num += count ;
                //在这里更新总量，避免最后一次不进入下面的if
                total += count ;
                output.write(data, 0, count);
                // 更新进度条,暂不更新数据库，等退出或者结束的时候一起更新,这样虽然可能导致进度条和真是下载长度不一致，但问题不大
                if (fileLength > 0 && num>fileLength/ Constant.TIMES_UPDATE_PROGRESS) { // only if total length is known
                    endTime = System.currentTimeMillis() ;
                    long speed = 0 ;
                    if(endTime!=beginTime){
                        speed = num/(1+(endTime-beginTime)/1000) ;
                    }
                    if(speed>Constant.GB){
                        taskInfo.setSpeed(speed/Constant.GB+"GB/s");
                    }else if(speed>Constant.MB){
                        taskInfo.setSpeed(speed/Constant.MB+"MB/s");
                    }else if(speed>Constant.KB){
                        taskInfo.setSpeed(speed/Constant.KB+"KB/s");
                    }else {
                        if(speed==0){
                            taskInfo.setSpeed("- B/s");
                        }else {
                            taskInfo.setSpeed(speed + "B/s");
                        }
                    }
                    taskInfo.setProgress((int) (total * 100 / fileLength));
                    num = 0;
                    unfinishedTaskListLiveData.updateValue(taskInfo);

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
            taskDao.update(taskInfo);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }finally {
            try {
                if (output != null)
                    output.close();
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
        taskInfo.setDownloadTask(null);
        runningThread = null ;
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
        taskInfo.setDownloadTask(null);
        status = DownloadTask.DELETE;
    }
}
