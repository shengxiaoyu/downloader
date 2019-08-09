//package nju.software.downloader.util;
//
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//import nju.software.downloader.model.repository.TaskManager.downloadTaskManager.DownloadTask;
//
//public class CustomerThreadPoolExecutor extends ThreadPoolExecutor {
//    public CustomerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
//        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
//    }
//
//    @Override
//    protected void beforeExecute(Thread t, Runnable r) {
//        if(r instanceof DownloadTask){
//            //开始前，检查任务状态，任务可能已经取消了，或者删除了
//            DownloadTask downloadTask = ((DownloadTask)r);
//            if(downloadTask.getStatus()!=DownloadTask.WAITTING){
//                //任务只能从waitting状态进入running态，如果是delete,pause都直接中断退出
//                Thread.currentThread().interrupt();
//            }else {
//                downloadTask.setRunningThread(Thread.currentThread());
//                downloadTask.setStatus(DownloadTask.RUNNING);
//            }
//        }
//        super.beforeExecute(t, r);
//    }
//
//    @Override
//    protected void afterExecute(Runnable r, Throwable t) {
//        super.afterExecute(r, t);
//
//        if(r instanceof DownloadTask){
//            DownloadTask downloadTask = ((DownloadTask)r);
//            if(downloadTask.getStatus()==DownloadTask.RUNNING){
//                //只能从RUNNING态进入
//                downloadTask.setRunningThread(null);
//                downloadTask.setStatus(DownloadTask.FINISHED);
//            }
//
//        }
//    }
//}
