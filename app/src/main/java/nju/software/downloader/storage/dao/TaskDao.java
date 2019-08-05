package nju.software.downloader.storage.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nju.software.downloader.model.TaskInfo;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TaskInfo taskInfo);

    @Delete
    void delete(TaskInfo taskInfo) ;

    @Query("DELETE FROM TaskInfo_Table")
    void deleteAll() ;

    //使用LiveData包装，已到达观察者自动观察更新的目的
    @Query("SELECT * FROM TaskInfo_Table ORDER BY id ASC")
    List<TaskInfo> getAll() ;

    @Update
    void update(TaskInfo taskInfo) ;
}