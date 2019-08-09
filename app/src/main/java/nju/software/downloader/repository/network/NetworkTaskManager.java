package nju.software.downloader.repository.network;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.TaskRepository;
import nju.software.downloader.repository.database.DBTaskManager;
import nju.software.downloader.repository.network.asyncTasks.CustomerThreadPoolExecutor;
import nju.software.downloader.repository.network.asyncTasks.DownloadTask;
import nju.software.downloader.util.Constant;

public class NetworkTaskManager {
    private static final String LOG_TAG = NetworkTaskManager.class.getSimpleName() ;
    private volatile TaskListLiveData unfinishedTaskListLiveData = new TaskListLiveData();
    private volatile TaskListLiveData finishedTaskListLiveData = new TaskListLiveData() ;

    private ThreadPoolExecutor executor ;
    private DBTaskManager dbTaskManager ;
    private File saveDir ;

    private TaskRepository taskRepository ;
    public NetworkTaskManager(TaskRepository taskRepository, DBTaskManager dbTaskManager, int maxTasks, File saveDir){
        this.taskRepository = taskRepository ;
        this.executor = new CustomerThreadPoolExecutor(maxTasks,maxTasks,0L,TimeUnit.MILLISECONDS,new PriorityBlockingQueue<Runnable>(Constant.BLOCKQUEUE_INIT_VALUE)) ;

        //相互引用
        this.dbTaskManager = dbTaskManager ;
        dbTaskManager.setNetworkTaskManager(this);

        this.saveDir = saveDir ;
    }

    //重启任务
    public void restartTasks(List<TaskInfo> taskInfos){
        CopyOnWriteArrayList<TaskInfo> unfinishedTasks = new CopyOnWriteArrayList<>() ;
        CopyOnWriteArrayList<TaskInfo> finishedTasks = new CopyOnWriteArrayList<>() ;
        if(taskInfos!=null && taskInfos.size()>0){
            for(TaskInfo taskInfo:taskInfos){
                if(taskInfo.isFinished()){
                    taskInfo.setSpeed(Constant.SPEED_OF_FINISHED);
                    taskInfo.setProgress(100);
                    finishedTasks.add(taskInfo) ;
                }else{
                    unfinishedTasks.add(taskInfo) ;
                }
            }
        }
        //放入缓存数据，提醒前端更新
        unfinishedTaskListLiveData.postValue(unfinishedTasks);
        finishedTaskListLiveData.postValue(finishedTasks);



        //未完成的任务加入下载队列
        if(unfinishedTasks==null||unfinishedTasks.size()==0){
            return;
        }
        for(TaskInfo taskInfo:unfinishedTasks) {
            if(!taskInfo.isFinished()) {
                addTask(taskInfo);
            }
        }
    }

    /**
     * 新增任务
     * @param taskInfo
     */
    public void addTask(TaskInfo taskInfo){
        DownloadTask downloadTask = new DownloadTask(dbTaskManager,saveDir,taskInfo,unfinishedTaskListLiveData,finishedTaskListLiveData) ;
        this.executor.execute(downloadTask);
    }

    /**
     * 删除任务
     * @param taskInfo
     */
    public void cancelTask(TaskInfo taskInfo){
        //不可恢复的
        DownloadTask downloadTask = taskInfo.getDownloadTask();
        if(downloadTask!=null) {
            //走到这里说明这个任务在执行池中，可能在等待，可能正在执行
            executor.remove(downloadTask);

            taskInfo.cancel() ;
        }
    }

    /**
     * 删除已选任务
     */
    public void multiDelete() {
        List<TaskInfo> unfinishedTasks = unfinishedTaskListLiveData.getValue();
        List<TaskInfo> toDeleteTasks = new ArrayList<>() ;
        if(unfinishedTasks!=null && unfinishedTasks.size()!=0){
            for(TaskInfo taskInfo:unfinishedTasks){
                if (taskInfo.isSelected()){
                    //先停止下载任务
                    DownloadTask downloadTask = taskInfo.getDownloadTask();
                    if(downloadTask!=null){
                        executor.remove(downloadTask) ;
                        downloadTask.cancel();
                        downloadTask.setTaskInfo(null);
                        taskInfo.setDownloadTask(null);
                    }
                    File saveFile = new File(saveDir,taskInfo.getFileName());
                    saveFile.delete() ;
                }
            }
        }
        //删除缓存，更新前端

        dbTaskManager.multiDelete(toDeleteTasks);
        unfinishedTaskListLiveData.multiDelete(toDeleteTasks) ;

        List<TaskInfo> finishedTasks = finishedTaskListLiveData.getValue();
        toDeleteTasks = new ArrayList<>() ;
        File saveFile ;
        for(TaskInfo taskInfo:finishedTasks){
            if (taskInfo.isSelected()){
                toDeleteTasks.add(taskInfo) ;
                saveFile = new File(saveDir,taskInfo.getFileName());
                saveFile.delete() ;
            }
        }
        dbTaskManager.multiDelete(toDeleteTasks);
        finishedTaskListLiveData.multiDelete(toDeleteTasks) ;
    }

    /**
     * 更新最大任务数
     * @param newNumberOfMaxTasks
     */
    public void updateMaxTask(int newNumberOfMaxTasks){
        if(newNumberOfMaxTasks!= Constant.MAX_TASKS) {
            this.executor.setCorePoolSize(newNumberOfMaxTasks);
            this.executor.setMaximumPoolSize(newNumberOfMaxTasks);
        }
    }


    public LiveData<List<TaskInfo>> getUnfinishedTasks() {
        return unfinishedTaskListLiveData;
    }

