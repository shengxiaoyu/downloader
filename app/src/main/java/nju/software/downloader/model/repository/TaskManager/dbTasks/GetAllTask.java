package nju.software.downloader.model.repository.TaskManager.dbTasks;

import android.os.AsyncTask;

import java.util.List;

import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.model.dao.TaskDao;

public class GetAllTask extends AsyncTask<Void,Void,List<TaskInfo> > {
    private TaskDao taskDao ;
    private DBTaskManager taskManager ;
    public GetAllTask(DBTaskManager taskManager, TaskDao taskDao){
        this.taskDao = taskDao ;
        this.taskManager = taskManager ;
    }
    @Override
    protected List<TaskInfo> doInBackground(Void... voids) {
        List<TaskInfo> all = taskDao.findAll();
        return all;
    }

    @Override
    protected void onPostExecute(List<TaskInfo> taskInfos) {
        super.onPostExecute(taskInfos);
        taskManager.afterFindAll(taskInfos) ;
    }

}
