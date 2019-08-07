package nju.software.downloader.page.taskList;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import nju.software.downloader.R;
import nju.software.downloader.model.TaskInfo;
import nju.software.downloader.page.addTask.AddTaskActivity;
import nju.software.downloader.page.config.ConfigActivity;
import nju.software.downloader.util.Constant;

import static nju.software.downloader.util.Constant.CONFIGUATION_INTENT_CODE;
import static nju.software.downloader.util.Constant.NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE;
import static nju.software.downloader.util.Constant.PERMISSIONS_REQUEST_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    //activity只和viewmodel交互
    private TaskViewModel mTaskViewModel;
    private static String LOG_TAG = MainActivity.class.getSimpleName() ;

    private SharedPreferences mPreferences ;
    private String sharedPrefFile = "nju.software.android.downloader" ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化最大连接数
        mPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE) ;
        Constant.MAX_TASKS = mPreferences.getInt(Constant.MAX_TASKS_KEY,Constant.MAX_TASKS);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //将fileviewModel和ui controller绑定，但是activity destory时，viewmodel并不会销毁，重新创建时则会重新返回存在的activity
        mTaskViewModel = ViewModelProviders.of(this).get(TaskViewModel.class);

        //初始化下载任务列表
        initListView();

        //初始化Fab-新增
        initFabAdd();

        //检查并申请权限
        checkAndRequestPermissions() ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //菜单栏操作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //删除全部
        if (id == R.id.deleteSelected) {
            // Add a toast just for confirmation
            Toast.makeText(this, "Clearing tasks...",
                    Toast.LENGTH_SHORT).show();
            // Delete the existing data
            mTaskViewModel.multiDelete();
            return true;
        }else if(id==R.id.config){
            //配置页面
            Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
            startActivityForResult(intent, CONFIGUATION_INTENT_CODE);
        }

        return super.onOptionsItemSelected(item);
    }


    private void initListView(){
        //添加展示列表
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final TaskListAdapter adapter = new TaskListAdapter(this,mTaskViewModel);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //添加对fileList的观察，
        mTaskViewModel.getAllFiles().observe(this, new Observer<List<TaskInfo>>() {

            //当被观察数据更新时，调用这个方法
            @Override
            public void onChanged(@Nullable final List<TaskInfo> taskInfos) {
                // Update the cached copy of the taskInfos in the adapter.
                adapter.setFiles(taskInfos);
            }
        });
        //左右滑动删除任务和长按移动任务（实现任务插队）
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.Callback() {
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                        int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                        return makeMovementFlags(dragFlags, swipeFlags);
                    }


                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int oldPosition = viewHolder.getLayoutPosition() ;
                        int targetPosition = target.getLayoutPosition() ;
                        mTaskViewModel.move(oldPosition,targetPosition);
                        return true;
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

                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE || actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                            float alpha = 1 - (Math.abs(dX) / recyclerView.getWidth());
                            viewHolder.itemView.setAlpha(alpha);
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                }
        ) ;
        helper.attachToRecyclerView(recyclerView);

        //增加对item的单机和双击监听
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        /**
                         * 暂停或继续
                         */
                        mTaskViewModel.pasueOrBegin(position);
                    }

                    @Override
                    public void onItemDoubleClicked(RecyclerView recyclerView, int position, View v) {
                        mTaskViewModel.selectTask(position) ;
                    }
                });
    }

    private void initFabAdd(){
        //新增下载任务
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivityForResult(intent, NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE);
            }
        });
    }
    //新增任务的逻辑处理
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_DOWNLADER_TASK_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            TaskInfo taskInfo = new TaskInfo(data.getStringExtra(AddTaskActivity.EXTRA_REPLY));
            mTaskViewModel.insert(taskInfo);
        } else if(requestCode == CONFIGUATION_INTENT_CODE && resultCode == RESULT_OK) {
            int max_connection_number = data.getIntExtra(ConfigActivity.EXTRA_REPLY,0);
            if(max_connection_number!=0&&max_connection_number!=Constant.MAX_TASKS){
                mTaskViewModel.changeMaxConnectionNumber(max_connection_number) ;
            }
        }
    }


    public void checkAndRequestPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //没有权限，则申请

            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                //如果之前拒绝过，则解释为何申请权限
                Toast.makeText(this,"下载保存文件需要访问您手机存储",Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_EXTERNAL_STORAGE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //获取到权限
                }else {
                    Toast.makeText(this,"未授权，下载文件无法保存",Toast.LENGTH_SHORT).show();
//                    finish();
                }
        }
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//
//    }
    //保存最大链接数
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putInt(Constant.MAX_TASKS_KEY,Constant.MAX_TASKS) ;
        edit.apply();
    }
}
