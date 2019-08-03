package nju.software.downloader.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

import nju.software.downloader.dao.WordDao;
import nju.software.downloader.entities.FileInfo;
import nju.software.downloader.room.WordRoomDatabase;

public class WordRepository {
    private WordDao wordDao ;
    private LiveData<List<FileInfo>> words ;
    public WordRepository(Application application){
        WordRoomDatabase db = WordRoomDatabase.getDatabsae(application) ;
        wordDao = db.wordDao() ;
        words = wordDao.getAllWords() ;
    }

    public LiveData<List<FileInfo>> getAllWords(){
        return words ;
    }

    public void insert(FileInfo fileInfo){
        new insertAsyncTask(wordDao).execute(fileInfo) ;
    }
    private static class insertAsyncTask extends AsyncTask<FileInfo,Void,Void>{
        private WordDao asyncTaskDao ;

        insertAsyncTask(WordDao dao){
            this.asyncTaskDao = dao ;
        }
        @Override
        protected Void doInBackground(FileInfo... fileInfos) {
            asyncTaskDao.insert(fileInfos[0]);
            return null;
        }
    }
}
