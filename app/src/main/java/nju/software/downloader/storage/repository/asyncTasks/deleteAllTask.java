package nju.software.downloader.storage.repository.asyncTasks;

import android.os.AsyncTask;

import nju.software.downloader.storage.dao.TaskDao;

public class deleteAllTask extends AsyncTask<Void,Void,Void> {
    private TaskDao taskDao;
    @Override
    protected Void doInBackground(Void... voids) {
        taskDao.deleteAll();
        return null;
    }
    public deleteAllTask(TaskDao dao){
        this.taskDao = dao ;
    }
}
