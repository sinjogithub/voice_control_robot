package com.central.ble.speach_2_text_iot_ble;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/**
 * Created by binvargh on 10/16/2017.
 */

public class TtsSpeaker implements TextToSpeech.OnInitListener {
    interface Listener {
        void onTtsInitialized();

        void onTtsSpoken();
    }

    private static final String TAG = TtsSpeaker.class.getSimpleName();
    private static final String UTTERANCE_ID = BuildConfig.APPLICATION_ID + ".UTTERANCE_ID";

    private Listener listener;
    private TextToSpeech ttsEngine;

    private boolean isInitialized = false;

    public TtsSpeaker(Context context, Listener listener) {
        this.listener = listener;
        ttsEngine = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            ttsEngine.setLanguage(Locale.US);
            ttsEngine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    Log.i(TAG, "onStart");
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.i(TAG, "onDone");
                    listener.onTtsSpoken();
                }

                @Override
                public void onError(String s, int e) {
                    Log.w(TAG, "onError (" + s + ")" + ". Error code: " + e);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.w(TAG, "onError");
                }
            });

            ttsEngine.setPitch(1f);
            ttsEngine.setSpeechRate(1f);

            isInitialized = true;
            Log.i(TAG, "TTS initialized successfully");

            listener.onTtsInitialized();
        }else {
            Log.w(TAG, "Tts engine initialization failed");
            ttsEngine = null;
        }
    }

    public void say(String message) {
        if(!isInitialized || ttsEngine == null){
            Log.w(TAG, "Tts engine not initialized yet");
            return;
        }
        ttsEngine.speak(message, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
    }

    public void onDestroy() {
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
    }
}
