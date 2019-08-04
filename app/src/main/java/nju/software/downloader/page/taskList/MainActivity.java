package nju.software.downloader.page.taskList;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.page.addTask.AddTaskActivity;
import nju.software.downloader.model.TaskInfo;

import static nju.software.downloader.util.Constant.NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    //activity只和viewmodel交互
    private TaskViewModel mTaskViewModel;
    private static String LOG_TAG = MainActivity.class.getSimpleName() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //添加展示列表
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final TaskListAdapter adapter = new TaskListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //左右滑动删除任务
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.Callback() {
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                        int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                        return makeMovementFlags(dragFlags, swipeFlags);
                    }


                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int layoutPosition = viewHolder.getLayoutPosition();
                        TaskInfo taskAtPosition = adapter.getTaskAtPosition(layoutPosition);
                        Toast.makeText(MainActivity.this, "Deleting " +
                                taskAtPosition.getUrl(), Toast.LENGTH_LONG).show();
                        Log.d(LOG_TAG,"左右滑动删除!") ;
                        mTaskViewModel.delete(taskAtPosition);

                    }
                    @Override
                    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                        return 0.5f;
                    }
                }
        ) ;
        helper.attachToRecyclerView(recyclerView);

        //新增下载任务
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivityForResult(intent, NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE);
            }
        });

        //将fileviewModel和ui controller绑定，但是activity destory时，viewmodel并不会销毁，重新创建时则会重新返回存在的activity
        mTaskViewModel = ViewModelProviders.of(this).get(TaskViewModel.class);


        //添加对fileList的观察，
        mTaskViewModel.getAllFiles().observe(this, new Observer<List<TaskInfo>>() {

            //当被观察数据更新时，调用这个方法
            @Override
            public void onChanged(@Nullable final List<TaskInfo> taskInfos) {
                // Update the cached copy of the taskInfos in the adapter.
                adapter.setFiles(taskInfos);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.delete_all) {
            // Add a toast just for confirmation
            Toast.makeText(this, "Clearing tasks...",
                    Toast.LENGTH_SHORT).show();

            // Delete the existing data
            mTaskViewModel.deleteALl();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //新增任务activity保存时回调此函数
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            TaskInfo taskInfo = new TaskInfo(data.getStringExtra(AddTaskActivity.EXTRA_REPLY));
            mTaskViewModel.insert(taskInfo);
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    R.string.empty_not_saved,
                    Toast.LENGTH_LONG).show();
        }
    }


}
