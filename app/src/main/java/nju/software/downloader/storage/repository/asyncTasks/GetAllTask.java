package nju.software.downloader.storage.repository.asyncTasks;

import android.os.AsyncTask;

import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.storage.repository.TaskRepository;

public class GetAllTask extends AsyncTask<Void,Void,List<TaskInfo> > {
    private TaskDao taskDao ;
    private TaskListLiveData taskListLiveData ;
    private TaskRepository taskRepository ;
    public GetAllTask(TaskRepository taskRepository, TaskDao taskDao, TaskListLiveData taskListLiveData){
        this.taskDao = taskDao ;
        this.taskListLiveData = taskListLiveData ;
        this.taskRepository = taskRepository ;
    }
    @Override
    protected List<TaskInfo> doInBackground(Void... voids) {
        return taskDao.getAll();
    }

    @Override
    protected void onPostExecute(List<TaskInfo> taskInfos) {
        super.onPostExecute(taskInfos);
        taskRepository.restartUnfinishedTask(taskInfos);
        CopyOnWriteArrayList newCurrentList = new CopyOnWriteArrayList(taskInfos) ;
        taskListLiveData.postValue(newCurrentList);
    }

}
