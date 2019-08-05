package nju.software.downloader.model;

import androidx.lifecycle.MutableLiveData;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskListLiveData extends MutableLiveData<List<TaskInfo>> {
    public void updateValue(TaskInfo taskInfo){
        List<TaskInfo> oldTasks = getValue();
        for(TaskInfo theTask:oldTasks){
            if(theTask.getId()==taskInfo.getId()){
                theTask.updateByAnotherOne(theTask);
                break;
            }
        }
        postValue(oldTasks);
    }
    public void addValue(TaskInfo taskInfo){
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        oldTasks.add(taskInfo) ;
        postValue(oldTasks);
    }

    public void delete(TaskInfo taskInfo){
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        oldTasks.remove(taskInfo) ;
        postValue(oldTasks);
    }
}
