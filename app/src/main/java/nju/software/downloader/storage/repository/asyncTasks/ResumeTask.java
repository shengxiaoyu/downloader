package nju.software.downloader.storage.repository.asyncTasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.model.TaskListLiveData;
import nju.software.downloader.storage.dao.TaskDao;

public class ResumeTask implements Runnable{
    private TaskListLiveData taskListLiveData ;
    private TaskDao taskDao;
    private File saveDir ;
    private TaskInfo taskInfo ;
    public ResumeTask(TaskDao taskDao, File saveDir, TaskListLiveData taskListLiveData, TaskInfo taskInfo){
        this.taskListLiveData = taskListLiveData ;
        this.taskDao = taskDao;
        this.saveDir = saveDir ;
        this.taskInfo = taskInfo ;
    }
    @Override
    public void run() {}}
