package nju.software.downloader.model.repository.TaskManager;

import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nju.software.downloader.model.repository.TaskManager.dbTasks.DBTaskManager;
import nju.software.downloader.model.repository.TaskRepository;
import nju.software.downloader.model.repository.entities.TaskInfo;
import nju.software.downloader.util.Constant;

public class NetworkTaskManager {
    private static final String LOG_TAG = NetworkTaskManager.class.getSimpleName() ;
    private volatile TaskListLiveData unfinishedTaskListLiveData = new TaskListLiveData();
    private volatile TaskListLiveData finishedTaskListLiveData = new TaskListLiveData() ;

    private ThreadPoolExecutor executor ;
    private TaskRepository taskRepository ;
    private DBTaskManager dbTaskManager ;
    public NetworkTaskManager(TaskRepository taskRepository, DBTaskManager dbTaskManager){
        this.executor = new ThreadPoolExecutor(Constant.MAX_TASKS,Constant.MAX_TASKS,0L, TimeUnit.MILLISECONDS,new PriorityBlockingQueue<Runnable>()) ;
        this.taskRepository = taskRepository ;

        this.dbTaskManager = dbTaskManager ;
        dbTaskManager.setNetworkTaskManager(this);
    }

    //重启任务
    public void restartTasks(List<TaskInfo> taskInfos){
        CopyOnWriteArrayList<TaskInfo> unfinishedTasks = new CopyOnWriteArrayList<>() ;
        CopyOnWriteArrayList<TaskInfo> finishedTasks = new CopyOnWriteArrayList<>() ;
        if(taskInfos!=null && taskInfos.size()>0){
            for(TaskInfo taskInfo:taskInfos){
                if(taskInfo.isFinished()){
                    taskInfo.setSpeed(Constant.SPEED_OF_FINISHED);
                    taskInfo.setProgress(100);
                    finishedTasks.add(taskInfo) ;
                }else{
                    unfinishedTasks.add(taskInfo) ;
                }
            }
        }
        //放入缓存数据
        unfinishedTaskListLiveData.postValue(unfinishedTasks);
        finishedTaskListLiveData.postValue(finishedTasks);


        if(unfinishedTasks==null||unfinishedTasks.size()==0){
            return;
        }
        for(TaskInfo taskInfo:unfinishedTasks) {
            if(!taskInfo.isFinished()) {
                addTask(taskInfo);
            }
        }
    }

    /**
     * 新增任务
     * @param taskInfo
     */
    public void addTask(TaskInfo taskInfo){
        TaskInfo
        taskInfo.waitting();
        this.executor.execute(taskInfo);
    }

    /**
     * 删除任务
     * @param taskInfo
     */
    public void cancelTask(TaskInfo taskInfo){
        taskInfo.cancel();
        //从监听队列删除
        unfinishedTaskListLiveData.delete(taskInfo);
        executor.remove(taskInfo) ;
    }

    /**
     * 删除已选任务
     */
    public void multiDelete() {
        List<TaskInfo> unfinishedTasks = unfinishedTaskListLiveData.getValue();
        List<TaskInfo> toDeleteTasks = new ArrayList<>() ;
        if(unfinishedTasks!=null && unfinishedTasks.size()!=0){
            for(TaskInfo taskInfo:unfinishedTasks){
                if (taskInfo.isSelected()){
                    //先停止下载任务
                    taskInfo.cancel();
                    executor.remove(taskInfo) ;
                }
            }
        }
        //删除缓存，更新前端
        unfinishedTaskListLiveData.multiDelete(toDeleteTasks) ;
        taskRepository.afterMultiDelete(toDeleteTasks);

        List<TaskInfo> finishedTasks = finishedTaskListLiveData.getValue();
        toDeleteTasks = new ArrayList<>() ;
        for(TaskInfo taskInfo:finishedTasks){
            if (taskInfo.isSelected()){
                toDeleteTasks.add(taskInfo) ;
            }
        }
        finishedTaskListLiveData.multiDelete(toDeleteTasks) ;
    }
    /**
     * 暂停任务
     * @param taskInfo
     */
    public void pauseTask(TaskInfo taskInfo){
        taskInfo.pause();
    }

