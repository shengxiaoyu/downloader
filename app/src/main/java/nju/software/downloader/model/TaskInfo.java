package nju.software.downloader.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

import nju.software.downloader.storage.repository.asyncTasks.DownloadTask;
import nju.software.downloader.util.Constant;


@Entity(tableName = "TaskInfo_Table")
public class TaskInfo implements Serializable,Comparable<TaskInfo> {

    @NonNull
    @ColumnInfo(name = "id")
    //自增
    @PrimaryKey(autoGenerate = true)
    private Long id ;

    @NonNull
    @ColumnInfo(name = "url")
    private String url ;

    //是否已完成
    @ColumnInfo(name = "finished")
    private boolean finished ;

    //存储文件名
    @ColumnInfo(name = "fileName")
    private String fileName ;

    //任务优先级
    @ColumnInfo(name = "priority")
    private int priority ;

    //插队的时间戳
    @ColumnInfo(name = "jumpTimeStamp")
    private long jumpTimeStamp;

    //下载进度
    @Ignore
    private Integer progress ;

    //下载速度
    @Ignore
    private String speed = Constant.SPEED_OF_WAITTING;

    //是否暂停状态
    @Ignore
    private boolean paused ;

    //是否被选中
    @Ignore
    private boolean selected ;

    @Ignore
    private volatile DownloadTask downloadTask ;

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public TaskInfo(@NonNull String url){
        this.url = url ;

    }
    //room使用一个构造器去添加构造，因此要@Ignore一个
    @Ignore
    public TaskInfo(Long id, @NonNull String url){
        this.id = id ;
        this.url = url ;
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
        this.downloadTask = another.downloadTask ;
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
