package com.central.ble.speach_2_text_iot_ble;

import android.util.Log;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

/**
 * Created by binvargh on 10/15/2017.
 */

public class PocketSphinx implements RecognitionListener {
    private static final String TAG = PocketSphinx.class.getSimpleName();
    private static final String ACTION_SEARCH = "action";
    //private final Listener listener;
    private SpeechRecognizer recognizer;
    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

    }

    @Override
    public void onResult(Hypothesis hypothesis) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }

    public void startListeningToAction() {
        Log.i(TAG, "Start listening for some actions with a 10secs timeout");
        recognizer.startListening(ACTION_SEARCH, 10000);
    }

    public void onDestroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }


}
