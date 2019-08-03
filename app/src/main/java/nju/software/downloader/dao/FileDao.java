package nju.software.downloader.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import nju.software.downloader.entities.FileInfo;

@Dao
public interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FileInfo fileInfo);

    @Delete
    void delete(FileInfo fileInfo) ;

    @Query("DELETE FROM FileInfo")
    void deleteAll() ;

    @Query("SELECT * FROM FileInfo ORDER BY word ASC")
    LiveData<List<FileInfo>> getAllWords() ;
}
