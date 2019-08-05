package nju.software.downloader.storage.repository;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.List;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.storage.repository.asyncTasks.DownloadTask;
import nju.software.downloader.storage.repository.asyncTasks.GetAllAsync;
import nju.software.downloader.storage.repository.asyncTasks.deleteAllTask;
import nju.software.downloader.storage.repository.asyncTasks.deleteSingleTask;
import nju.software.downloader.storage.room.TaskRoomDatabase;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private TaskDao taskDao;
    private volatile TaskListLiveData taskList = new TaskListLiveData();
    private static File saveDir ;
    private static String LOG_TAG = TaskRepository.class.getSimpleName() ;

    public TaskRepository(Application application){
        //下载保存到外村Download目录下
        saveDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ;
        Log.d(LOG_TAG,"存储目录："+saveDir.getAbsolutePath()) ;

        TaskRoomDatabase db = TaskRoomDatabase.getDatabsae(application) ;
        taskDao = db.taskDao() ;

        //初始化taskList
        new GetAllAsync(taskDao, taskList).execute();

    }

    //LiveData room自动启动worker线程获取数据
    public MutableLiveData<List<TaskInfo>> getAllFiles(){
        return taskList ;
    }

    //插入任务，使用异步线程
    public void insert(TaskInfo taskInfo){

        DownloadTask downloadTask = new DownloadTask(taskDao,saveDir,taskList);
        downloadTask.execute(taskInfo) ;
        taskInfo.setTaskThraed(downloadTask);
    }


    //删除所有
    public void deleteAll(){
        new deleteAllTask(taskDao).execute() ;
    }

    //删除单个
    public void delete(TaskInfo task){

        //判断是否已经下载完成，如果为完成，则取消线程
        AsyncTask taskThraed = task.getTaskThraed();
        if(taskThraed!=null && taskThraed.getStatus()!= AsyncTask.Status.FINISHED
                && taskThraed.cancel(true)){
            new deleteSingleTask(taskList,taskDao,saveDir).execute(task) ;
        }else {
            //如果已经完成，只需要直接删除
            //启用额外线程删除数据库数据
            new deleteSingleTask(taskList,taskDao,saveDir).execute(task) ;
        }
    }

    public void pauseOrBegin(TaskInfo task) {

        if(task.isFinished())
            return;

        if (task.isPaused()) {
            //原先是暂停状态
            /**
             * 进入等待下载状态 起一个后台任务
             */
        }else{
            //原先是下载状态
            AsyncTask taskThraed = task.getTaskThraed();
            //判断是否已经下载完成，如果为完成，则取消线程
            if(taskThraed!=null && taskThraed.getStatus()!= AsyncTask.Status.FINISHED) {
                taskThraed.cancel(true);
            }
            task.setPaused(true);
            taskList.updateValue(task);
        }

    }
}
