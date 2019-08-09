package nju.software.downloader.util;

public class Constant {

    //增加下载任务 意图标识
    public static final int NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE = 1;

    public static final int CONFIGUATION_INTENT_CODE = 2 ;

    public static final int FINISHED_FLAG = 1 ;
    public static final int UNFINISHED_FLAG = 2 ;

    //申请外存读写权限代码
    public static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1 ;

    //同时最大下载数
    public static volatile int MAX_TASKS = 2 ;
    public static String MAX_TASKS_KEY = "number_of_max_tasks" ;

    public static final int KB = 1<<10 ;

    public static final int MB = 1<<20 ;

    public static final int GB = 1<<30 ;

    public static final String SPEED_OF_WAITTING = "Waiting..." ;

    public static final String SPEED_OF_PAUSE = "Pausing..." ;

    public static final String SPEED_OF_FINISHED = "" ;

    public static final String EMPTY = "No Task Info" ;

    public static final int REFRESH_PROGRESS_INTERVAL = 500 ;
}
