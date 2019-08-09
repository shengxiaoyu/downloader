package nju.software.downloader.repository.network.asyncTasks;

import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.repository.database.TaskDao;
import nju.software.downloader.repository.TaskRepository;
import nju.software.downloader.util.Constant;

public class GetAllTask extends AsyncTask<Void,Void,List<TaskInfo> > {
    private TaskDao taskDao ;
    private TaskListLiveData unfinishedTaskListLiveData;
    private TaskRepository taskRepository ;
    private TaskListLiveData finishedTaskLiveData ;
    public GetAllTask(TaskRepository taskRepository, TaskDao taskDao, TaskListLiveData unfinishedTaskListLiveData, TaskListLiveData finishedTaskLiveData){
        this.taskDao = taskDao ;
        this.unfinishedTaskListLiveData = unfinishedTaskListLiveData ;
        this.taskRepository = taskRepository ;
        this.finishedTaskLiveData = finishedTaskLiveData ;
    }
    @Override
    protected List<TaskInfo> doInBackground(Void... voids) {
        return taskDao.getAll();
    }

    @Override
    protected void onPostExecute(List<TaskInfo> taskInfos) {
        super.onPostExecute(taskInfos);
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

        //重启未完成的任务
        taskRepository.restartUnfinishedTask(unfinishedTasks);

        //放入缓存数据
        unfinishedTaskListLiveData.postValue(unfinishedTasks);
        finishedTaskLiveData.postValue(finishedTasks);
    }

}
