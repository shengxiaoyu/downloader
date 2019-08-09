package nju.software.downloader.model.repository.TaskManager.dbTasks;

import android.app.Application;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nju.software.downloader.model.repository.TaskManager.NetworkTaskManager;
import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.model.dao.TaskDao;
import nju.software.downloader.model.repository.TaskRepository;

/**
 * 管理数据库操作，将数据库操作分为读和写两种：
 *  1、读使用原生asyncTask实现,因为读主要是在初始化的时候获取任务列表，此时需要监控何时操作完成，使用asyncTask可以方便地知道结果
 *  2、写操作通过一个singleThread的线程池实现，这是为了避免sqlite.writeDatabase的多线程FileBlockException。同时也考虑到数据库的写操作对用户而言，不需要保持强一致性
 */
public class DBTaskManager {
    private TaskDao taskDao ;
    private ExecutorService writeExecutorService ;
    private TaskRepository taskRepository ;
    private NetworkTaskManager networkTaskManager ;

    public DBTaskManager(Application context,TaskRepository repository){
        this.taskDao = new TaskDao(context);
        writeExecutorService = Executors.newSingleThreadExecutor();
        this.taskRepository = repository ;
    }

    public void getAllTasks(){
        new GetAllTask(this,taskDao).execute() ;
    }
    /**
     * 这是GetAllTasks方法执行完毕回调的接口，通知manager已经完成,
     * 只对包内可见
     * @param taskInfos
     */
    void afterFindAll(List<TaskInfo> taskInfos) {
        //manager再去通知repository
        taskRepository.afterFindAll(taskInfos);
    }

    /**
     * 向数据库插入，这个任务扔给线程池，不用知道什么时候成功
     * @param taskInfo
     */
    public void insert(TaskInfo taskInfo){
        writeExecutorService.execute(new InsertTask(taskInfo));
    }
    public void afterInsert(TaskInfo taskInfo) {
        taskRepository.afterInsert(taskInfo) ;
    }
    /**
     * 数据库删除，同理插入
     */
    public void delete(TaskInfo taskInfo){
        writeExecutorService.execute(new DeleteSingleTask(taskDao,taskInfo));
    }

    /**
     *
     */
    public void multiDelete(List<TaskInfo> taskInfos){
        writeExecutorService.execute(new MultiDeleteTask(taskDao,taskInfos));
    }

    /***
     *
     */
    public void update(TaskInfo taskInfo){
        writeExecutorService.execute(new UpdateTask(taskDao,taskInfo));
    }

    class InsertTask implements Runnable {
        private TaskInfo taskInfo ;

        InsertTask(TaskInfo taskInfo){
            this.taskInfo = taskInfo ;
        }
        @Override
        public void run() {
            long rowId = taskDao.insert(taskInfo);
            //插入完成，更新一下Id这个属性，按值传递因此可以做到和其他地方同步
            taskInfo.setId(rowId);
            networkTaskManager.addTask(taskInfo);
        }
    }

    public void setNetworkTaskManager(NetworkTaskManager networkTaskManager) {
        this.networkTaskManager = networkTaskManager;
    }
}
