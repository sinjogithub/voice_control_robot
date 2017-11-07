package com.central.ble.speach_2_text_iot_ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
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
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TtsSpeaker.Listener, PocketSphinx.Listener{

    private static final long SCAN_PERIOD = 10000;

    private enum Control_State {
        INITIALIZING,
        LISTENING_TO_KEYPHRASE,
        START_CONTROL_MOTOR,
        LISTEN_CONTROL_MOTOR_ACTION,
        CONFIRM_CONTROL_MOTOR_ACTION,
        CONTROL_MOTOR_LISTEN_TIMEOUT
    }
    private static final String TAG = MainActivity.class.getSimpleName();
    private ButtonInputDriver buttonInputDriver;
    private Control_State control_state;

    private TtsSpeaker ttsSpeaker;
    private PocketSphinx pocketSphinx;
    private boolean isSphinxInitialized = false;


    private Ble_Client_Service ble_client_service;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mBLEConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;
    private Handler mHandler;

    // Code to manage Service lifecycle.
    private final ServiceConnection mBLEServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ble_client_service = ((Ble_Client_Service.LocalBinder) service).getService();
            if (!ble_client_service.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //ble_client_service.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            ble_client_service = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ble_client_service.ACTION_GATT_CONNECTED.equals(action)) {
                mBLEConnected = true;
                Log.d(TAG, "ble connected");
                ttsSpeaker.say("connected to blue tooth low energy server");
               // updateConnectionState(R.string.connected);
               // invalidateOptionsMenu();
            } else if (ble_client_service.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBLEConnected = false;
                Log.d(TAG, "ble disconnected");
                ttsSpeaker.say("disconnected from blue tooth low energy server");
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
               // clearUI();
            } else if (ble_client_service.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
               // displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ble_client_service.ACTION_DATA_AVAILABLE.equals(action)) {
               // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "starting voice listener activity");

        mHandler = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Log.d(TAG, "Bluetoooth LE not supported");
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null){
            Log.d(TAG, "Error in getting Bluetooth adapter");
            finish();
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();


        PeripheralManagerService pioService = new PeripheralManagerService();

        try{
            Log.i(TAG, "Configuring button driver");

            buttonInputDriver = new ButtonInputDriver(BoardDefaults.getGPIOForButton(), Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE);
            buttonInputDriver.register();
        }catch (IOException e){
            Log.i(TAG, "Error configuring button", e);
        }

        Intent gattServiceIntent = new Intent(this, Ble_Client_Service.class);
        bindService(gattServiceIntent, mBLEServiceConnection, BIND_AUTO_CREATE);

        ttsSpeaker = new TtsSpeaker(this, this);

        scanLeDevice(true);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button pressed");
            // Automatically connects to the device upon successful start-up initialization.
            if(!mBLEConnected){
                scanLeDevice(true);
            }


            if(isSphinxInitialized){
                ttsSpeaker.say("Your turn");
                control_state = Control_State.LISTEN_CONTROL_MOTOR_ACTION;
                pocketSphinx.startListeningToAction();
            }else{
                ttsSpeaker.say("speach recogniser not ready");
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void scanLeDevice(final boolean enable) {
        if(enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Stopping the scan");
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(Ble_Motor_GattAttributes.MOTOR_CONTROL_SERVICE))
                            .build();

            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            scanFilters.add(scanFilter);

            ScanSettings scanSettings = new ScanSettings.Builder().build();

            mBluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);

            mScanning = true;



        }else{
            Log.d(TAG, "Stopping the scan : 2");
            mScanning = false;
            mBluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            mDeviceAddress = bluetoothDevice.getAddress();
            ble_client_service.connect(mDeviceAddress);
        }

        @Override
        public void onScanFailed(int errorCode){
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed : " + errorCode);
        }
    };

    @Override
    public void onSpeechRecognizerReady() {
        control_state = Control_State.INITIALIZING;
        isSphinxInitialized = true;
        ttsSpeaker.say("Start controlling robot");
    }

    @Override
    public void onActivationPhraseDetected() {
        control_state = Control_State.START_CONTROL_MOTOR;
        Log.i(TAG, "Activation phrase detected");
        if(!mBLEConnected){
            scanLeDevice(true);
        }
        ttsSpeaker.say("Yup");
    }

    @Override
    public void onTtsInitialized() {
        pocketSphinx = new PocketSphinx(this, this);
    }

    @Override
    public void onTtsSpoken() {
        Log.i(TAG, "on tts spoken");
        switch (control_state){
            case INITIALIZING:
            case CONFIRM_CONTROL_MOTOR_ACTION:
            case CONTROL_MOTOR_LISTEN_TIMEOUT:
                Log.d(TAG, "Listening for key phrase");
                control_state = Control_State.LISTENING_TO_KEYPHRASE;
                pocketSphinx.clearRecognizer();
                pocketSphinx.startListeningToActivationPhrase();
                break;

            case START_CONTROL_MOTOR:
                Log.d(TAG, "Listening for action");
                control_state = Control_State.LISTEN_CONTROL_MOTOR_ACTION;
                pocketSphinx.clearRecognizer();
                pocketSphinx.startListeningToAction();
                break;

            default:
                Log.d(TAG, "wrong state : " + control_state);
                break;
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
    public void onTextRecognized(String recognizedText) {
        control_state = Control_State.CONFIRM_CONTROL_MOTOR_ACTION;
        String input = recognizedText == null ? "" : recognizedText;
        String answer;
        Log.d(TAG, "command is " + input);
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
        control_state = Control_State.CONTROL_MOTOR_LISTEN_TIMEOUT;
        ttsSpeaker.say("Timeout please press button again or say control motor");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (ble_client_service != null) {
            final boolean result = ble_client_service.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Ble_Client_Service.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Ble_Client_Service.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(Ble_Client_Service.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(Ble_Client_Service.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(mGattUpdateReceiver);
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
        unbindService(mBLEServiceConnection);
        ble_client_service = null;

        ttsSpeaker.onDestroy();
        pocketSphinx.onDestroy();
    }

}
