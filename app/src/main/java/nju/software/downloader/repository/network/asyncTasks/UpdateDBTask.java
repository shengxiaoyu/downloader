package nju.software.downloader.repository.network.asyncTasks;

import android.os.AsyncTask;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.repository.database.TaskDao;

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