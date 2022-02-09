package it.adalborgo.latorcia;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import static android.hardware.camera2.CameraCharacteristics.*;

public class MainActivity extends AppCompatActivity {

    // Revision control id
    public static final String cvsId = "$Id: LaTorcia.java,v 1.6 02/09/2020 23:59:59 adalborgo $";

    final int IS_ON = Color.WHITE;
    final int IS_OFF = Color.DKGRAY;

    private String on = null;
    private String off = null;

    private Button button;

    private WindowManager.LayoutParams layout;

    private Camera camera;
    private Parameters params;

    String cameraId = null;

    private boolean hasFlash = false;

    // Init: turn on the light
    private boolean setOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Prevent multiple instances of this activity
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        // Disable screensaver
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        layout = getWindow().getAttributes();
        getWindow().getDecorView().setBackgroundColor(IS_OFF);

        // Full screen
        LinearLayout linearlayout = (LinearLayout) findViewById(R.id.mainlayout);

        // Get string on/off
        on  = getString(R.string.on);
        off = getString(R.string.off);

        // Flash switch button
        button = findViewById(R.id.button);

        // Set background listener
        linearlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOn = !setOn;
                lightSwitch(setOn);
            }
        });

        // Set button listener
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOn = !setOn;
                lightSwitch(setOn);
            }
        });

        // Check if device is supporting flashlight
        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    void getCamera() {
        boolean found = false;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    if (cameraManager.getCameraCharacteristics(id).get(FLASH_INFO_AVAILABLE)) {
                        cameraId = id;
                        found = true;
                    }
                }

            } catch (CameraAccessException e) {
                //Toast.makeText(getApplicationContext(), "Torch Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } else {
            if (camera==null) {
                try {
                    camera = Camera.open();
                    params = camera.getParameters();
                    found = true;
                } catch (RuntimeException e) {
                    //Toast.makeText(getApplicationContext(), "Camera Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        hasFlash = found;
    }

    /**
     * Switch the light
     */
    private void lightSwitch(boolean setOn) {
        if (setOn) { // Turn on
            if (hasFlash) switchFlash(true);
            else switchScreenLight(true);
        } else { // Turn off
            if (hasFlash) switchFlash(false);
            else switchScreenLight(false);
        }

        setButtonState(); // Set text of the button
    }

    /**
     * Turn on the flash if camera has one
     * @param setOn
     */
    private void switchFlash(boolean setOn) {

        if (!hasFlash) return; // No flash

        if(Build.VERSION.SDK_INT >= 23) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try { cameraManager.setTorchMode(cameraId, setOn);
            } catch (CameraAccessException e) { }

        } else {
            if (camera==null || params==null) return;
            params = camera.getParameters();
            if (setOn) {
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                camera.startPreview();

            } else {
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                camera.stopPreview();
            }
        }
    }

    /**
     * Toggle the screen
     * @param setOn
     */
    private void switchScreenLight(boolean setOn) {
        if (setOn) {
            getWindow().getDecorView().setBackgroundColor(IS_ON);
            layout.screenBrightness = 1F;
        } else {
            getWindow().getDecorView().setBackgroundColor(IS_OFF);
            layout.screenBrightness = -1F;
        }

        getWindow().setAttributes(layout);
    }

    // Set button text to the next states (on/off)
    private void setButtonState() {
        if(setOn) {
            button.setText(off);
            button.setBackgroundResource(R.drawable.button_off);
        } else {
            button.setText(on);
            button.setBackgroundResource(R.drawable.button_on);
        }
    }

    void turnOff() {
        setOn = true;
        button.setText(on);
        switchScreenLight(false);
        if (hasFlash) {
            switchFlash(false);
            // Release the camera
            if (camera!=null) {
                camera.release();
                camera = null;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get the camera
        if (hasFlash) getCamera();

        // Set switch
        lightSwitch(setOn);
    }

    @Override
    protected void onPause() {
        super.onPause();
        turnOff();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        turnOff();
    }

    /**
     * When the back_key is pressed, clear all and move task to back
     */
    @Override
    public void onBackPressed() {
        // Enable screensaver
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        turnOff();

        finish();
        moveTaskToBack(true);
    }
}