    /**
     * 更新最大任务数
     * @param newNumberOfMaxTasks
     */
    public void updateMaxTask(int newNumberOfMaxTasks){
        if(newNumberOfMaxTasks!= Constant.MAX_TASKS) {
            this.executor.setCorePoolSize(newNumberOfMaxTasks);
            this.executor.setMaximumPoolSize(newNumberOfMaxTasks);
        }
    }


    public LiveData<List<TaskInfo>> getUnfinishedTasks() {
        return unfinishedTaskListLiveData;
    }

    public LiveData<List<TaskInfo>> getFinishedTasks(){
        return finishedTaskListLiveData ;
    }

    public void moveTask(int oldPosition, int targetPosition) {
        TaskInfo movingTask = unfinishedTaskListLiveData.get(oldPosition);
        TaskInfo targetPositionTask = unfinishedTaskListLiveData.get(targetPosition) ;

        //检测运行状态
        if(oldPosition>targetPosition){
            //更新任务优先级，
            movingTask.setPriority(targetPositionTask.getPriority());
            movingTask.setJumpTimeStamp(System.currentTimeMillis());

            //往前插，提高优先级：如果当前线程处于WAITTING状态，检测插队的后面是否有正在执行的任务，有的话就停止，让出线程；
            // //           如果当前线程处于RUNNING，不做处理；
            //              如果当前线程处于CANCEL，不做处理。

            //先检测移动任务处于什么状态
            if(movingTask.isPause()){
                //暂停态,不做处理
            }else if(movingTask.isFinished()){
                //任务刚好完成，不做处理
            }
            if(movingTask.isWaitting()){
                //等待态，需要考虑新位置后面是否有正在执行的

                //重新入队

                //先取消再入队
                movingTask.cancel();
                executor.remove(movingTask) ;

                //再重新入队，为了更新优先级
                executor.execute(movingTask);
                movingTask.waitting();
                //一会一起更新，减少界面刷新频次
                unfinishedTaskListLiveData.updateValueNotPost(movingTask);


                //然后看看有没有优先级低的在运行态
                List<TaskInfo> subTasks = unfinishedTaskListLiveData.get(targetPosition, oldPosition);

                //从后往前遍历，看看如果有任务在执行，则停止，让出线程
                TaskInfo backTask ;
                for(int i=subTasks.size()-1;i>=0;i--){
                    backTask = subTasks.get(i);
                    if(backTask.isRunning()){
                        //找到第一个运行态的，结束让出线程
                        backTask.cancel();
                            //把人家放回去
                        executor.execute(backTask);
                        unfinishedTaskListLiveData.updateValueNotPost(backTask);
                        break;
                    }
                }
            }else {
                //运行态，已经提高优先级，不用处理
            }
        }
        else {
            //往后，降低优先级
            //更新任务优先级，
            movingTask.setPriority(targetPositionTask.getPriority());
            movingTask.setJumpTimeStamp(targetPositionTask.getJumpTimeStamp()-1);

            //如果当前线程处于RUNNING状态，检测新位置靠前是否有任务在等待态，如果有，则停止当前任务的执行，让出线程；
            //                          如果当前线程处于WAITTING状态，重新入队列；
            //                           如果当前线程处于CANCEL状态，不做处理
            if(movingTask.isPause()){
                //暂停态，不处理
            }else if(movingTask.isFinished()){
                //完成态，不处理
            }else if(movingTask.isRunning()){
                ///运行态
                //遍历新位置之前的任务，如果有等待态的，则让出线程
                List<TaskInfo> subList = unfinishedTaskListLiveData.get(oldPosition+1, targetPosition+1);
                //从高优先级的开始遍历
                TaskInfo forwardTask ;
                for(int i=0;i<subList.size();i++){
                    forwardTask = subList.get(i);
                    if(forwardTask.isWaitting()){
                        //让出线程
                        movingTask.cancel();
                        //重新进入等待
                        executor.execute(movingTask);
                        movingTask.waitting();

                        unfinishedTaskListLiveData.updateValueNotPost(movingTask);
                        break;
                    }
                }
            }else if(movingTask.isWaitting()){
                //等待态，重新入队
                executor.remove(movingTask) ;
                executor.execute(movingTask);
            }
        }
        //更新缓存数据，以更新前端
        unfinishedTaskListLiveData.move(oldPosition,targetPosition) ;
    }

