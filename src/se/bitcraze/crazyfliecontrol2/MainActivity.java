/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyfliecontrol2;

import java.io.File;
import java.util.List;
import java.util.Locale;

import se.bitcraze.crazyflie.lib.crazyradio.Crazyradio;
import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.GamepadController;
import se.bitcraze.crazyfliecontrol.controller.GyroscopeController;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.controller.TouchController;
import se.bitcraze.crazyfliecontrol.prefs.PreferencesActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.JoystickView;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "CrazyflieControl";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 42;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 43;


    private JoystickView mJoystickViewLeft;
    private JoystickView mJoystickViewRight;
    private FlightDataView mFlightDataView;

    private ScrollView mConsoleScrollView;
    private TextView mConsoleTextView;

    private SharedPreferences mPreferences;

    private IController mController;
    private GamepadController mGamepadController;

    private String mRadioChannelDefaultValue;
    private String mRadioDatarateDefaultValue;

    private boolean mDoubleBackToExitPressedOnce = false;
    private Controls mControls;
    private SoundPool mSoundPool;
    private boolean mLoaded;
    private int mSoundConnect;
    private int mSoundDisconnect;

    private ImageButton mToggleConnectButton;
    private ImageButton mRingEffectButton;
    private ImageButton mHeadlightButton;
    private ImageButton mBuzzerSoundButton;
    private File mCacheDir;

    private TextView mTextView_battery;
    private TextView mTextView_linkQuality;
    private MainPresenter mPresenter;
    private boolean mUsbReceiverRegistered = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPresenter = new MainPresenter(this);

        setDefaultPreferenceValues();

        mTextView_battery = (TextView) findViewById(R.id.battery_text);
        mTextView_linkQuality = (TextView) findViewById(R.id.linkQuality_text);

        setBatteryLevel(-1.0f);
        setLinkQualityText("N/A");

        mControls = new Controls(this, mPreferences);
        mControls.setDefaultPreferenceValues(getResources());

        //Default controller
        mJoystickViewLeft = (JoystickView) findViewById(R.id.joystick_left);
        mJoystickViewRight = (JoystickView) findViewById(R.id.joystick_right);
        mJoystickViewRight.setLeft(false);
        mController = new TouchController(mControls, this, mJoystickViewLeft, mJoystickViewRight);

        //initialize gamepad controller
        mGamepadController = new GamepadController(mControls, this, mPreferences);
        mGamepadController.setDefaultPreferenceValues(getResources());

        //initialize buttons
        mToggleConnectButton = (ImageButton) findViewById(R.id.imageButton_connect);
        initializeMenuButtons();

        mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);

        mConsoleScrollView = (ScrollView) findViewById(R.id.console_scrollView);
        mConsoleTextView = (TextView) findViewById(R.id.console_textView);
        registerForContextMenu(mConsoleTextView);

        //action buttons
        mRingEffectButton = (ImageButton) findViewById(R.id.button_ledRing);
        mHeadlightButton = (ImageButton) findViewById(R.id.button_headLight);
        mBuzzerSoundButton = (ImageButton) findViewById(R.id.button_buzzerSound);

        registerUsbReceiver();

        initializeSounds();

        setCacheDir();
    }

    private void registerUsbReceiver() {
        // The USB permission action is private to this app, while attach/detach are
        // system broadcasts. Android 13+ requires an export flag on dynamic receivers,
        // so they are registered separately with the appropriate flag.
        IntentFilter usbPermissionFilter = new IntentFilter(this.getPackageName() + ".USB_PERMISSION");
        IntentFilter usbEventFilter = new IntentFilter();
        usbEventFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbEventFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mUsbReceiver, usbPermissionFilter, RECEIVER_NOT_EXPORTED);
            registerReceiver(mUsbReceiver, usbEventFilter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mUsbReceiver, usbPermissionFilter);
            registerReceiver(mUsbReceiver, usbEventFilter);
        }
        mUsbReceiverRegistered = true;
        Log.d(LOG_TAG, "USB receiver registered (SDK " + Build.VERSION.SDK_INT + ")");
    }

    private void initializeSounds() {
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Load sounds
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mLoaded = true;
            }
        });
        mSoundConnect = mSoundPool.load(this, R.raw.proxima, 1);
        mSoundDisconnect = mSoundPool.load(this, R.raw.tejat, 1);
    }

    private void setCacheDir() {
        if (isExternalStorageWriteable()) {
            Log.d(LOG_TAG, "External storage is writeable.");
            if (mCacheDir == null) {
                File appDir = getApplicationContext().getExternalFilesDir(null);
                mCacheDir = new File(appDir, "TOC_cache");
                mCacheDir.mkdirs();
            }
        } else {
            Log.d(LOG_TAG, "External storage is not writeable.");
            mCacheDir = null;
        }
    }

    private boolean isExternalStorageWriteable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void setDefaultPreferenceValues(){
        // Set default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Initialize preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mRadioChannelDefaultValue = getString(R.string.preferences_radio_channel_defaultValue);
        mRadioDatarateDefaultValue = getString(R.string.preferences_radio_datarate_defaultValue);
    }

    private void checkScreenLock() {
        boolean isScreenLock = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SCREEN_ROTATION_LOCK_BOOL, false);
        if(isScreenLock){
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else{
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void checkConsole() {
        boolean showConsole = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_SHOW_CONSOLE_BOOL, false);
        if (showConsole) {
            mConsoleScrollView.setVisibility(View.VISIBLE);
        } else {
            mConsoleScrollView.setVisibility(View.INVISIBLE);
        }
    }

    private void initializeMenuButtons() {
        mToggleConnectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    if (mPresenter != null && mPresenter.getCrazyflie() != null && mPresenter.getCrazyflie().isConnected()) {
                        mPresenter.disconnect();
                    } else {
                        // TODO: FIXME
                        if(isCrazyradioAvailable(MainActivity.this)) {
                            connectCrazyradio();
                        } else {
                            connectBlePreChecks();
                        }
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton settingsButton = (ImageButton) findViewById(R.id.imageButton_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
              Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
              startActivity(intent);
            }
        });
    }

    private void connectCrazyradio() {
        int radioChannel = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_CHANNEL, mRadioChannelDefaultValue));
        int radioDatarate = Integer.parseInt(mPreferences.getString(PreferencesActivity.KEY_PREF_RADIO_DATARATE, mRadioDatarateDefaultValue));
        mPresenter.connectCrazyradio(radioChannel, radioDatarate, mCacheDir);
    }

    private void connectBlePreChecks() {
        // Check if Bluetooth LE is supported by the Android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.e(LOG_TAG, Build.VERSION.SDK_INT + "does not support Bluetooth LE.");
            Toast.makeText(this, Build.VERSION.SDK_INT + "does not support Bluetooth LE. Please use a Crazyradio to connect to the Crazyflie instead.", Toast.LENGTH_LONG).show();
            return;
        }
        // Check if Bluetooth LE is supported by the hardware
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(LOG_TAG, "Device does not support Bluetooth LE.");
            Toast.makeText(this,  "Device does not support Bluetooth LE. Please use a Crazyradio to connect to the Crazyflie instead.", Toast.LENGTH_LONG).show();
            return;
        }
        // Since API 31, BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions are required, location is not anymore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(LOG_TAG, "Android version >= 31 requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions for Bluetooth scanning");
            requestBluetoothPermissions();
        }
        // Android 10 and 11 require fine location for BLE scan results. Android 8 and 9 accept coarse location.
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String locationPermission = getLocationPermission();
            Log.d(LOG_TAG, "Android version <= 30 requires " + locationPermission + " for Bluetooth scanning.");
            requestRuntimePermission(locationPermission, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            connectBle();
        }
    }

    private void connectBle() {
        boolean writeWithResponse = mPreferences.getBoolean(PreferencesActivity.KEY_PREF_BLATENCY_BOOL, false);
        Log.d(LOG_TAG, "Using bluetooth write with response - " + writeWithResponse);
        mPresenter.connectBle(writeWithResponse, mCacheDir);
    }

    private void checkLocationSettings() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (service == null) {
            Log.e(LOG_TAG, "Location service is unavailable.");
            Toast.makeText(this, "Location service is unavailable.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean isEnabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isEnabled = service.isLocationEnabled();
        } else {
            isEnabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || service.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        if (!isEnabled) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Access");
            builder.setMessage("The app needs location access for Bluetooth scanning. Please enable it in the settings menu.");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.d(LOG_TAG, "Location access request has been denied.");
                }
            });
            builder.show();
        } else {
            connectBle();
        }

    }

    private static String getLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Manifest.permission.ACCESS_FINE_LOCATION;
        }
        return Manifest.permission.ACCESS_COARSE_LOCATION;
    }

    private void requestRuntimePermission(String permission, int request) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                Toast.makeText(this, "Location permission is required for Bluetooth scanning on this Android version.", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, request);
        } else {
            checkLocationSettings();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length == 0 || permissions.length == 0) {
                    Log.d(LOG_TAG, "Location permission request was cancelled.");
                    Toast.makeText(this, "Location permission request was cancelled.", Toast.LENGTH_LONG).show();
                } else if (allGranted(grantResults)) {
                    checkLocationSettings();
                } else {
                    Log.d(LOG_TAG, "Location permission request has been denied.");
                    showPermissionHelp(
                            "Location permission",
                            "Location permission is required for Bluetooth scanning on this Android version.",
                            permissions,
                            grantResults);
                }
                break;
            case MY_PERMISSIONS_REQUEST_BLUETOOTH:
                if (grantResults.length == 0 || permissions.length == 0) {
                    Log.d(LOG_TAG, "Bluetooth permission request was cancelled.");
                    Toast.makeText(this, "Bluetooth permission request was cancelled.", Toast.LENGTH_LONG).show();
                } else if (allGranted(grantResults)) {
                    // Location services are not required for BLE scanning since Android 12
                    connectBle();
                } else {
                    Log.d(LOG_TAG, "BLUETOOTH_SCAN/BLUETOOTH_CONNECT permission request has been denied.");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        showPermissionHelp(
                                "Bluetooth permissions",
                                "Bluetooth permissions are required to find and connect to the Crazyflie.",
                                permissions,
                                grantResults);
                    }
                }
                break;
            default:
                break;
        }
    }

    private static boolean allGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestBluetoothPermissions() {
        String[] permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (allPermissionsGranted) {
            connectBle();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }
    }

    private void showPermissionHelp(String title, String message, String[] permissions, int[] grantResults) {
        boolean permanentlyDenied = false;
        int resultCount = Math.min(permissions.length, grantResults.length);
        for (int i = 0; i < resultCount; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED
                    && !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissions[i])) {
                permanentlyDenied = true;
                break;
            }
        }
        if (permanentlyDenied) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setMessage(message + " Please allow it in the app settings.");
            builder.setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO: improve
        PreferencesActivity.setDefaultJoystickSize(this);
        mJoystickViewLeft.setPreferences(mPreferences);
        mJoystickViewRight.setPreferences(mPreferences);
        mControls.setControlConfig();
        mGamepadController.setControlConfig();
        resetInputMethod();
        checkScreenLock();
        checkConsole();
        //disable action buttons
        mRingEffectButton.setEnabled(false);
        mHeadlightButton.setEnabled(false);
        mBuzzerSoundButton.setEnabled(false);
        if (mPreferences.getBoolean(PreferencesActivity.KEY_PREF_IMMERSIVE_MODE_BOOL, false)) {
            setHideyBar();
        }
        //mJoystickViewLeft.requestLayout();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");
        if (mControls != null) {
            mControls.resetAxisValues();
        }
        if (mController != null) {
            mController.disable();
        }
        updateFlightData();
        mPresenter.disconnect();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        if (mUsbReceiverRegistered) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiverRegistered = false;
        }
        mSoundPool.release();
        mSoundPool = null;
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(mPreferences.getBoolean(PreferencesActivity.KEY_PREF_IMMERSIVE_MODE_BOOL, false) && hasFocus){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setHideyBar();
                }
            }, 2000);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.console_textView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.add(Menu.NONE, 0, 0, "Copy to clipboard");
            menu.add(Menu.NONE, 1, 1, "Clear console");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                ClipboardManager cm = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("console text", mConsoleTextView.getText());
                cm.setPrimaryClip(clipData);
                showToastie("Copied to clipboard");
                break;
            case 1:
                mConsoleTextView.setText("");
                showToastie("Console cleared");
                break;
            default:
                break;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setHideyBar() {
        Log.i(LOG_TAG, "Activating immersive mode");
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        if(Build.VERSION.SDK_INT >= 14){
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if(Build.VERSION.SDK_INT >= 16){
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if(Build.VERSION.SDK_INT >= 18){
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    //TODO: fix indirection
    public void updateFlightData(){
        mFlightDataView.updateFlightData(mController.getPitch(), mController.getRoll(), mController.getThrust(), mController.getYaw());
    }

    public void appendToConsole(String text) {
        final String ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConsoleTextView.append("\n" + ftext);
                mConsoleScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mConsoleScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && event.getAction() == MotionEvent.ACTION_MOVE && mController instanceof GamepadController) {
            mGamepadController.dealWithMotionEvent(event);
            updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // do not call super if key event comes from a gamepad, otherwise the buttons can quit the app
        if (isJoystickButton(event.getKeyCode()) && mController instanceof GamepadController) {
            mGamepadController.dealWithKeyEvent(event);
            // exception for OUYA controllers
            if (!Build.MODEL.toUpperCase(Locale.getDefault()).contains("OUYA")) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // this workaround is necessary because DPad buttons are not considered to be "Gamepad buttons"
    private static boolean isJoystickButton(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            default:
                return KeyEvent.isGamepadButton(keyCode);
        }
    }

    private void resetInputMethod() {
        mController.disable();
        updateFlightData();
        switch (mControls.getControllerType()) {
            case 0:
                // Use GyroscopeController if activated in the preferences
                if (mControls.isUseGyro()) {
                    mController = new GyroscopeController(mControls, this, mJoystickViewLeft, mJoystickViewRight);
                } else {
                    // TODO: reuse existing touch controller?
                    mController = new TouchController(mControls, this, mJoystickViewLeft, mJoystickViewRight);
                }
                break;
            case 1:
                    // TODO: show warning if no game pad is found?
                    mController = mGamepadController;
                break;
            default:
                break;

        }
        mController.enable();
        Toast.makeText(this, "Using " + mController.getControllerName(), Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "mUsbReceiver action: " + action);
            if ((MainActivity.this.getPackageName()+".USB_PERMISSION").equals(action)) {
                //reached only when USB permission on physical connect was canceled and "Connect" or "Radio Scan" is clicked
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(MainActivity.this, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(LOG_TAG, "permission denied for device " + device);
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && UsbLinkAndroid.isUsbDevice(device, Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID)) {
                    Log.d(LOG_TAG, "Crazyradio detached");
                    Toast.makeText(MainActivity.this, "Crazyradio detached", Toast.LENGTH_SHORT).show();
                    playSound(mSoundDisconnect);
                    if (mPresenter != null && mPresenter.getCrazyflie() != null) {
                        Log.d(LOG_TAG, "linkDisconnect()");
                        mPresenter.disconnect();
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && UsbLinkAndroid.isUsbDevice(device, Crazyradio.CRADIO_VID, Crazyradio.CRADIO_PID)) {
                    Log.d(LOG_TAG, "Crazyradio attached");
                    Toast.makeText(MainActivity.this, "Crazyradio attached", Toast.LENGTH_SHORT).show();
                    playSound(mSoundConnect);
                }
            }
        }
    };

    private void playSound(int sound){
        if (mLoaded) {
            float volume = 1.0f;
            mSoundPool.play(sound, volume, volume, 1, 0, 1f);
        }
    }

    // extra method for onClick attribute in XML
    public void switchLedRingEffect(View view) {
        if (mPresenter != null) {
            mPresenter.runAltAction("ring.effect");
        }
    }

    // extra method for onClick attribute in XML
    public void toggleHeadlight(View view) {
        if (mPresenter != null) {
            mPresenter.runAltAction("ring.headlightEnable");
        }
    }

    // extra method for onClick attribute in XML
    public void playBuzzerSound(View view) {
        if (mPresenter != null) {
            mPresenter.runAltAction("sound.effect:10");
        }
    }

    public MainPresenter getPresenter() {
        return mPresenter;
    }

    public IController getController(){
        return mController;
    }

    public Controls getControls(){
        return mControls;
    }

    public static boolean isCrazyradioAvailable(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new IllegalArgumentException("UsbManager == null!");
        }
        List<UsbDevice> usbDeviceList = UsbLinkAndroid.findUsbDevices(usbManager, (short) Crazyradio.CRADIO_VID, (short) Crazyradio.CRADIO_PID);
        return !usbDeviceList.isEmpty();
    }

    public void setBatteryLevel(float battery) {
        float normalizedBattery = battery - 3.0f;
        int batteryPercentage = (int) (normalizedBattery * 100);
        if (battery == -1f) {
            batteryPercentage = 0;
        } else if (normalizedBattery < 0f && normalizedBattery > -1f) {
            batteryPercentage = 0;
        } else if (normalizedBattery > 1f) {
            batteryPercentage = 100;
        }
        //TODO: FIXME
        final int fBatteryPercentage = batteryPercentage;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView_battery.setText(format(R.string.battery_text, fBatteryPercentage));
            }
        });
    }

    public void setLinkQualityText(final String quality){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView_linkQuality.setText(format(R.string.linkQuality_text, quality));
            }
        });
    }

    private String format(int identifier, Object o){
        return String.format(getResources().getString(identifier), o);
    }

    public void showToastie(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setConnectionButtonConnected() {
        setConnectionButtonBackground(R.drawable.custom_button_connected);
    }

    public void setConnectionButtonConnectedBle() {
        setConnectionButtonBackground(R.drawable.custom_button_connected_ble);
    }

    public void setConnectionButtonDisconnected() {
        setConnectionButtonBackground(R.drawable.custom_button);
    }

    public void setConnectionButtonBackground(final int drawable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToggleConnectButton.setBackgroundDrawable(getResources().getDrawable(drawable));
            }
        });
    }

    public void setBuzzerSoundButtonEnablement(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBuzzerSoundButton.setEnabled(enabled);
            }
        });
    }

    public void setRingEffectButtonEnablement(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRingEffectButton.setEnabled(enabled);
            }
        });
    }

    public void setHeadlightButtonEnablement(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHeadlightButton.setEnabled(enabled);
            }
        });
    }

    public void toggleHeadlightButtonColor(final boolean toggle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHeadlightButton.setColorFilter(toggle ? Color.parseColor("#00FF00") : Color.BLACK);
            }
        });
    }

    public void disableButtonsAndResetBatteryLevel() {
        setRingEffectButtonEnablement(false);
        setHeadlightButtonEnablement(false);
        setBuzzerSoundButtonEnablement(false);
        setBatteryLevel(-1.0f);
    }
}
