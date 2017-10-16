package com.central.ble.speach_2_text_iot_ble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TtsSpeaker.Listener, PocketSphinx.Listener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private ButtonInputDriver buttonInputDriver;
    private static final int REQ_SPEECH_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "starting voice listener activity");

        PeripheralManagerService pioService = new PeripheralManagerService();

        try{
            Log.i(TAG, "Configuring button driver");

            buttonInputDriver = new ButtonInputDriver(BoardDefaults.getGPIOForButton(), Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE);
            buttonInputDriver.register();
        }catch (IOException e){
            Log.i(TAG, "Error configuring button", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button pressed");
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
           // i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me, I am Listening!");

            try{
                startActivityForResult(i, REQ_SPEECH_RESULT);
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Speach to text not supported !!", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onKeyDown(keyCode, event);
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


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button released");
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(buttonInputDriver != null){
            buttonInputDriver.unregister();
            try{
                buttonInputDriver.close();
            }catch (IOException e){
                Log.i(TAG, "Error closing button driver", e);
            }finally {
                buttonInputDriver = null;
            }
        }
    }

}
