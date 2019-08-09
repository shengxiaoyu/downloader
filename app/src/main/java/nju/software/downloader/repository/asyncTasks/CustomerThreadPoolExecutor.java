package nju.software.downloader.repository.asyncTasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomerThreadPoolExecutor extends ThreadPoolExecutor {

    private PriorityBlockingQueue<DownloadTask> waittingQueue  ;
    private PriorityBlockingQueue<DownloadTask> runningQueue ;
    private volatile boolean isTransfering ;
    public CustomerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<Runnable>());
        waittingQueue = new PriorityBlockingQueue<DownloadTask>() ;
        runningQueue = new PriorityBlockingQueue<DownloadTask>() ;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if(r instanceof DownloadTask){
            //开始前，检查任务状态，任务可能已经取消了，或者删除了
            DownloadTask downloadTask = ((DownloadTask)r);
            if(downloadTask.getStatus()!=DownloadTask.WAITTING){
                //任务只能从waitting状态进入running态，如果是delete,pause都直接中断退出
                Thread.currentThread().interrupt();
            }else {
                downloadTask.setRunningThread(Thread.currentThread());
                downloadTask.setStatus(DownloadTask.RUNNING);
                waittingQueue.remove(downloadTask) ;
                runningQueue.add(downloadTask) ;
            }
        }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if(r instanceof DownloadTask){
            DownloadTask downloadTask = ((DownloadTask)r);
            while (isTransfering){
                //被发起中断的都是任务优先级低的
                if (Thread.currentThread().isInterrupted()|| downloadTask.isWaitting()) {
                    return ;
                }
            }

            if(downloadTask.getStatus()==DownloadTask.RUNNING){
                //只能从RUNNING态进入
                downloadTask.setRunningThread(null);
                downloadTask.setStatus(DownloadTask.FINISHED);
                runningQueue.remove(downloadTask) ;
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        if(command instanceof DownloadTask) {
            waittingQueue.add((DownloadTask) command);
        }
        super.execute(command);
    }

    public void cutting(){
        new cutting().execute(this) ;
    }
    class cutting extends AsyncTask<ThreadPoolExecutor,Void,Void>{

        @Override
        protected Void doInBackground(ThreadPoolExecutor... ThreadPoolExecutors) {
            int activeCount ;
            int corePoolSize ;
            if((activeCount=getActiveCount())>getCorePoolSize()){
                //清空阻塞队列
                PriorityBlockingQueue<DownloadTask> newWaittingQueue = new PriorityBlockingQueue<>(waittingQueue.size()+runningQueue.size()) ;
                waittingQueue.drainTo(newWaittingQueue) ;

                //再确定一次
                if((activeCount=getActiveCount())>(corePoolSize=getCorePoolSize())){
                    //让所有的运行线程不会刚好此时进入空闲态
                    //在这里主要是为了让优先级低的任务所在的线程中断
                    isTransfering = true ;
                    PriorityBlockingQueue<DownloadTask> newRunningQueue = new PriorityBlockingQueue<>(activeCount) ;
                    runningQueue.drainTo(newRunningQueue,corePoolSize) ;

                    //将running队列中剩余的任务线程都中断
                    Iterator<DownloadTask> iterator = runningQueue.iterator();

                    ArrayList<DownloadTask> tmpWaittingQueue = new ArrayList<>(activeCount-corePoolSize) ;
                    while (iterator.hasNext()) {
                        DownloadTask next = iterator.next();
                        next.waittting();

                        //进入临时队列等待，这时候不能直接进入线程池的阻塞队列
                        tmpWaittingQueue.add(next) ;
                    }

                    while (getActiveCount()>getCorePoolSize()){
                        //自旋等待线程空闲线程被回收
                        System.out.println("自旋等待空闲线程被回收");
                    }

                    //恢复
                    waittingQueue = newWaittingQueue ;
                    //回收结束，重新入队
                    for(DownloadTask downloadTask:tmpWaittingQueue){
                        ThreadPoolExecutors[0].execute(downloadTask);

                    }

                    //结束转移
                    isTransfering = false ;
                }
            }
            return null ;
        }
    }
}
