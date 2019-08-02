package nju.software.downloader.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import nju.software.downloader.entities.Word;
import nju.software.downloader.repository.WordRepository;

public class WordViewModel extends AndroidViewModel {
    private WordRepository repository ;
    private LiveData<List<Word>> words ;

    public WordViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application) ;
        words = repository.getAllWords() ;
    }

    public LiveData<List<Word>> getAllWords(){
        return words ;
    }

    public void insert(Word word){
        repository.insert(word);
    }
}
