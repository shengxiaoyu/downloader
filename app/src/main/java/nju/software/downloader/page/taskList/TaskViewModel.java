package nju.software.downloader.page.taskList;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nju.software.downloader.model.repository.TaskRepository;
import nju.software.downloader.model.repository.entities.TaskInfo;

//以一种不受配置更新影响的形式保存ui数据，不能传递context到viewmodel中
    public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository ;
    private MutableLiveData<List<TaskInfo>> finishedTasks ;
    private MutableLiveData<List<TaskInfo>> unfinishedTasks ;
    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application,this) ;
        finishedTasks = new MutableLiveData<>();
        finishedTasks.postValue(new CopyOnWriteArrayList<TaskInfo>());
        unfinishedTasks = new MutableLiveData<>() ;
        unfinishedTasks.postValue(new CopyOnWriteArrayList<TaskInfo>());
    }

    LiveData<List<TaskInfo>> getUnfinishedTasks(){
        return finishedTasks ;
    }

    LiveData<List<TaskInfo>> getFinishedTasks(){
        return unfinishedTasks ;
    }

    void multiDelete(){
        repository.multiDelete() ;
    }
    public void addNewDownloadTask(String url) {
        repository.insert(url);
    }
    /**
     *
     * @param taskInfo
     * @param taskFlag
     */
    void delete(TaskInfo taskInfo, int taskFlag){
        repository.delete(taskInfo.getId(),taskFlag);
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


    public void update(Long id, int progress, String speed) {
        List<TaskInfo> value = unfinishedTasks.getValue();
        if(value!=null && value.size()!=0){
            for(TaskInfo TaskInfo:value){
                if(TaskInfo.g().equals())
            }
        }
    }

    public void addTaskInfo(TaskInfo TaskInfo) {
        List<TaskInfo> value = unfinishedTasks.getValue();
        value.add(TaskInfo) ;
        unfinishedTasks.postValue(value);
    }
}
