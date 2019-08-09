package nju.software.downloader.repository.network.asyncTasks;

import android.os.AsyncTask;

import java.io.File;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.repository.database.TaskDao;

public class DeleteSingleTask extends AsyncTask<TaskInfo,Void,Void> {
    private TaskDao taskDao;
    private File saveDir ;
    @Override
    protected Void doInBackground(TaskInfo... taskInfos) {
        taskDao.delete(taskInfos[0]);
        //删除下载文件
        File file1 = new File(saveDir,taskInfos[0].getFileName()) ;
        if(file1.exists()){
            file1.delete() ;
        }
        return null;
    }
    public DeleteSingleTask(TaskDao taskDao, File saveDir){
        this.taskDao = taskDao;
        this.saveDir = saveDir ;
    }
}
