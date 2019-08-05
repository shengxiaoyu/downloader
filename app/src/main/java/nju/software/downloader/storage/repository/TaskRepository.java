package nju.software.downloader.storage.repository;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.storage.repository.asyncTasks.DownloadTask;
import nju.software.downloader.storage.repository.asyncTasks.GetAllAsync;
import nju.software.downloader.storage.repository.asyncTasks.DeleteAllTask;
import nju.software.downloader.storage.repository.asyncTasks.DeleteSingleTask;
import nju.software.downloader.storage.room.TaskRoomDatabase;
import nju.software.downloader.util.Constant;
import nju.software.downloader.util.FileUtil;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private TaskDao taskDao;
    private volatile TaskListLiveData taskList = new TaskListLiveData();
    private static File saveDir;
    private static String LOG_TAG = TaskRepository.class.getSimpleName();

    private static ExecutorService threadPoolExecutor;


    public TaskRepository(Application application) {
        //下载保存到外村Download目录下
        saveDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Log.d(LOG_TAG, "存储目录：" + saveDir.getAbsolutePath());

        TaskRoomDatabase db = TaskRoomDatabase.getDatabsae(application);
        taskDao = db.taskDao();

        //初始化taskList
        new GetAllAsync(taskDao, taskList).execute();

        threadPoolExecutor = Executors.newFixedThreadPool(Constant.MAX_TASKS);
    }

    //LiveData room自动启动worker线程获取数据
    public LiveData<List<TaskInfo>> getAllFiles() {
        return taskList;
    }

    //插入任务，使用异步线程
    public void insert(TaskInfo taskInfo) {
        new addTask().execute(taskInfo) ;
    }


    //删除所有
    public void deleteAll() {
        new DeleteAllTask(taskDao).execute();
    }

    //删除单个
    public void delete(TaskInfo task) {

        //判断是否已经下载完成，如果未完成，则取消或中断任务
        Future taskThraed = task.getTaskThraed();
        if (taskThraed != null && !taskThraed.isDone()) {
            taskThraed.cancel(true);
            new DeleteSingleTask(taskList, taskDao, saveDir).execute(task);
        } else {
            //如果已经完成，只需要直接删除
            //启用额外线程删除数据库数据
            new DeleteSingleTask(taskList, taskDao, saveDir).execute(task);
        }
    }

    public void pauseOrBegin(TaskInfo task) {

        if (task.isFinished())
            return;

        if (task.isPaused()) {
            //原先是暂停状态
            /**
             * 重新进入下载队列
             */
            Future future = threadPoolExecutor.submit(new DownloadTask(taskDao, saveDir, taskList, task));
            task.setTaskThraed(future);
            task.setPaused(false);
            task.setSpeed(Constant.WAITTING);
            taskList.updateValue(task);
        } else {
            //原先是下载状态
            Future taskThraed = task.getTaskThraed();

            //判断是否已经下载完成，如果未完成，则取消线程
            if (taskThraed != null && !taskThraed.isDone()) {
                taskThraed.cancel(true);
                task.setPaused(true);
                task.setSpeed(Constant.PAUSE);
                taskList.updateValue(task);
            }
        }

    }

    public void selectTask(TaskInfo taskInfo) {
        taskInfo.setSelected(!taskInfo.isSelected());
        taskList.updateValue(taskInfo);
    }

    public class addTask extends AsyncTask<TaskInfo, Void, TaskInfo> {

        @Override
        protected TaskInfo doInBackground(TaskInfo... taskInfos) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            TaskInfo taskInfo = taskInfos[0];

            boolean canConnection = true ;

            //获取文件名，确定文件存储路径
            try {
                URL url = new URL(taskInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    canConnection = false ;
                }else {
                    //获取文件名
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
                    File saveFile = new File(saveDir, fileName);
                    //如果文件名重复
                    int index = 1;
                    while (saveFile.exists()) {
                        saveFile = new File(saveDir, FileUtil.increaseFileName(fileName, index));
                        index++;
                    }
                    taskInfo.setFileName(saveFile.getName());
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //更新task并通知前端
            long id = taskDao.insert(taskInfo);
            taskInfo.setId(id);
            taskList.addValue(taskInfo);

            if(!canConnection){
                return null ;
            }
            return taskInfo ;
        }


        //只有先将任务加到数据库和前端展示开始了，才真正的开始网络下载部分
        @Override
        protected void onPostExecute(TaskInfo taskInfo) {
            super.onPostExecute(taskInfo);
            if (taskInfo==null){
                //说明网络连不上
                return;
            }
            Future future = threadPoolExecutor.submit(new DownloadTask(taskDao, saveDir, taskList, taskInfo));
            taskInfo.setTaskThraed(future);
        }
    }
}