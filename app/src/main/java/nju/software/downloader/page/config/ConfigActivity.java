package nju.software.downloader.page.config;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import nju.software.downloader.R;
import nju.software.downloader.util.Constant;

public class ConfigActivity extends AppCompatActivity {
    private EditText maxConnectionNumbersEt ;
    private Button updateBt ;

    public static final String EXTRA_REPLY =
            "nju.software.config.REPLY";

    private static String LOG_TAG = ConfigActivity.class.getSimpleName() ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        maxConnectionNumbersEt = findViewById(R.id.max_connections_number) ;
        maxConnectionNumbersEt.setText(Constant.MAX_TASKS+"");
        updateBt = findViewById(R.id.config_update_button) ;

        updateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent replyIntent = new Intent();
                String input = maxConnectionNumbersEt.getText().toString();
                if (TextUtils.isEmpty(input)) {
                    setResult(RESULT_CANCELED, replyIntent);
                    Toast.makeText(
                            getApplicationContext(),
                            R.string.empty_not_saved,
                            Toast.LENGTH_LONG).show();
                } else {
                    int number = Integer.parseInt(input) ;
                    if(number<1){
                        Toast.makeText(
                                getApplicationContext(),
                                R.string.need_number,
                                Toast.LENGTH_LONG).show();
                    }else{
                        replyIntent.putExtra(EXTRA_REPLY, number);
                        setResult(RESULT_OK, replyIntent);
                        Log.d(LOG_TAG,number+"") ;
                        //这个activity结束，出栈
                        finish();
                    }

                }
            }
        });
    }
}
