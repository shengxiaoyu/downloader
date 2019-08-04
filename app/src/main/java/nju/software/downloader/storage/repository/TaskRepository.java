package nju.software.downloader.storage.repository;

import android.app.Application;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.storage.room.TaskRoomDatabase;
import nju.software.downloader.util.FileUtil;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private TaskDao taskDao;
    private LiveData<List<TaskInfo>> fileList ;
    private static File saveDir ;
    private static String LOG_TAG = TaskRepository.class.getSimpleName() ;
    public TaskRepository(Application application){
        saveDir = new File(application.getFilesDir(),"Donwload");
        saveDir.mkdirs() ;
        Log.d(LOG_TAG,"初始化存储目录："+saveDir.getAbsolutePath()) ;

        TaskRoomDatabase db = TaskRoomDatabase.getDatabsae(application) ;
        taskDao = db.wordDao() ;
        fileList = taskDao.getAllWords() ;
    }

    //LiveData room自动启动worker线程获取数据
    public LiveData<List<TaskInfo>> getAllFiles(){
        return taskDao.getAllWords() ;
    }

    //插入任务，使用异步线程
    public void insert(TaskInfo taskInfo){
        new DownloadTask(taskDao).execute(taskInfo) ;
    }

    private static class DownloadTask extends AsyncTask<TaskInfo, Void, Void> {

        private TaskDao taskDao;

        DownloadTask(TaskDao taskDao) {
            this.taskDao = taskDao;
        }

        @Override
        protected Void doInBackground(TaskInfo... taskInfos) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            TaskInfo taskInfo = taskInfos[0] ;
            //先将任务插入数据库

            try {
                URL url = new URL(taskInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }
                Log.d(LOG_TAG,"网络连接成功") ;
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // 通过Content-Disposition获取文件名
                String fileName = connection.getHeaderField("Content-Disposition");
                if (fileName == null || fileName.length() < 1) {
                    // 通过截取URL来获取文件名
                    URL downloadUrl = connection.getURL();
                    // 获得实际下载文件的URL
                    fileName = downloadUrl.getFile();
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
               } else {
                    fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename=") + 9), "UTF-8");
                    // 存在文件名会被包含在""里面，所以要去掉，否则读取异常
                    fileName = fileName.replaceAll("\"", "");
                }

                File saveFile = new File(saveDir,fileName) ;

                //如果文件名重复
                int index = 1 ;
                while (saveFile.exists()){
                    saveFile = new File(saveDir, FileUtil.increaseFileName(fileName,index)) ;
                    index++ ;
                }
                taskInfo.setFileName(saveFile.getName());
                long id = taskDao.insert(taskInfo);
                taskInfo.setId(id);
                input = connection.getInputStream();



                output = new FileOutputStream(saveFile) ;
                Log.d(LOG_TAG,"下载保存地址："+saveFile.getAbsolutePath()) ;

                //进度总分为20份，每一份更新一次,每百分之五更新一次,减少更新频次
                byte data[] = new byte[4096];
                //一次之内的下载量
                long num = 0 ;
                //总体下载量
                long total = 0;
                //单个循环下载量
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    num += count ;
                    output.write(data, 0, count);


                    // 更新进度条
                    if (fileLength > 0 && num>fileLength/20) { // only if total length is known
                        total += num ;
                        taskInfo.setProgress((int) (total * 100 / fileLength));
                        num = 0;
                        taskDao.update(taskInfo);
                    }
                }
                //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
                taskInfo.setProgress(100);
                taskDao.update(taskInfo);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG,e.getMessage()) ;
            } finally {
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
            return null;
        }

    }

    public void deleteAll(){
        new deleteAllAsyncTask(taskDao).execute() ;
    }
    //删除全部
    private static class deleteAllAsyncTask extends AsyncTask<Void,Void,Void>{
        private TaskDao taskDao;
        @Override
        protected Void doInBackground(Void... voids) {
            taskDao.deleteAll();
            return null;
        }
        deleteAllAsyncTask(TaskDao dao){
            this.taskDao = dao ;
        }
    }

    //删除单个
    public void delete(TaskInfo file){
        new deleteSingleFile(taskDao).execute(file) ;
    }
    private static class deleteSingleFile extends AsyncTask<TaskInfo,Void,Void>{
        private TaskDao taskDao;
        @Override
        protected Void doInBackground(TaskInfo... taskInfos) {
            taskDao.delete(taskInfos[0]);
            return null;
        }
        deleteSingleFile(TaskDao taskDao){
            this.taskDao = taskDao;
        }
    }
}
