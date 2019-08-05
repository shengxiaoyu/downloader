package nju.software.downloader.storage.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.storage.dao.TaskDao;

//定义哪些entities属于此数据库
@Database(entities = {TaskInfo.class},version = 2,exportSchema = false)
public abstract class TaskRoomDatabase extends RoomDatabase {

    public abstract TaskDao taskDao() ;

    private static volatile TaskRoomDatabase INSTANCE ;

    //单例模式
    public static TaskRoomDatabase getDatabsae(final Context context){
        if(INSTANCE==null){
            synchronized (TaskRoomDatabase.class){
                if(INSTANCE==null){
                    //创建数据库
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TaskRoomDatabase.class,"file_database")
                            //定义如何维护数据库变化，在此简单地重建删除
                            .fallbackToDestructiveMigration()
                            .build() ;
                }
            }
        }
        return INSTANCE ;
    }
    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @Override
    public void clearAllTables() {

    }
}
