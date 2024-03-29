package nju.software.downloader.page.addTask;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

import nju.software.downloader.R;

/**
 * 新增页面
 */
public class AddTaskActivity extends AppCompatActivity {
    public static final String EXTRA_REPLY =
            "nju.software.fileAdd.REPLY";
    private static String LOG_TAG = AddTaskActivity.class.getSimpleName() ;
    private EditText mEditWordView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_word);
        mEditWordView = findViewById(R.id.edit_word);

        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent replyIntent = new Intent();
                Log.d(LOG_TAG,"获取下载url") ;

                if (TextUtils.isEmpty(mEditWordView.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                    Toast.makeText(
                            getApplicationContext(),
                            R.string.empty_not_saved,
                            Toast.LENGTH_LONG).show();

                } else {
                    //检验url有效性
                    String url = mEditWordView.getText().toString() ;
                    try{
                        new URL(url) ;
                    } catch (MalformedURLException e) {
                        Toast.makeText(getApplicationContext(),
                                "URL is invalid",Toast.LENGTH_SHORT).show();
                    }

                    String word = mEditWordView.getText().toString();
                    replyIntent.putExtra(EXTRA_REPLY, word);
                    setResult(RESULT_OK, replyIntent);
                    Log.d(LOG_TAG,word) ;
                    //这个activity结束，出栈
                    finish();
                }
            }
        });
    }
}
