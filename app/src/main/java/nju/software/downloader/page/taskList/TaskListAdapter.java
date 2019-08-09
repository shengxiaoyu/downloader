package nju.software.downloader.page.taskList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.util.Constant;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskHolder> {
    private final LayoutInflater mInflater;

    private List<TaskInfo> mTaskInfos; // Cached copy of words
    private TaskViewModel taskViewModel ;
    private int flag ;//标识当前时完成任务还是未完成任务列表
    private Context context ;

    public TaskListAdapter(Context context, TaskViewModel myTaskViewModel, int flag) {
        this.context = context ;
        mInflater = LayoutInflater.from(context);
        this.taskViewModel = myTaskViewModel ;
        this.flag = flag ;
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new TaskHolder(itemView);

    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, final int position) {
        if (mTaskInfos != null) {
            TaskInfo current = mTaskInfos.get(position);
            holder.bind(current,position);
        } else {
            // Covers the case of data not being ready yet.
            holder.fileNameView.setText(Constant.EMPTY);
            holder.progressBar.setProgress(0);
        }
        holder.startBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.pasueOrBegin(position);
            }
        });
        holder.pauseBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.pasueOrBegin(position);
            }
        });
        holder.deleteBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.delete(mTaskInfos.get(position),flag);
            }
        });
        holder.selectBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.selectTask(position,flag);
            }
        });

        holder.upBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.move(position,position-1);
            }
        });

        holder.downBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskViewModel.move(position,position+1);
            }
        });
    }


    void setTasks(List<TaskInfo> taskInfos){
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
    public TaskInfo getTaskAtPosition(int position){
        return mTaskInfos.get(position) ;
    }



    //定义单个item如何展示
    class TaskHolder extends RecyclerView.ViewHolder
    {
        private final TextView fileNameView;
        private final TextView speedView ;
        private final ProgressBar progressBar ;
        private final Button startBt ;
        private final Button pauseBt ;
        private final Button deleteBt ;
        private final CheckBox  selectBox ;
        private final Button upBt ;
        private final Button downBt ;

        private TaskHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar) ;
            speedView = itemView.findViewById(R.id.speed_tv) ;
            fileNameView = itemView.findViewById(R.id.filename_tv);

            startBt = itemView.findViewById(R.id.start_btn);
            pauseBt = itemView.findViewById(R.id.pause_btn);
            deleteBt = itemView.findViewById(R.id.delete_btn) ;
            selectBox = itemView.findViewById(R.id.select_box) ;

            upBt = itemView.findViewById(R.id.up_btn);
            downBt = itemView.findViewById(R.id.down_btn);
        }
        //单个item如何展示
        void bind(final TaskInfo current,int position){
            if(current==null){
                fileNameView.setText(Constant.EMPTY);
                progressBar.setProgress(0);
            }
            fileNameView.setText(current.getFileName()==null?current.getUrl():current.getFileName());
            progressBar.setProgress(current.getProgress());

            if(current.isPaused()){
                //暂停和其他状态的进度条区别显示
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

            if(current.isPaused()){
                pauseBt.setVisibility(View.GONE);
                startBt.setVisibility(View.VISIBLE);
            }else {
                startBt.setVisibility(View.GONE);
                pauseBt.setVisibility(View.VISIBLE);
            }
            if(current.isFinished()){
                startBt.setVisibility(View.GONE);
                pauseBt.setVisibility(View.GONE);
                upBt.setVisibility(View.GONE);
                downBt.setVisibility(View.GONE);
            }
            selectBox.setChecked(current.isSelected());
        }
    }

}