    public LiveData<List<TaskInfo>> getFinishedTasks(){
        return finishedTaskListLiveData ;
    }

    public void moveTask(int oldPosition, int targetPosition) {
        TaskInfo movingTask = unfinishedTaskListLiveData.get(oldPosition);
        TaskInfo targetPositionTask = unfinishedTaskListLiveData.get(targetPosition) ;

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
            }else if(downloadTask.isFinished()){
                //任务刚好完成，不做处理
            }
            if(downloadTask.isWaitting()){
                //等待态，需要考虑新位置后面是否有正在执行的

                //重新入队

                //先取消再入队
                downloadTask.cancel();
                executor.remove(downloadTask) ;
                movingTask.setDownloadTask(null);
                downloadTask.setTaskInfo(null);
                downloadTask = new DownloadTask(movingTask,saveDir,dbTaskManager, unfinishedTaskListLiveData,finishedTaskListLiveData);
                executor.execute(downloadTask);

                //然后看看有没有优先级低的在运行态
                List<TaskInfo> subTasks = unfinishedTaskListLiveData.get(targetPosition, oldPosition);

                //从后往前遍历，看看如果有任务在执行，则停止，让出线程
                TaskInfo backTask ;
                DownloadTask backDownloadTask ;
                for(int i=subTasks.size()-1;i>=0;i--){
                    backTask = subTasks.get(i);
                    backDownloadTask = backTask.getDownloadTask() ;
                    if(backDownloadTask.isRunning()){
                        //找到第一个运行态的，结束让出线程
                        backDownloadTask.cancel();
                        backDownloadTask.setTaskInfo(null);
                        backTask.setDownloadTask(null);

                        //也要把人家放回去等待态
                        backDownloadTask = new DownloadTask(backTask,saveDir,dbTaskManager,unfinishedTaskListLiveData,finishedTaskListLiveData);
                        executor.execute(backDownloadTask);
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
            }else if(downloadTask.isFinished()){
                //完成态，不处理
            }else if(downloadTask.isRunning()){
                ///运行态
                //遍历新位置之前的任务，如果有等待态的，则让出线程
                List<TaskInfo> subList = unfinishedTaskListLiveData.get(oldPosition+1, targetPosition+1);
                //从高优先级的开始遍历
                TaskInfo forwardTask ;
                DownloadTask forwardDownloadTask ;
                for(int i=0;i<subList.size();i++){
                    forwardTask = subList.get(i);
                    forwardDownloadTask = forwardTask.getDownloadTask() ;
                    if(forwardDownloadTask.isWaitting()){
                        //让出线程
                        downloadTask.cancel();
                        downloadTask.setTaskInfo(null);
                        movingTask.setDownloadTask(null);
                        downloadTask = new DownloadTask(movingTask,saveDir,dbTaskManager,unfinishedTaskListLiveData,finishedTaskListLiveData) ;
                        executor.execute(downloadTask);
                        unfinishedTaskListLiveData.updateValueNotPost(movingTask);
                        break;
                    }
                }
            }else if(downloadTask.isWaitting()){
                //等待态，重新入队
                downloadTask.cancel();
                executor.remove(downloadTask) ;
                downloadTask.setTaskInfo(null);
                movingTask.setDownloadTask(null);
                downloadTask = new DownloadTask(movingTask,saveDir,dbTaskManager, unfinishedTaskListLiveData,finishedTaskListLiveData) ;
                executor.execute(downloadTask);
            }
        }
        //更新缓存数据，以更新前端
        unfinishedTaskListLiveData.move(oldPosition,targetPosition) ;
    }

    /**
     * delete finished task
     * @param task
     */
    public void delete(TaskInfo task) {
        if(task.isFinished()) {
            //如果任务已经完成了的，直接删除
            finishedTaskListLiveData.delete(task);
            //删除文件
            File saveFile = new File(saveDir,task.getFileName()) ;
            saveFile.delete() ;
        }else{
            //否则，尝试去执行队列中删除
            cancelTask(task);
        }
    }

    public void pauseOrBegin(int postiton) {
        TaskInfo task = unfinishedTaskListLiveData.get(postiton);
        if (task.isFinished())
            return;
        DownloadTask downloadTask = task.getDownloadTask();
        if (downloadTask==null) {
            //原先是暂停状态
            /**
             * 重新进入下载队列
             */

            downloadTask = new DownloadTask(task,saveDir, dbTaskManager, unfinishedTaskListLiveData,finishedTaskListLiveData);
            executor.execute(downloadTask);

            //更新缓存和前端数据
            task.setPaused(false);
            task.setSpeed(Constant.SPEED_OF_WAITTING);
            unfinishedTaskListLiveData.updateValue(task);
        } else if(downloadTask.isRunning()){
            executor.remove(downloadTask) ;
            downloadTask.pause();
            downloadTask.setTaskInfo(null);
            task.setDownloadTask(null);
            //更新缓存内容
            task.setPaused(true);
            task.setSpeed(Constant.SPEED_OF_PAUSE);
            //重新入队
            //提醒更新界面
            unfinishedTaskListLiveData.updateValue(task);
        }
    }

    //线程池优先队列 比较器
    private class downloadTaskCompator implements Comparator {
        @Override
        public int compare(Object o, Object t1) {
            if(o instanceof DownloadTask && t1 instanceof DownloadTask){
                return ((DownloadTask)o).compareTo(((DownloadTask)t1)) ;
            }
            return 0;
        }
    }

    public TaskListLiveData getUnfinishedTaskListLiveData() {
        return unfinishedTaskListLiveData;
    }

    public TaskListLiveData getFinishedTaskListLiveData() {
        return finishedTaskListLiveData;
    }
}
