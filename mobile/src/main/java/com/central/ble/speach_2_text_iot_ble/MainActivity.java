package com.central.ble.speach_2_text_iot_ble;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.*;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "cmd listener";
    private static final int REQ_SPEECH_RESULT = 1;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button)findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMotorControlVoiceCommand();
            }

            private void startMotorControlVoiceCommand() {
                Log.d(TAG, "starting voice listener intent");
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me, I am Listening!");

                try{
                    startActivityForResult(i, REQ_SPEECH_RESULT);
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Speach to text not supported !!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQ_SPEECH_RESULT) {
            ArrayList results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String command = (String) results.get(0);
            Log.d(TAG, "current command [" + command + "]");
            //This command we will snd to st nucleo board over ble.
        }
    }


}
