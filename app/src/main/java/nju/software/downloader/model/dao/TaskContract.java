package nju.software.downloader.model.dao;

import android.provider.BaseColumns;

public class TaskContract {
    private TaskContract(){}

    public static class TaskEntry implements BaseColumns{
        public static final String TABLE_NAME = "task" ;
        public static final String COLUMN_NAME_URL = "url" ;
        public static final String COLUMN_NAME_FILENAME = "fileName" ;
        public static final String COLUMN_NAME_FINISHED = "finished" ;
        public static final String COLUMN_NAME_PRIORITY = "PRIORITY" ;
        public static final String COLUMN_NAME_JUMPTIMESTAMP = "jumpTimeStamp" ;


    }
}
