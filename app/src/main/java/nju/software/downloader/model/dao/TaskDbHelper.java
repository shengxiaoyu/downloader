package nju.software.downloader.model.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1 ;
    public static final String DATABASE_NAME = "downlaoder.db" ;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE "+ TaskContract.TaskEntry.TABLE_NAME+"("+
                    TaskContract.TaskEntry._ID+" INTEGER PRIMARY KEY,"+
                    TaskContract.TaskEntry.COLUMN_NAME_URL+ " TEXT,"+
                    TaskContract.TaskEntry.COLUMN_NAME_FILENAME+" TEXT,"+
                    TaskContract.TaskEntry.COLUMN_NAME_FINISHED+" INTEGER,"+
                    TaskContract.TaskEntry.COLUMN_NAME_PRIORITY+" INTEGER,"+
                    TaskContract.TaskEntry.COLUMN_NAME_JUMPTIMESTAMP+" INTEGER)" ;

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS "+ TaskContract.TaskEntry.TABLE_NAME ;
    public TaskDbHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}
