package nju.software.downloader.storage.repository.asyncTasks;

import android.os.AsyncTask;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.storage.dao.TaskDao;

public class UpdateDBTask extends AsyncTask<TaskInfo,Void,Void> {
    private TaskDao taskDao;
    @Override
    protected Void doInBackground(TaskInfo... taskInfos) {
        taskDao.delete(taskInfos[0]);
        return null;
    }
    public UpdateDBTask(TaskDao taskDao){
        this.taskDao = taskDao;
    }
}