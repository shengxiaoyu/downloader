package nju.software.downloader.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

import nju.software.downloader.repository.asyncTasks.DownloadTask;
import nju.software.downloader.util.Constant;


/**
 * 任务实体类，记录了任务的主要属性，前端展示信息和数据库存储信息。
 * 任务的下载信息放在一起，这个类就过于重，因此独立出一个DownloadTask类，专门管理下载过程信息。
 */
public class TaskInfo implements Serializable,Comparable<TaskInfo> {

    private Long id ;

    private String url ;

    //是否已完成
    private boolean finished ;

    //存储文件名
    private String fileName ;

    /**
     * 任务优先级，通过priority和jumpTimeStamp控制，任务比较通过先比较优先级，值越小优先级越大，相同情况比较插队时间戳，改时间戳越大，优先级越高
     */
    private int priority ;

    //插队的时间戳
    private long jumpTimeStamp;

    //下载进度
    private int progress ;

    //下载速度
    private String speed = Constant.SPEED_OF_WAITTING;

    //是否暂停状态
    private boolean paused ;

    //是否被选中
    private boolean selected ;

    private volatile DownloadTask downloadTask ;

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public TaskInfo(String url){
        this.url = url ;
    }
    public TaskInfo(long id, String url, String fileName, boolean finished, int priority, long jumpTimeStamp){
        this.id = id ;
        this.url = url ;
        this.fileName = fileName ;
        this.finished = finished ;
        this.priority = priority ;
        this.jumpTimeStamp = jumpTimeStamp ;

    }
    //room使用一个构造器去添加构造，因此要@Ignore一个
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


    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
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
     * 再比较插队的时间戳，因为插队的时候一定是吧prioity设为被插队的task的priority,并记录插队时间戳，插队时间戳的更新方式为：往前插则设为当前系统时间，往后插则为原位置记录时间戳-1.所以priority相同的情况下，时间戳大的优先级高
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
