package nju.software.downloader.repository;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.database.DBTaskManager;
import nju.software.downloader.repository.asyncTasks.CustomerThreadPoolExecutor;
import nju.software.downloader.repository.asyncTasks.DownloadTask;
import nju.software.downloader.util.Constant;
import nju.software.downloader.util.FileUtil;

//封装数据的获取，可以从数据库，从网络
public class TaskRepository {
    private DBTaskManager dbTaskManager;
    private volatile TaskListLiveData unfinishedTaskListLiveData ;
    private volatile TaskListLiveData finishedTaskListLiveData ;
    private static File saveDir;
    private static String LOG_TAG = TaskRepository.class.getSimpleName();

    private CustomerThreadPoolExecutor threadPoolExecutor ;

    public TaskRepository(Application application) {
        //下载保存到外村Download目录下
        saveDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Log.d(LOG_TAG, "存储目录：" + saveDir.getAbsolutePath());

        threadPoolExecutor = new CustomerThreadPoolExecutor(Constant.MAX_TASKS,
                Constant.MAX_TASKS,
                0L,TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>()) ;
        unfinishedTaskListLiveData = new TaskListLiveData();
        finishedTaskListLiveData = new TaskListLiveData() ;

        dbTaskManager = new DBTaskManager(application,this) ;

        //初始化taskList
        dbTaskManager.getAllTasks();


    }

    //LiveData room自动启动worker线程获取数据
    public LiveData<List<TaskInfo>> getUnfinishedTasks() {
        return unfinishedTaskListLiveData;
    }

    public LiveData<List<TaskInfo>> getFinishedTasks(){
        return finishedTaskListLiveData ;
    }


    //重启未完成任务
    public void restartUnfinishedTask(List<TaskInfo> taskIList){
        if(taskIList==null||taskIList.size()==0){
            return;
        }
        for(TaskInfo taskInfo:taskIList) {
            if(!taskInfo.isFinished()) {
                threadPoolExecutor.execute(new DownloadTask(dbTaskManager, saveDir, unfinishedTaskListLiveData, taskInfo,finishedTaskListLiveData));
            }
        }
    }

