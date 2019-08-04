package nju.software.downloader.page.taskList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.model.TaskInfo;

public class TaskListAdapter extends RecyclerView.Adapter {
    private final LayoutInflater mInflater;

    private List<TaskInfo> mTaskInfos; // Cached copy of words

    public TaskListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new FileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mTaskInfos != null) {
            TaskInfo current = mTaskInfos.get(position);
            ((FileViewHolder) holder).wordItemView.setText(current.getFileName()==null?current.getUrl():current.getFileName());
            ((FileViewHolder) holder).progressBar.setProgress(current.getProgress()==null?0:current.getProgress());

        } else {
            // Covers the case of data not being ready yet.
            ((FileViewHolder) holder).wordItemView.setText("No TaskInfo");
            ((FileViewHolder) holder).progressBar.setProgress(0);
        }
    }


    public void setFiles(List<TaskInfo> taskInfos){
        mTaskInfos = taskInfos;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mTaskInfos has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mTaskInfos != null)
            return mTaskInfos.size();
        else return 0;
    }
    //定义单个item如何展示
    class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordItemView;
        private final ProgressBar progressBar ;

        private FileViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.determinateBar) ;
            wordItemView = itemView.findViewById(R.id.textView);
        }
    }

    /**
     * 根据位置获取任务
     * @param position
     * @return
     */
    public TaskInfo getTaskAtPosition(int position){
        return mTaskInfos.get(position) ;
    }
}