    /**
     * delete finished task
     * @param task
     */
    public void delete(TaskInfo task) {
        finishedTaskListLiveData.delete(task);
    }

    public void pauseOrBegin(int postiton) {
        TaskInfo task = unfinishedTaskListLiveData.get(postiton);
        if (task.isFinished())
            return;
        if (task.isPause()) {
            //原先是暂停状态
            /**
             * 重新进入下载队列
             */
            addTask(task);
            //更新缓存和前端数据
            unfinishedTaskListLiveData.updateValue(task);
        } else if(task.isWaitting() || task.isRunning()){
            //原先是等待，或者running态
            executor.remove(task) ;
            task.pause();
            //提醒更新界面
            unfinishedTaskListLiveData.updateValue(task);
        }
    }

    /**
     * 选中
     * @param position
     * @param taskFlag
     */
    public void selectTask(int position, int taskFlag) {
        if(taskFlag==Constant.UNFINISHED_FLAG){
            TaskInfo taskInfo = unfinishedTaskListLiveData.get(position);
            taskInfo.setSelected(!taskInfo.isSelected());
            unfinishedTaskListLiveData.updateValue(taskInfo);
        }else{
            TaskInfo taskInfo = finishedTaskListLiveData.get(position);
            taskInfo.setSelected(!taskInfo.isSelected());
            finishedTaskListLiveData.updateValue(taskInfo);
        }
    }

    class DownloadTask implements Runnable{

        private volatile Long id ;

        private URL url ;

        //存储文件名
        private File saveFile ;

        //任务优先级
        private int priority ;

        //插队的时间戳
        private long jumpTimeStamp;

        //下载进度
        private int progress ;

        //下载速度
        private String speed = Constant.SPEED_OF_WAITTING;

        /**
         * 标识当前下载任务的状态，
         */
        private volatile int state ;

        /**
         * 任务的六种状态
         */
        private static final int WAITTING = 1 ;
        private static final int RUNNING = 1<<1 ;
        private static final int PAUSE = 1<<2 ;
        private static final int FINISH = 1<<3 ;
        private static final int CANCEL = 1<<4 ;
        @Override
        public void run() {
            if(saveFile.exists()){
                try {
                    resume(saveFile);
                }catch (IllegalStateException e){
                    //不支持断点重传，
                    downloadDirectly(saveFile);
                }
            }else {
                downloadDirectly(saveFile);
            }
        }

        private void resume(File saveFile) throws IllegalStateException {
            HttpURLConnection connection = null;
            InputStream input = null;
            RandomAccessFile rwd = null ;
            try {
                URL _URL = new URL(this.url);
                connection = (HttpURLConnection) _URL.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }
                Log.d(LOG_TAG, "网络连接成功");

                long beginPosition = saveFile.length();
                connection.setRequestProperty("Range", "bytes=" + beginPosition + "-");
                try {
                    input = connection.getInputStream();
                    //断点续传
                } catch (IllegalStateException e) {
                    //有可能不支持断点续传
                    throw e;
                }

                //状态设置为运行
                state = state|RUNNING ;

                int fileLength = connection.getContentLength();
                rwd = new RandomAccessFile(saveFile, "rwd");
                byte data[] = new byte[4096];
                //一次之内的下载量
                long num = 0;
                //总体下载量
                long total = beginPosition;
                //单个循环下载量
                int count;
                long beginTime = System.currentTimeMillis() ;
                long endTime ;
                while ((count = input.read(data)) != -1) {
                    if(isRunning()){
                        //只要不再是运行态，说明线程需要让出
                        return;
                    }
                    // allow canceling
                    num += count;
                    rwd.write(data, 0, count);

                    // 更新进度条,暂不更新数据库，等退出或者结束的时候一起更新,这样虽然可能导致进度条和真是下载长度不一致，但问题不大
                    if (fileLength > 0 && num>fileLength/ Constant.TIMES_UPDATE_PROGRESS) { // only if total length is known
                        endTime = System.currentTimeMillis() ;
                        long _Speed = 0 ;
                        if(endTime!=beginTime){
                            _Speed = num/(1+(endTime-beginTime)/1000) ;
                        }
                        if(_Speed>Constant.GB){
                            this.speed = (_Speed/Constant.GB+"GB/s");
                        }else if(_Speed>Constant.MB){
                            this.speed = _Speed/Constant.MB+"MB/s";
                        }else if(_Speed>Constant.KB){
                            this.speed = _Speed/Constant.KB+"KB/s";
                        }else {
                            if(_Speed==0){
                                this.speed = "- B/s";
                            }else {
                                this.speed = _Speed + "B/s";
                            }
                        }
                        this.progress = (int) (total * 100 / fileLength);
                        num = 0;
                        unfinishedTaskListLiveData.updateValue(this);

                        beginTime = System.currentTimeMillis() ;
                    }
                }
                //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
                progress = 100 ;
                finish();
                unfinishedTaskListLiveData.updateValue(this);
                finishedTaskListLiveData.addValue(this);
                //更新数据库
                dbTaskManager.update(this);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (rwd != null)
                        rwd.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }

        }

