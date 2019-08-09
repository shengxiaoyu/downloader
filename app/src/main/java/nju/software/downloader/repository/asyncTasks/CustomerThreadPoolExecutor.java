package nju.software.downloader.repository.asyncTasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 继承并扩展了线程池，增加任务执行状态的记录，便于进行减少最大配置数时的操作。
 */
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
                //被发起中断的都是任务优先级低的，其余的自旋等待cutting过程结束
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

    /**
     * 当线程池活动线程数大于配置数时调用，因为涉及自旋耗时操作，使用异步线程进行
     * 实现方式是增加两个队列分别记录线程池等待队列中的downloadTask和正在运行的downloadTask,使用优先队列，所以高优先级的一定先出队
     *  当正在运行的队列数大于配置数时，
     *      先清空等待队列（清完再次检查）；
     *      将运行队列中的配置数大小的线程出队；
     *      将运行队列中剩余线程放行进入空闲态； (这个控制通过volatile关键字isTransfering控制，可见afterExecute方法，运行态的线程任务downloadTask一定是RUNNING，这个时候把需要中断的任务置为WAITTING态，并中断，放行这类线程进入空闲态，等待被回收）
     *      自旋等待空闲态的线程被回收完毕，将任务放回等待队列（可以直接替换）
     *
     *      !!!!!!!自旋等待导致配置数减少的时候会有漫长一段时间等待线程池状态回归！！！！！！，这个问题是为什么啊。。。来不及了 8.10
     *          哈哈，解决，问题出在本来是想将线程从RUNNING态置为WAITTING态，然后检测这个达到区分放行低优先级线程的目的。但是有时候线程从阻塞队列到达执行beforeExecutor之前，刚好在这段时间置为了WAITTING，而在
     *          beforeExecutor方法中会按常规顺序置为RUNNING状态，导致后面多个低优先级的线程也一直陷在自旋中。解决方法是，在设置为WAITTING状态前，自旋检测，当前任务必须以及设为RUNNING态，完美搞定~
     *
     */
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

                        //这里加个自旋，避免线程刚从等待队列获得线程执行权，还没进入beforeExecutore方法，导致在后面又将statue置为了RUNNING，自旋卡死。
                        while (true) {
                            if(next.isRunning()) {
                                next.waittting();
                                break;
                            }
                        }

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
