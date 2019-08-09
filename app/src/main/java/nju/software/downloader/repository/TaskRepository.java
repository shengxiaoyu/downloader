package nju.software.downloader.repository;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.database.DBTaskManager;
import nju.software.downloader.repository.network.NetworkTaskManager;
import nju.software.downloader.util.Constant;
import nju.software.downloader.util.FileUtil;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private volatile TaskListLiveData unfinishedTaskListLiveData ;
    private volatile TaskListLiveData finishedTaskListLiveData ;
    private static File saveDir;

    private DBTaskManager dbTaskManager ;
    private NetworkTaskManager networkTaskManager ;

    private static String LOG_TAG = TaskRepository.class.getSimpleName();


    public TaskRepository(Application application) {
        //下载保存到外村Download目录下
        saveDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Log.d(LOG_TAG, "存储目录：" + saveDir.getAbsolutePath());

        dbTaskManager = new DBTaskManager(application) ;
        networkTaskManager = new NetworkTaskManager(this,dbTaskManager,Constant.MAX_TASKS,saveDir) ;

        init() ;
    }

    /**
     * 初始化工作，从数据库获取下载列表，重启下载或展示完成列表
     */
    private void init() {
        dbTaskManager.getAllTasks();
    }


    //LiveData room自动启动worker线程获取数据
    public LiveData<List<TaskInfo>> getUnfinishedTasks() {
        this.unfinishedTaskListLiveData = networkTaskManager.getUnfinishedTaskListLiveData() ;
        return unfinishedTaskListLiveData;
    }

    public LiveData<List<TaskInfo>> getFinishedTasks(){
        this.finishedTaskListLiveData = networkTaskManager.getFinishedTaskListLiveData() ;
        return finishedTaskListLiveData ;
    }


    //插入任务，使用异步线程
    public void insert(TaskInfo taskInfo) {
        //解析url
        String url = taskInfo.getUrl() ;
        try {
            URL format_url = new URL(url);
            String fileName = format_url.getFile();
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            File saveFile = new File(saveDir, fileName);
            //如果文件名重复
            int index = 1;
            String tmpFilename = fileName;
            while (saveFile.exists()) {
                tmpFilename = FileUtil.increaseFileName(fileName, index) ;
                saveFile = new File(saveDir, tmpFilename);
                index++;
            }
            taskInfo.setFileName(tmpFilename);
            //前端展示
            unfinishedTaskListLiveData.addValue(taskInfo);
            //后台开始存数据库和下载
            dbTaskManager.insert(taskInfo);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            //正常情况不应该到达这里，前端需要做url的规范检测
            Log.d(LOG_TAG, "url格式错误，无法下载:" + url);
        }
    }

    public void changeMaxTaskNumbers(int max_connection_number) {
        networkTaskManager.updateMaxTask(max_connection_number);
    }

    public void multiDelete() {
        networkTaskManager.multiDelete();
    }


    //删除单个
    public void delete(TaskInfo task,int taskFlag) {
        if(taskFlag==Constant.UNFINISHED_FLAG) {
            //数据库内容、已下载文件，并停止任务
            networkTaskManager.delete(task,taskFlag);
        }else {
            //删除缓存数据、数据库内容、已下载文件
            dbTaskManager.delete(task);
            finishedTaskListLiveData.delete(task);
        }
    }

    //暂停，开始
    public void pauseOrBegin(int postiton) {
        networkTaskManager.pauseOrBegin(postiton);
    }

    public void selectTask(int position,int taskFlag) {
        if(taskFlag==Constant.UNFINISHED_FLAG){
            TaskInfo taskInfo = unfinishedTaskListLiveData.get(position);
            taskInfo.setSelected(!taskInfo.isSelected());
            unfinishedTaskListLiveData.updateValue(taskInfo);
        }else{
            TaskInfo taskInfo = finishedTaskListLiveData.get(position);
            taskInfo.setSelected(!taskInfo.isSelected());
            finishedTaskListLiveData.updateValue(taskInfo);
        }
    }

    /**
     * 插队实现要做两个事：
     * 1、给任务设置新的优先级
     * 2、是否要进行任务中断、出队再入队的操作：
     *      a)往前插队：如果当前线程处于WAITTING状态，检测插队的前面是否有正在执行的任务，有的话就停止，让出线程；如果当前线程处于RUNNING，不做处理；如果当前线程处于CANCEL，不做处理。
     *      b)往后插队：如果当前线程处于RUNNING状态，检测新位置靠前是否有任务在等待态，如果有，则停止当前任务的执行，让出线程；如果当前线程处于WAITTING状态，重新入队列；如果当前线程处于CANCEL状态，不做处理
     * @param oldPosition
     * @param targetPosition
     */
    public void move(int oldPosition, int targetPosition) {
        networkTaskManager.moveTask(oldPosition,targetPosition);
    }

}