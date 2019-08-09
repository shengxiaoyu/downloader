package nju.software.downloader.model;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

import nju.software.downloader.repository.network.DownloadTask;
import nju.software.downloader.util.Constant;


public class TaskInfo implements Serializable,Comparable<TaskInfo> {

    private Long id ;

    private String url ;

    //是否已完成
    private boolean finished ;

    //存储文件名
    private String fileName ;

    //任务优先级
    private int priority ;

    //插队的时间戳
    private long jumpTimeStamp;

    //下载进度
    private Integer progress ;

    //下载速度
    private String speed = Constant.SPEED_OF_WAITTING;

    //是否暂停状态
    private boolean paused ;

    //是否被选中
    private boolean selected ;

    private DownloadTask downloadTask ;


    public TaskInfo(long id, String url, String fileName, boolean finished, int priority, long jumptimestamp){
        this.id = id ;
        this.url = url ;
        this.fileName = fileName ;
        this.priority = priority ;
        this.jumpTimeStamp = jumptimestamp ;

        if(finished){
            progress = 100 ;
        }else {
            speed = Constant.SPEED_OF_WAITTING ;
        }
    }



    public TaskInfo(@NonNull String url){
        this.url = url ;
    }
    public TaskInfo(@NonNull String url,String fileName){
        this.url = url ;
        this.fileName = fileName ;
    }
    void updateByAnotherOne(TaskInfo another){
        if(another==null){
            return;
        }
        this.finished = another.finished ;
        this.fileName = another.fileName ;
        this.progress = another.progress ;
        this.paused = another.paused ;
        this.speed = another.speed ;
        this.selected = another.selected ;
        this.priority = another.priority ;
        this.jumpTimeStamp = another.jumpTimeStamp ;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }
    @NonNull
    public Long getId() {
        return id;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getJumpTimeStamp() {
        return jumpTimeStamp;
    }

    public void setJumpTimeStamp(long jumpTimeStamp) {
        this.jumpTimeStamp = jumpTimeStamp;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode() ;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==null){
            return false ;
        }
        if(obj instanceof TaskInfo){
            TaskInfo another = (TaskInfo) obj;
            return another.getId()== id ;
        }
        return false ;
    }

    /**
     * 优先级比较，通过两个属性来比较：首先是比较priotity属性，如果priority相等，
     * 再比较插队的时间戳，因为插队的时候一定是吧prioity设为被插队的task的priority,并记录插队时间戳，因此时间戳大的就是后来插队的，应该排在前面
     * @param taskInfo
     * @return
     */
    @Override
    public int compareTo(@NonNull TaskInfo taskInfo) {
        if(priority==taskInfo.priority){
            return jumpTimeStamp >taskInfo.jumpTimeStamp ? -1:1 ;
        }else{
            return (priority < taskInfo.priority) ? -1 : 1 ;
        }
    }

}
