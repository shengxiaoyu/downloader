package nju.software.downloader.page.taskList;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.storage.repository.TaskRepository;

//以一种不受配置更新影响的形式保存ui数据，不能传递context到viewmodel中
public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository ;
    private LiveData<List<TaskInfo>> files ;


    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application) ;
        files = repository.getAllFiles() ;
    }

    public LiveData<List<TaskInfo>> getAllFiles(){
        return repository.getAllFiles() ;
    }

    public void insert(TaskInfo taskInfo){
        Log.d("TaskViewModel","保存下载信息:"+ taskInfo.getUrl()) ;
        repository.insert(taskInfo);
    }

    public void deleteALl(){
        repository.deleteAll();
    }

    public void delete(TaskInfo file){
        repository.delete(file);
    }

}
