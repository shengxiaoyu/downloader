package nju.software.downloader.model;

import androidx.lifecycle.MutableLiveData;

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
        if(oldTasks==null){
            oldTasks = new CopyOnWriteArrayList<TaskInfo>() ;
        }
        //设置优先级为上一个队尾的优先级+1
        TaskInfo lastTask  ;
        if(oldTasks.size()>0&& (lastTask=oldTasks.get(oldTasks.size()-1))!=null){
            int priority = lastTask.getPriority();
            taskInfo.setPriority(priority+1);
        }else {
            //初始为1
            taskInfo.setPriority(1) ;
        }
        oldTasks.add(taskInfo) ;
        postValue(oldTasks);
    }

    public void delete(TaskInfo taskInfo){
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        if(oldTasks==null){
            return;
        }
        oldTasks.remove(taskInfo) ;
        postValue(oldTasks);
    }

    public void move(int oldPosition, int targetPosition) {
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        TaskInfo taskInfo = oldTasks.get(oldPosition);
        oldTasks.remove(oldPosition) ;
        oldTasks.add(targetPosition,taskInfo) ;
        postValue(oldTasks);
    }

    public TaskInfo get(int index){
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        if(oldTasks!=null&&index>=0&&index<oldTasks.size()){
            return oldTasks.get(index) ;
        }
        return null ;
    }

    public List<TaskInfo> get(int fromIndex,int toIndex){
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        if(oldTasks!=null){
            return oldTasks.subList(fromIndex,toIndex) ;
        }
        return null ;
    }

    public void updateValueNotPost(TaskInfo taskInfo) {
        List<TaskInfo> oldTasks = getValue();
        for(TaskInfo theTask:oldTasks){
            if(theTask.getId()==taskInfo.getId()){
                theTask.updateByAnotherOne(theTask);
                break;
            }
        }
    }

    public void multiDelete(List<TaskInfo> toStopTasks) {
        CopyOnWriteArrayList<TaskInfo> oldTasks = (CopyOnWriteArrayList)getValue();
        if(oldTasks!=null && oldTasks.size()!=0&&toStopTasks!=null&&toStopTasks.size()!=0){
            for(TaskInfo taskInfo:toStopTasks){
                oldTasks.remove(taskInfo) ;
            }
        }
        postValue(oldTasks);
    }
}
