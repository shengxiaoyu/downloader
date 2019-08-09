package nju.software.downloader.model.repository.TaskManager.dbTasks;

import java.util.List;

import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.model.dao.TaskDao;

public class MultiDeleteTask implements Runnable {
    private TaskDao taskDao ;
    private List<TaskInfo> tasks ;
    MultiDeleteTask(TaskDao taskDao, List<TaskInfo> tasks){
        this.taskDao = taskDao ;
        this.tasks = tasks ;
    }
    @Override
    public void run() {
        if(tasks!=null&&tasks.size()>0){
            long[] ids = new long[tasks.size()] ;
            int index = 0 ;
            for(TaskInfo taskInfo:tasks){
                ids[index++] = taskInfo.getId() ;
            }
            taskDao.deleteTasks(ids);
        }
    }
}
