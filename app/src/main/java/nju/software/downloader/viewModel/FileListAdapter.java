package nju.software.downloader.ViewModel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.entities.FileInfo;

public class WordListAdapter extends RecyclerView.Adapter {
    private final LayoutInflater mInflater;
    private List<FileInfo> mFileInfos; // Cached copy of words

    public WordListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public WordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new WordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder = (WordViewHolder)holder ;
        if (mFileInfos != null) {
            FileInfo current = mFileInfos.get(position);
            ((WordViewHolder) holder).wordItemView.setText(current.getWord());
        } else {
            // Covers the case of data not being ready yet.
            ((WordViewHolder) holder).wordItemView.setText("No FileInfo");
        }
    }


    public void setWords(List<FileInfo> fileInfos){
        mFileInfos = fileInfos;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mFileInfos has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mFileInfos != null)
            return mFileInfos.size();
        else return 0;
    }

    class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordItemView;

        private WordViewHolder(View itemView) {
            super(itemView);
            wordItemView = itemView.findViewById(R.id.textView);
        }
    }
}
