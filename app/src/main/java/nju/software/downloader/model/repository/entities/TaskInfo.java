package nju.software.downloader.model.repository.entities;

import java.io.File;
import java.io.Serializable;

import nju.software.downloader.util.Constant;


public class TaskInfo implements Serializable,Comparable<TaskInfo> {

    private volatile Long id ;

    private String url ;

    //存储文件名
    private String fileName ;

    private File saveFile ;

    //任务优先级
    private int priority ;

    //插队的时间戳
    private long jumpTimeStamp;

    //下载进度
    private int progress ;

    //下载速度
    private String speed ;

    //是否被选中
    private boolean selected ;

    private boolean isPause ;

    private boolean isFinished ;
    /**
     * 给数据库初始化任务列表使用
     * @param id
     * @param url
     * @param fileName
     * @param finished
     * @param priority
     * @param jumptimestamp
     */
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

    public TaskInfo(String url,String fileName,File file){
        this.url = url ;
        this.fileName = fileName ;
        this.saveFile = file ;
        this.speed = Constant.SPEED_OF_WAITTING ;
    }

    public void updateByAnotherOne(TaskInfo another){
        if(another==null){
            return;
        }
        this.fileName = another.fileName ;
        this.priority = another.priority ;
        this.jumpTimeStamp = another.jumpTimeStamp ;
        this.progress = another.progress ;
        this.speed = another.speed ;
        this.selected = another.selected ;
        this.isPause = another.isPause ;
        this.isFinished = another.isFinished ;
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
    public int compareTo( TaskInfo taskInfo) {
        if(priority==taskInfo.priority){
            return jumpTimeStamp >taskInfo.jumpTimeStamp ? -1:1 ;
        }else{
            return (priority < taskInfo.priority) ? -1 : 1 ;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(long rowId) {
        this.id = rowId ;
    }

}
