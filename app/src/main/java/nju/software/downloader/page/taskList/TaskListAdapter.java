package nju.software.downloader.page.taskList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.util.Constant;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskHolder> {
    private final LayoutInflater mInflater;

    private List<TaskVO> mTaskInfos; // Cached copy of words

    private Context context ;

    public TaskListAdapter(Context context) {
        this.context = context ;
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new TaskHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, int position) {
        if (mTaskInfos != null) {
            TaskVO current = mTaskInfos.get(position);
            holder.bind(current);
        } else {
            // Covers the case of data not being ready yet.
            holder.fileNameView.setText(Constant.EMPTY);
            holder.progressBar.setProgress(0);
        }
    }


    void setTasks(List<TaskVO> taskInfos){
        mTaskInfos = taskInfos;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mTaskInfos != null)
            return mTaskInfos.size();
        else return 0;
    }

    /**
     * 根据位置获取任务
     * @param position
     * @return
     */
    public TaskVO getTaskAtPosition(int position){
        return mTaskInfos.get(position) ;
    }


    //定义单个item如何展示
    class TaskHolder extends RecyclerView.ViewHolder
    {
        private final TextView fileNameView;
        private final TextView speedView ;
        private final ProgressBar progressBar ;
        private final ImageView selectView;

        private TaskHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar) ;
            speedView = itemView.findViewById(R.id.speed_tv) ;
            fileNameView = itemView.findViewById(R.id.filename_tv);
            selectView = itemView.findViewById(R.id.select_iv) ;
        }
        //单个item如何展示
        void bind(final TaskVO current){
            if(current==null){
                fileNameView.setText(Constant.EMPTY);
                progressBar.setProgress(0);
            }
            fileNameView.setText(current.getFileName());
            progressBar.setProgress(current.getProgress());

            if(current.isPaused()){
                final Drawable drawable;
                int sdk = android.os.Build.VERSION.SDK_INT;
                if(sdk < 16) {
                    drawable =  context.getResources().getDrawable(R.drawable.pause_progress_bar);
                } else {
                    drawable = ContextCompat.getDrawable(context, R.drawable.pause_progress_bar);
                }
                progressBar.setProgressDrawable(drawable) ;
            }else {
                final Drawable drawable;
                int sdk = android.os.Build.VERSION.SDK_INT;
                if(sdk < 16) {
                    drawable =  context.getResources().getDrawable(R.drawable.running_progress_bar);
                } else {
                    drawable = ContextCompat.getDrawable(context, R.drawable.running_progress_bar);
                }
                progressBar.setProgressDrawable(drawable) ;
            }
            speedView.setText(current.getSpeed());
            selectView.setVisibility(current.isSelected()?View.VISIBLE:View.GONE);
        }
    }

}
