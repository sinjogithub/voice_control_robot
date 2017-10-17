package com.central.ble.speach_2_text_iot_ble;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by binvargh on 10/15/2017.
 */

public class PocketSphinx implements RecognitionListener {

    public interface Listener {
        void onSpeechRecognizerReady();

        void onActivationPhraseDetected();

        void onTextRecognized(String recognizedText);

        void onTimeout();
    }

    private static final String TAG = PocketSphinx.class.getSimpleName();
    private static final String ACTION_SEARCH = "action";
    private final Listener listener;
    private SpeechRecognizer recognizer;


    public PocketSphinx(Context context, Listener listener) {
        this.listener = listener;
        runRecognizerSetup(context);
    }

    private void runRecognizerSetup(final Context context) {
        Log.d(TAG, "Recognizer setup");

        new AsyncTask<Void, Void, Exception> (){
            @Override
            protected Exception doInBackground(Void... params){
                try{
                    Assets assets = new Assets ( context );
                    File assetDir = assets.syncAssets ();
                    setupRecognizer(assetDir);
                }catch (IOException e){
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result){
                if(result != null){
                    Log.e(TAG, "Failed to initialize recognizer " + result);
                }else{
                    listener.onSpeechRecognizerReady ();
                }

            }
        }.execute (  );

    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup ( )
                .setAcousticModel ( new File ( assetsDir, "en-us" ) )
                .setDictionary ( new File ( assetsDir, "cmudict-en-us.dict" ) )
                .getRecognizer ( );
        recognizer.addListener ( this );
        recognizer.addGrammarSearch ( ACTION_SEARCH, new File(assetsDir, "en-us.lm.bin") );
    }

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
