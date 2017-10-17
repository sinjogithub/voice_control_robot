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

    private TtsSpeaker ttsSpeaker;
    private PocketSphinx pocketSphinx;

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

        ttsSpeaker = new TtsSpeaker(this, this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button pressed");
            ttsSpeaker.say("Your turn");
            pocketSphinx.startListeningToAction();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSpeechRecognizerReady() {
        ttsSpeaker.say("Start controlling robot");
    }

    @Override
    public void onActivationPhraseDetected() {
        Log.i(TAG, "Activation phrase detected");
    }

    @Override
    public void onTtsInitialized() {
        pocketSphinx = new PocketSphinx(this, this);
    }

    @Override
    public void onTtsSpoken() {
        Log.i(TAG, "on tts spoken");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button released");
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onTextRecognized(String recognizedText) {
        String input = recognizedText == null ? "" : recognizedText;
        String answer;
        if(input.contains("left")){
            answer = "Turn left";
        }else if(input.contains("right")){
            answer = "Turn right";
        }else if(input.contains("forward")){
            answer = "move forward";

        }else if(input.contains("reverse")){
            answer = "move backward";
        }else{
            answer = "unknown command";
        }

        ttsSpeaker.say(answer);
    }

    @Override
    public void onTimeout() {
        ttsSpeaker.say("only ten seconds.please press button again");
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

        ttsSpeaker.onDestroy();
        pocketSphinx.onDestroy();
    }

}
