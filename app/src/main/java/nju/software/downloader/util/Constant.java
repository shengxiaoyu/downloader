package nju.software.downloader.util;

public class Constant {

    //增加下载任务 意图标识
    public static final int NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE = 1;

//    public static String EXTERNAL_BASE_SAVE_PATH = "downloader" ;


    //申请外存读写权限代码
    public static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1 ;

    //同时最大下载数
    public static final int MAX_TASKS = 2 ;

    public static final int TIMES_UPDATE_PROGRESS = 20 ;

    public static final int KB = 1<<10 ;

    public static final int MB = 1<<20 ;

    public static final int GB = 1<<30 ;

    public static final String WAITTING = "Waiting..." ;

    public static final String PAUSE = "Pausing..." ;

    public static final String EMPTY = "No Task Info" ;
}
