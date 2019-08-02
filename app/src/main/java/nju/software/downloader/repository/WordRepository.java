package nju.software.downloader.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.nio.channels.AsynchronousChannel;
import java.util.List;

import nju.software.downloader.dao.WordDao;
import nju.software.downloader.entities.Word;
import nju.software.downloader.room.WordRoomDatabase;

public class WordRepository {
    private WordDao wordDao ;
    private LiveData<List<Word>> words ;
    public WordRepository(Application application){
        WordRoomDatabase db = WordRoomDatabase.getDatabsae(application) ;
        wordDao = db.wordDao() ;
        words = wordDao.getAllWords() ;
    }

    public LiveData<List<Word>> getAllWords(){
        return words ;
    }

    public void insert(Word word){
        new insertAsyncTask(wordDao).execute(word) ;
    }
    private static class insertAsyncTask extends AsyncTask<Word,Void,Void>{
        private WordDao asyncTaskDao ;

        insertAsyncTask(WordDao dao){
            this.asyncTaskDao = dao ;
        }
        @Override
        protected Void doInBackground(Word... words) {
            asyncTaskDao.insert(words[0]);
            return null;
        }
    }
}
