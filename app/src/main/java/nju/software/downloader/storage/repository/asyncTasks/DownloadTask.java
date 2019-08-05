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

public class DownloadTask implements Runnable {
    private TaskListLiveData taskListLiveData ;
    private TaskDao taskDao;
    private File saveDir ;
    private TaskInfo taskInfo ;
    private static final String LOG_TAG = DownloadTask.class.getSimpleName() ;
    public DownloadTask(TaskDao taskDao, File saveDir, TaskListLiveData taskListLiveData,TaskInfo taskInfo) {
        this.taskListLiveData = taskListLiveData ;
        this.taskDao = taskDao;
        this.saveDir = saveDir ;
        this.taskInfo = taskInfo ;
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

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            long beginPosition = saveFile.length();
            connection.setRequestProperty("Range", "bytes=" + beginPosition + "-" + fileLength);

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
                    taskInfo.setSpeed(Constant.PAUSE);
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
                    taskListLiveData.updateValue(taskInfo);
                }
            }
            //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
            taskInfo.setProgress(100);
            taskInfo.setFinished(true);
            taskListLiveData.updateValue(taskInfo);

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
                    taskInfo.setSpeed(Constant.PAUSE);
                    taskDao.update(taskInfo);
                    return ;
                }
                num += count ;
                output.write(data, 0, count);
                // 更新进度条,暂不更新数据库，等退出或者结束的时候一起更新,这样虽然可能导致进度条和真是下载长度不一致，但问题不大
                if (fileLength > 0 && num>fileLength/ Constant.TIMES_UPDATE_PROGRESS) { // only if total length is known
                    endTime = System.currentTimeMillis() ;
                    total += num ;
                    long speed = num/((endTime-beginTime)/1000) ;
                    if(speed>Constant.GB){
                        taskInfo.setSpeed(speed/Constant.GB+"GB/s");
                    }else if(speed>Constant.MB){
                        taskInfo.setSpeed(speed/Constant.MB+"MB/s");
                    }else if(speed>Constant.KB){
                        taskInfo.setSpeed(speed/Constant.KB+"KB/s");
                    }else {
                        taskInfo.setSpeed(speed+"B/s");
                    }
                    taskInfo.setProgress((int) (total * 100 / fileLength));
                    num = 0;
                    taskListLiveData.updateValue(taskInfo);
                }
            }
            //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
            taskInfo.setProgress(100);
            taskInfo.setFinished(true);
            taskListLiveData.updateValue(taskInfo);

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
}
