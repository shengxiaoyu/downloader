package nju.software.downloader.model;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.concurrent.Future;

import nju.software.downloader.R;
import nju.software.downloader.util.Constant;


@Entity(tableName = "TaskInfo_Table")
public class TaskInfo implements Serializable {

    @NonNull
    @ColumnInfo(name = "id")
    //自增
    @PrimaryKey(autoGenerate = true)
    private Long id ;

    @NonNull
    @ColumnInfo(name = "url")
    private String url ;


    @ColumnInfo(name = "fileName")
    private String fileName ;

    @ColumnInfo(name = "length")
    private Integer progress ;


    @Ignore
    private String speed = Constant.WAITTING;

    @ColumnInfo(name = "paused")
    private boolean paused ;

    @ColumnInfo(name = "finished")
    private boolean finished ;

    @Ignore
    private Future taskThraed ;

    @Ignore
    private boolean selected ;
    public TaskInfo(@NonNull String url){
        this.url = url ;

    }

    //room使用一个构造器去添加构造，因此要@Ignore一个
    @Ignore
    public TaskInfo(Long id, @NonNull String url){
        this.id = id ;
        this.url = url ;
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

    public Future getTaskThraed() {
        return taskThraed;
    }

    public void setTaskThraed(Future taskThraed) {
        this.taskThraed = taskThraed;
    }


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void updateByAnotherOne(TaskInfo another){
        if(another==null){
            return;
        }
        this.taskThraed = another.taskThraed ;
        this.finished = another.finished ;
        this.fileName = another.fileName ;
        this.progress = another.progress ;
        this.paused = another.paused ;
        this.speed = another.speed ;
        this.selected = another.selected ;
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
}
