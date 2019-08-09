package nju.software.downloader.model.repository.TaskManager.dbTasks;

import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.model.dao.TaskDao;

public class UpdateTask implements Runnable {
    private TaskDao taskDao ;
    private TaskInfo taskInfo ;
    UpdateTask(TaskDao taskDao,TaskInfo taskInfo){
        this.taskDao = taskDao ;
        this.taskInfo = taskInfo ;
    }
    @Override
    public void run() {
        taskDao.update(taskInfo);
    }
}