    //插入任务，使用异步线程
    public void insert(TaskInfo taskInfo) {
        //这里只做更新数据库，更新缓存数据以更新前端展示
        URL url = null;
        try {
            url = new URL(taskInfo.getUrl());
            String fileName = url.getFile();
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            try {
                fileName = URLEncoder.encode(fileName,"utf8") ;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            File saveFile = new File(saveDir, fileName);
            //如果文件名重复
            int index = 1;
            while (saveFile.exists()) {
                saveFile = new File(saveDir, FileUtil.increaseFileName(fileName, index));
                index++;
            }

            //要把这个文件创建存起来，避免后续的文件检查的时候发现没有文件，导致重名
            saveFile.createNewFile() ;
            taskInfo.setFileName(saveFile.getName());
            //更新task并通知前端
            DownloadTask downloadTask = new DownloadTask(dbTaskManager, saveDir, unfinishedTaskListLiveData, taskInfo,finishedTaskListLiveData);
            dbTaskManager.insert(taskInfo,downloadTask,threadPoolExecutor);

            unfinishedTaskListLiveData.addValue(taskInfo);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeMaxTaskNumbers(int max_connection_number) {
        if(max_connection_number>Constant.MAX_TASKS){
            Constant.MAX_TASKS = max_connection_number ;
            threadPoolExecutor.setMaximumPoolSize(max_connection_number);
            threadPoolExecutor.setCorePoolSize(max_connection_number);
        }else {
            //比原来小的时候一定要先改小max值
            Constant.MAX_TASKS = max_connection_number ;
            threadPoolExecutor.setCorePoolSize(max_connection_number);
            threadPoolExecutor.setMaximumPoolSize(max_connection_number);
            if(threadPoolExecutor.getActiveCount()>max_connection_number){
                //正在执行的任务数大于了配置数，
                //先清空等待队列，然后将执行任务中优先级低的停止。
                threadPoolExecutor.cutting();
            }
        }


    }

    public void multiDelete() {
        List<TaskInfo> unfinishedTasks = unfinishedTaskListLiveData.getValue();
        List<TaskInfo> toStopTasks = new ArrayList<>() ;
        if(unfinishedTasks!=null && unfinishedTasks.size()!=0){
            for(TaskInfo taskInfo:unfinishedTasks){
                if (taskInfo.isSelected()){
                    //先停止下载任务
                    DownloadTask downloadTask = taskInfo.getDownloadTask();
                    if (downloadTask != null) {
                        //删除任务
                        downloadTask.delete();
                    }
                    //释放引用
                    taskInfo.setDownloadTask(null);
                    toStopTasks.add(taskInfo) ;
                }
            }
        }
        //删除缓存，更新前端
        unfinishedTaskListLiveData.multiDelete(toStopTasks) ;

        List<TaskInfo> finishedTasks = finishedTaskListLiveData.getValue();
        List<TaskInfo> toStopTasksOfFinished = new ArrayList<>() ;
        for(TaskInfo taskInfo:finishedTasks){
            if (taskInfo.isSelected()){
                toStopTasksOfFinished.add(taskInfo) ;
            }
        }
        finishedTaskListLiveData.multiDelete(toStopTasksOfFinished) ;
    }


    //删除单个
    public void delete(TaskInfo task,int taskFlag) {
        if(taskFlag==Constant.UNFINISHED_FLAG) {
            //可以从waitting,running,pause,finish四个态转换到delete
            DownloadTask downloadTask = task.getDownloadTask();
            if (downloadTask != null) {
                //删除任务
                downloadTask.delete();
            }
            //释放引用
            task.setDownloadTask(null);

            //删除缓存数据、数据库内容、已下载文件
            dbTaskManager.delete(task);
            File file1 = new File(saveDir,task.getFileName()) ;
            if(file1.exists()){
                file1.delete() ;
            }
            unfinishedTaskListLiveData.delete(task);
        }else {
            //删除缓存数据、数据库内容、已下载文件
            dbTaskManager.delete(task);
            File file1 = new File(saveDir,task.getFileName()) ;
            if(file1.exists()){
                file1.delete() ;
            }
            finishedTaskListLiveData.delete(task);
        }
    }

    //暂停，开始
    public void pauseOrBegin(int postiton) {
        TaskInfo task = unfinishedTaskListLiveData.get(postiton);
        if (task.isFinished())
            return;
        DownloadTask downloadTask = task.getDownloadTask();
        if (downloadTask==null||downloadTask.getStatus()==DownloadTask.PAUSE) {
            //原先是暂停状态
            /**
             * 重新进入下载队列
             */

            downloadTask = new DownloadTask(dbTaskManager, saveDir, unfinishedTaskListLiveData, task,finishedTaskListLiveData);
            threadPoolExecutor.execute(downloadTask);

            //更新缓存和前端数据
            task.setPaused(false);
            task.setSpeed(Constant.SPEED_OF_WAITTING);
            unfinishedTaskListLiveData.updateValue(task);
        } else if(downloadTask.getStatus()==DownloadTask.WAITTING||downloadTask.getStatus()==DownloadTask.RUNNING){
            downloadTask.pause();
            //更新缓存内容
            task.setPaused(true);
            task.setSpeed(Constant.SPEED_OF_PAUSE);

            //提醒更新界面
            unfinishedTaskListLiveData.updateValue(task);
        }

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

        TaskInfo movingTask = unfinishedTaskListLiveData.get(oldPosition);
        TaskInfo targetPositionTask = unfinishedTaskListLiveData.get(targetPosition) ;

        if(movingTask==null||targetPositionTask==null){
            return;
        }

        //检测运行状态
        if(oldPosition>targetPosition){
            //更新任务优先级，
            movingTask.setPriority(targetPositionTask.getPriority());
            movingTask.setJumpTimeStamp(System.currentTimeMillis());

            //往前插，提高优先级：如果当前线程处于WAITTING状态，检测插队的后面是否有正在执行的任务，有的话就停止，让出线程；
            // //           如果当前线程处于RUNNING，不做处理；
            //              如果当前线程处于CANCEL，不做处理。

            //先检测移动任务处于什么状态
            DownloadTask downloadTask = movingTask.getDownloadTask();
            if(downloadTask==null){
                //暂停态,不做处理
            }else if(downloadTask.getStatus()==DownloadTask.FINISHED){
                //任务刚好完成，不做处理
            }if(downloadTask.getStatus()== DownloadTask.WAITTING){
                 //等待态，需要考虑新位置后面是否有正在执行的

                //重新入队

                //先取消再入队
                downloadTask.delete();
                downloadTask = new DownloadTask(dbTaskManager, saveDir, unfinishedTaskListLiveData, movingTask,finishedTaskListLiveData);
                threadPoolExecutor.execute(downloadTask);

                //然后看看有没有优先级低的在运行态
                List<TaskInfo> subTasks = unfinishedTaskListLiveData.get(targetPosition, oldPosition);

                //从后往前遍历，看看如果有任务在执行，则停止，让出线程
                TaskInfo backTask ;
                DownloadTask backDownloadTask ;
                for(int i=subTasks.size()-1;i>=0;i--){
                    backTask = subTasks.get(i);
                    backDownloadTask = backTask.getDownloadTask() ;
                    if(backDownloadTask.getStatus()==DownloadTask.RUNNING){
                        //找到第一个运行态的，结束让出线程
                        backDownloadTask.delete();

                        //也要把人家放回去等待态
                        backDownloadTask = new DownloadTask(dbTaskManager, saveDir, unfinishedTaskListLiveData, backTask,finishedTaskListLiveData);
                        threadPoolExecutor.execute(backDownloadTask);
                        unfinishedTaskListLiveData.updateValueNotPost(backTask);
                        break;
                    }
                }
            }else {
                //运行态，已经提高优先级，不用处理
            }
        }
        else {
            //往后拉
            //更新任务优先级，
            movingTask.setPriority(targetPositionTask.getPriority());
            movingTask.setJumpTimeStamp(targetPositionTask.getJumpTimeStamp()-1);

            //如果当前线程处于RUNNING状态，检测新位置靠前是否有任务在等待态，如果有，则停止当前任务的执行，让出线程；
            //                          如果当前线程处于WAITTING状态，重新入队列；
            //                           如果当前线程处于CANCEL状态，不做处理
            DownloadTask downloadTask = movingTask.getDownloadTask();
            if(downloadTask==null){
                //暂停态，不处理
            }else if(downloadTask.getStatus()==DownloadTask.FINISHED){
                //完成态，不处理
            }else if(downloadTask.getStatus()==DownloadTask.RUNNING){
                ///运行态
                //遍历新位置之前的任务，如果有等待态的，则让出线程
                List<TaskInfo> subList = unfinishedTaskListLiveData.get(oldPosition+1, targetPosition+1);
                //从高优先级的开始遍历
                TaskInfo forwardTask ;
                DownloadTask forwardDownloadTask ;
                for(int i=0;i<subList.size();i++){
                    forwardTask = subList.get(i);
                    forwardDownloadTask = forwardTask.getDownloadTask() ;
                    if(forwardDownloadTask.getStatus()==DownloadTask.WAITTING){
                        //让出线程
                        downloadTask.delete();
                        downloadTask = new DownloadTask(dbTaskManager,saveDir, unfinishedTaskListLiveData,movingTask,finishedTaskListLiveData) ;
                        threadPoolExecutor.execute(downloadTask);
                        unfinishedTaskListLiveData.updateValueNotPost(movingTask);
                        break;
                    }
                }
            }else if(downloadTask.getStatus()==DownloadTask.WAITTING){
                //等待态，重新入队
                downloadTask.delete();
                downloadTask = new DownloadTask(dbTaskManager,saveDir, unfinishedTaskListLiveData,movingTask,finishedTaskListLiveData) ;
                threadPoolExecutor.execute(downloadTask);
            }
        }
        //更新缓存数据，以更新前端
        unfinishedTaskListLiveData.move(oldPosition,targetPosition) ;
        dbTaskManager.update(movingTask);
    }
}