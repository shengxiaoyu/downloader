package nju.software.downloader.repository.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nju.software.downloader.model.TaskInfo;


/**
 * sqlite的writeDatabase只支持Serial调用，因此将考虑单线程完成数据库的写操作。
 * 这样会导致数据库更新慢，但考虑用户的插入任务操作不会很快，而且不存在多个app同时运行，因此可以接受
 */
public class TaskDao {
    private TaskDbHelper dbHelper ;
    private SQLiteDatabase dbReader ;
    private SQLiteDatabase dbWriter ;
    public TaskDao(Context context){
        dbHelper = new TaskDbHelper(context) ;
        dbReader = dbHelper.getReadableDatabase() ;
        dbWriter = dbHelper.getWritableDatabase() ;
    }

    /**
     * 单线程，不用考虑线程安全性
     * @param taskInfo
     */
    public long insert(TaskInfo taskInfo){
        ContentValues values = new ContentValues() ;
        values.put(TaskContract.TaskEntry.COLUMN_NAME_URL,taskInfo.getUrl());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FILENAME,taskInfo.getFileName());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FINISHED,taskInfo.isFinished());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_PRIORITY,taskInfo.getPriority());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP,taskInfo.getJumpTimeStamp());

        long newRowId = dbWriter.insert(TaskContract.TaskEntry.TABLE_NAME, null, values);
        return newRowId ;
    }

    /**
     * 返回所有下载任务列表
     * @return
     */
    public List<TaskInfo> findAll(){
        String[] projection = {
                BaseColumns._ID,
                TaskContract.TaskEntry.COLUMN_NAME_URL,
                TaskContract.TaskEntry.COLUMN_NAME_FILENAME,
                TaskContract.TaskEntry.COLUMN_NAME_FINISHED,
                TaskContract.TaskEntry.COLUMN_NAME_PRIORITY,
                TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP
        } ;
        String sortOrder = TaskContract.TaskEntry.COLUMN_NAME_PRIORITY+" ASC,"
                + TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP+" DESC" ;

        Cursor cursor = dbReader.query(
                TaskContract.TaskEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
        ArrayList<TaskInfo> taskInfos = new ArrayList() ;

        //实例化每个任务
        long id;
        String url ;
        String fileName ;
        int finished ;
        int priority ;
        long jumpTimeStamp ;
        TaskInfo taskInfo ;

        while (cursor.moveToNext()){
            id = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry._ID)) ;
            url = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_URL)) ;
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_FILENAME)) ;
            finished = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_FINISHED)) ;
            priority = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_PRIORITY)) ;
            jumpTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP)) ;
            taskInfo = new TaskInfo(id,url,fileName,finished>0?true:false,priority,jumpTimeStamp) ;
            taskInfos.add(taskInfo) ;
        }
        return taskInfos ;
    }

    public void delete(long id){
        String selection = TaskContract.TaskEntry._ID +" =? " ;
        String[] selectionArgs = {id+""} ;

        dbWriter.delete(TaskContract.TaskEntry.TABLE_NAME,selection,selectionArgs) ;
    }
    public void deleteTasks(long[] ids){
        StringBuilder sb = new StringBuilder() ;
        if(ids!=null && ids.length>0){
            for(long id:ids){
                sb.append(id+"");
                sb.append(",") ;
            }
            sb.delete(sb.length()-1,sb.length()) ;
            String selection = TaskContract.TaskEntry._ID +" in ( "+ sb.toString() +")";
            dbWriter.delete(TaskContract.TaskEntry.TABLE_NAME,selection,new String[0]) ;
        }
    }
    public void update(TaskInfo taskInfo){
        ContentValues values = new ContentValues() ;
        values.put(TaskContract.TaskEntry.COLUMN_NAME_URL,taskInfo.getUrl());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FILENAME,taskInfo.getFileName());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FINISHED,taskInfo.isFinished());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_PRIORITY,taskInfo.getPriority());
        values.put(TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP,taskInfo.getJumpTimeStamp());

        String selection = BaseColumns._ID+ " =? " ;
        String[] selectionArgs = {taskInfo.getId()+""} ;

        dbWriter.update(TaskContract.TaskEntry.TABLE_NAME,values,selection,selectionArgs) ;
    }
}
