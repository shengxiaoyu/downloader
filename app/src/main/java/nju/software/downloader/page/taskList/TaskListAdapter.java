package nju.software.downloader.page.taskList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.util.Constant;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskHolder> {
    private final LayoutInflater mInflater;

    private List<TaskInfo> mTaskInfos; // Cached copy of words
    private TaskViewModel taskViewModel ;

    private Context context ;

    public TaskListAdapter(Context context,TaskViewModel myTaskViewModel) {
        this.context = context ;
        mInflater = LayoutInflater.from(context);
        this.taskViewModel = myTaskViewModel ;
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
            TaskInfo current = mTaskInfos.get(position);
            holder.bind(current);
        } else {
            // Covers the case of data not being ready yet.
            holder.fileNameView.setText(Constant.EMPTY);
            holder.progressBar.setProgress(0);
        }
    }


    void setFiles(List<TaskInfo> taskInfos){
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
    class TaskHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
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
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }
        //单个item如何展示
        void bind(final TaskInfo current){
            if(current==null){
                fileNameView.setText(Constant.EMPTY);
                progressBar.setProgress(0);
            }
            fileNameView.setText(current.getFileName()==null?current.getUrl():current.getFileName());
            progressBar.setProgress(current.getProgress()==null?0:current.getProgress());

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

        //单击：暂停、开始
        @Override
        public void onClick(View view) {
            int layoutPosition = getLayoutPosition();
            TaskInfo taskInfo = mTaskInfos.get(layoutPosition);
            /**
             * 暂停或继续
             */
            if(!taskInfo.isFinished()) {
                taskViewModel.pasueOrBegin(taskInfo);
            }
        }

        //长按：选中
        @Override
        public boolean onLongClick(View view) {
            int layoutPosition = getLayoutPosition();
            TaskInfo taskInfo = mTaskInfos.get(layoutPosition);
            taskViewModel.selectTask(taskInfo) ;
            Toast.makeText(context,"选中"+layoutPosition,Toast.LENGTH_SHORT).show();
            return true;
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
