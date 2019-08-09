package nju.software.downloader.model.repository;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import nju.software.downloader.model.repository.TaskManager.NetworkTaskManager;
import nju.software.downloader.model.repository.TaskManager.dbTasks.DBTaskManager;
import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.page.taskList.TaskViewModel;
import nju.software.downloader.util.Constant;
import nju.software.downloader.util.FileUtil;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private DBTaskManager dbTaskManager ;
    private NetworkTaskManager networdNetworkTaskManager;
    private TaskViewModel viewModel ;
    private File saveDir ;

    private static String LOG_TAG = TaskRepository.class.getSimpleName();
    public TaskRepository(Application application, TaskViewModel taskViewModel) {
        //业务层确定存在哪里
        saveDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ;
        //初始化任务管理器
        dbTaskManager = new DBTaskManager(application,this) ;
        networdNetworkTaskManager = new NetworkTaskManager(this,dbTaskManager) ;

        init() ;

    }

    private void init(){
        //数据库查询所有的任务列表
        dbTaskManager.getAllTasks();
    }
    public void afterFindAll(List<TaskInfo> taskInfos) {
        //重启任务
        networdNetworkTaskManager.restartTasks(taskInfos);
    }

    /**
     * 监听未完成任务队列
     * @return
     */
    public LiveData<List<TaskInfo>> getUnfinishedTasks() {
        return networdNetworkTaskManager.getUnfinishedTasks();
    }

    /**
     * 监听已完成任务队列
     * @return
     */
    public LiveData<List<TaskInfo>> getFinishedTasks(){
        return networdNetworkTaskManager.getFinishedTasks() ;
    }

    //插入任务，使用异步线程插入数据库
    public void insert(String url) {
        //解析url
        try {
            URL format_url = null;
            format_url = new URL(url);
            String fileName = format_url.getFile();
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            File saveFile = new File(saveDir, fileName);
            //如果文件名重复
            int index = 1;
            while (saveFile.exists()) {
                fileName = FileUtil.increaseFileName(fileName, index) ;
                saveFile = new File(saveDir, fileName);
                index++;
            }
            TaskInfo taskInfo = new TaskInfo(url, fileName,saveFile);
            viewModel.addTaskInfo(taskInfo);
            dbTaskManager.insert(taskInfo);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            //正常情况不应该到达这里，前端需要做url的规范检测
            Log.d(LOG_TAG, "url格式错误，无法下载:" + url);
        }
    }
    //在记录插入数据库之后，才开始真正的下载
    public void afterInsert(TaskInfo taskInfo) {
        networdNetworkTaskManager.addTask(taskInfo);
    }

    /**
     * 更新配置：最大任务数
     * @param max_connection_number
     */
    public void changeMaxTaskNumbers(int max_connection_number) {
        networdNetworkTaskManager.updateMaxTask(max_connection_number);
    }

    /**
     * 批量删除
     */
    public void multiDelete() {
        networdNetworkTaskManager.multiDelete() ;
    }
    public void afterMultiDelete(List<TaskInfo> toDeleteTasks) {
        dbTaskManager.multiDelete(toDeleteTasks);
    }


    //删除单个
    public void delete(TaskInfo task,int taskFlag) {
        if(taskFlag==Constant.UNFINISHED_FLAG) {
            networdNetworkTaskManager.cancelTask(task);
            dbTaskManager.delete(task);
        }else {
            networdNetworkTaskManager.delete(task);
            dbTaskManager.delete(task);
        }
    }

    //暂停，开始
    public void pauseOrBegin(int postiton) {
        networdNetworkTaskManager.pauseOrBegin(postiton) ;
    }

    public void selectTask(int position,int taskFlag) {
        networdNetworkTaskManager.selectTask(position,taskFlag) ;
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
        networdNetworkTaskManager.moveTask(oldPosition,targetPosition) ;
    }


    public void update(Long id, int progress, String speed) {
        viewModel.update(id,progress,speed) ;
    }
}