package nju.software.downloader.storage.repository.asyncTasks;

import android.os.AsyncTask;

import java.io.File;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.storage.dao.TaskDao;
import nju.software.downloader.util.FileUtil;

public class DeleteSingleTask extends AsyncTask<TaskInfo,Void,Void> {
    private TaskListLiveData taskListLiveData ;
    private TaskDao taskDao;
    private File saveDir ;
    @Override
    protected Void doInBackground(TaskInfo... taskInfos) {
        taskListLiveData.delete(taskInfos[0]);
        taskDao.delete(taskInfos[0]);
        //删除下载文件
        File file1 = new File(saveDir,taskInfos[0].getFileName()) ;
        if(file1.exists()){
            file1.delete() ;
        }
        return null;
    }
    public DeleteSingleTask(TaskListLiveData taskListLiveData, TaskDao taskDao, File saveDir){
        this.taskDao = taskDao;
        this.saveDir = saveDir ;
        this.taskListLiveData = taskListLiveData ;
    }
}
