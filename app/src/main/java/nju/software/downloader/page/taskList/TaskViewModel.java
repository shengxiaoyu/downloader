package nju.software.downloader.page.taskList;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.repository.TaskRepository;

//以一种不受配置更新影响的形式保存ui数据，不能传递context到viewmodel中
public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository ;


    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application) ;
    }

    LiveData<List<TaskInfo>> getUnfinishedTasks(){
        return repository.getUnfinishedTasks() ;
    }

    LiveData<List<TaskInfo>> getFinishedTasks(){
        return repository.getFinishedTasks() ;
    }



    void insert(TaskInfo taskInfo){
        Log.d("TaskViewModel","保存下载信息:"+ taskInfo.getUrl()) ;
        repository.insert(taskInfo);
    }

    void multiDelete(){
        repository.multiDelete() ;
    }

    /**
     *
     * @param taskInfo
     * @param taskFlag
     */
    void delete(TaskInfo taskInfo,int taskFlag){
        repository.delete(taskInfo,taskFlag);
    }

    void pasueOrBegin(int postiton) {
        repository.pauseOrBegin(postiton) ;
    }

    public void selectTask(int taskInfo,int taskFlag) {
        repository.selectTask(taskInfo,taskFlag) ;
    }

    public void move(int oldPosition, int targetPosition) {
        repository.move(oldPosition,targetPosition) ;
    }

    public void changeMaxConnectionNumber(int max_connection_number) {
        repository.changeMaxTaskNumbers(max_connection_number) ;
    }
}
