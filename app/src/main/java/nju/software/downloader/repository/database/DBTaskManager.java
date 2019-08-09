package nju.software.downloader.repository.database;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.repository.database.dao.TaskDao;
import nju.software.downloader.repository.network.NetworkTaskManager;


/**
 * 管理数据库操作，将数据库操作分为读和写两种：
 *  1、读使用原生asyncTask实现,因为读主要是在初始化的时候获取任务列表，此时需要监控何时操作完成，使用asyncTask可以方便地知道结果
 *  2、写操作通过一个singleThread的线程池实现，这是为了避免sqlite.writeDatabase的多线程FileBlockException。同时也考虑到数据库的写操作对用户而言，不需要保持强一致性
 */
public class DBTaskManager {
    private TaskDao taskDao ;
    private ExecutorService writeExecutorService ;
    private NetworkTaskManager networkTaskManager ;

    public DBTaskManager(Application context){
        this.taskDao = new TaskDao(context);
        writeExecutorService = Executors.newSingleThreadExecutor();
    }

    public void getAllTasks(){
        new GetAllTask().execute() ;
    }

    /**
     * 向数据库插入，这个任务扔给线程池，不用知道什么时候成功
     * @param taskInfo
     */
    public void insert(TaskInfo taskInfo){
        writeExecutorService.execute(new InsertTask(taskInfo));
    }
    /**
     * 数据库删除，同理插入
     */
    public void delete(TaskInfo taskInfo){
        writeExecutorService.execute(new DeleteSingleTask(taskInfo));
    }

    /**
     *
     */
    public void multiDelete(List<TaskInfo> taskInfos){
        writeExecutorService.execute(new MultiDeleteTask(taskInfos));
    }

    /***
     *
     */
    public void update(TaskInfo taskInfo){
        writeExecutorService.execute(new UpdateTask(taskInfo));
    }

    public void setNetworkTaskManager(NetworkTaskManager networkTaskManager) {
        this.networkTaskManager = networkTaskManager;
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


    class DeleteSingleTask implements Runnable {
        private TaskInfo taskInfo;

        DeleteSingleTask(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }
        @Override
        public void run() {
            taskDao.delete(taskInfo.getId());
        }
    }

    class GetAllTask extends AsyncTask<Void,Void,List<TaskInfo> > {
        @Override
        protected List<TaskInfo> doInBackground(Void... voids) {
            return taskDao.findAll();
        }

        @Override
        protected void onPostExecute(List<TaskInfo> taskInfos) {
            super.onPostExecute(taskInfos);
            networkTaskManager.restartTasks(taskInfos); ;
        }
    }

    class MultiDeleteTask implements Runnable {
        private List<TaskInfo> tasks ;
        MultiDeleteTask(List<TaskInfo> tasks){
            this.tasks = tasks ;
        }
        @Override
        public void run() {
            if(tasks!=null&&tasks.size()>0){
                long[] ids = new long[tasks.size()] ;
                int index = 0 ;
                for(TaskInfo taskInfo:tasks){
                    ids[index++] = taskInfo.getId() ;
                }
                taskDao.deleteTasks(ids);
            }
        }
    }

    class UpdateTask implements Runnable {
        private TaskInfo taskInfo ;
        UpdateTask(TaskInfo taskInfo){
            this.taskInfo = taskInfo ;
        }
        @Override
        public void run() {
            taskDao.update(taskInfo);
        }
    }

}