        private void downloadDirectly(File saveFile){
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }
                Log.d(LOG_TAG, "网络连接成功");

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(saveFile) ;
                Log.d(LOG_TAG,"下载保存地址："+saveFile.getAbsolutePath()) ;
                byte[] data = new byte[4096];

                //单次刷新下载量
                long num = 0 ;
                //总体下载量
                long total = 0;
                int count;
                int changeProgress ;
                long beginTime = System.currentTimeMillis() ;
                long endTime ;
                while ((count = input.read(data)) != -1) {
                    if(isRunning()){
                        //只要不再是运行态，说明线程需要让出
                        return;
                    }
                    num += count ;
                    //在这里更新总量，避免最后一次不进入下面的if
                    total += count ;
                    output.write(data, 0, count);

                    //跟新进度条，时间间隔
                    if (((endTime=System.currentTimeMillis())-beginTime)>Constant.REFRESH_PROGRESS_INTERVAL) { // only if total length is known
                        long _Speed = num/((endTime-beginTime)/1000) ;
                        if(_Speed>Constant.GB){
                            this.speed = (_Speed/Constant.GB+"GB/s");
                        }else if(_Speed>Constant.MB){
                            this.speed = _Speed/Constant.MB+"MB/s";
                        }else if(_Speed>Constant.KB){
                            this.speed = _Speed/Constant.KB+"KB/s";
                        }else {
                            if(_Speed==0){
                                this.speed = "- B/s";
                            }else {
                                this.speed = _Speed + "B/s";
                            }
                        }

                        //如果进度小于1，不更新
                        changeProgress = (int)num*100/fileLength ;
                        if(changeProgress<1){
                            continue;
                        }


                        this.progress = (int) (total * 100 / fileLength);
                        num = 0;

                        if(isCancel()||isPause()){
                            return;
                        }
                        taskRepository.update(this.id,this.progress,this.speed) ;
                        beginTime = System.currentTimeMillis() ;
                    }
                }
                //下载完成进度条一定位100，也为了避免不知道下载总长度的情况
                progress = 100 ;
                finish();
                unfinishedTaskListLiveData.updateValue(this);
                finishedTaskListLiveData.addValue(this);
                //更新数据库
                dbTaskManager.update(this);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
        }
        boolean isRunning(){
            return state != RUNNING;
        }

        public void pause(){
            state = PAUSE ;
            speed = Constant.SPEED_OF_PAUSE ;
        }
        boolean isPause(){
            return state==PAUSE ;
        }

        public void cancel(){
            state = CANCEL ;
        }
        boolean isCancel(){
            return state==CANCEL ;
        }

        void finish(){
            state = FINISH ;
        }
        public boolean isFinished(){
            return state == FINISH;
        }

        public void waitting(){
            speed = Constant.SPEED_OF_WAITTING ;
            state = WAITTING ;
        }
        public boolean isWaitting(){
            return state==WAITTING;
        }
    }
}
