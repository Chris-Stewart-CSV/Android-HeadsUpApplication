package com.pd.trackeye;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.media.MediaPlayer;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import java.io.IOException;

/* TODO:
    - Implement timer to stop fast reacting alarm [x]
    - Fix pauseAlarm method. Currently causes all audio to stop [ ]
    - Generic Performance improvements [ ]
    - Improve formatting, readability, etc. [ ]
*/

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    EditText textView;                  // Shows eye tracking status / message to user
    MediaPlayer mp;                     // Declare media player (alarm)
    MediaPlayer mpT;                    // Declare media player (pingone)
    CameraSource cameraSource;          // Declare cameraSource
    boolean startWasPressed = false;    // Used to check if "start" is pressed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                     // Display main view
        mp = MediaPlayer.create(this,R.raw.alarm);          // Create media player
        mpT = MediaPlayer.create(this,R.raw.pingone);       // Create media player
        final Button startButton = findViewById(R.id.startButton); // Refers to start button
        final Button closeButton = findViewById(R.id.closeButton); // Refers to close button

        // Listen for Start button to be pressed
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWasPressed = true;                     // Trigger startWasPressed
                playPing();                                 // Play ping sound
                textView.setVisibility(View.VISIBLE);       // Show test once Started
                startButton.setVisibility(View.INVISIBLE);  // Hide Start button
                closeButton.setVisibility(View.VISIBLE);    // Show Close button
            }
        });

        // Listen for close button to be pressed
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { closeApplication(); }
        });

        // Request permission to use device camera and handle otherwise
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            Toast.makeText(this, "Grant Permission and restart app", Toast.LENGTH_SHORT).show();
        }
        else {
            textView = findViewById(R.id.textView);
            createCameraSource();
        }

    }//end onCreate

    // Used to play ping on button press
    public void playPing() {
        mpT.start();
    } //end playPing

    // In progress - Causes no sound to play at all
    // Used to pause alarm when driver pays attention again
    public void playAlarm() {
        mp.start();
    } //end playAlarm

    /** In progress - Causes no sound to play at all **/
    public void pauseAlarm() {
        mp.stop();
    } //end pauseAlarm

    private class EyesTracker extends Tracker<Face> {

        // Thresholds define the threshold of a face being detected or not
        private final float EYES_THRESHOLD = 0.75f; // original value = 0.75f;
        private final float TURNING_RIGHT_THRESHOLD = -45f;
        private final float TURNING_LEFT_THRESHOLD = 45f;


        private EyesTracker() { /***************/ }//end EyesTracker

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            if(startWasPressed){

                // If eyes are determined to be open then update text
                    if (face.getIsLeftEyeOpenProbability() > EYES_THRESHOLD || face.getIsRightEyeOpenProbability() > EYES_THRESHOLD) {
                        showStatus("Eyes Open.");

                    }else{

                        // Eyes Closed
                        if (face.getIsLeftEyeOpenProbability() < EYES_THRESHOLD || face.getIsRightEyeOpenProbability() < EYES_THRESHOLD){
                            playAlarm();
                            showStatus("Eyes Closed.");
                        }//end if

                        // Head Turned Right
                        if (face.getEulerY() < TURNING_RIGHT_THRESHOLD){
                            playAlarm();
                            showStatus("Turned Right.");
                        }//end if

                        // Head Turned Left
                        if(face.getEulerY() > TURNING_LEFT_THRESHOLD){
                            playAlarm();
                            showStatus("Turned Left.");
                        }//end if

                    }//end else
            }//end if startWasPressed
        }//end onUpdate

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            showStatus("Face Not Detected yet!");
            // Possibly play alarm here - Still to be determined
        }//end onMissing

        @Override
        public void onDone() { super.onDone(); } //end onDone

    }//end EyeTracker class

    private class FaceTrackerFactory implements MultiProcessor.Factory<Face> {
        private FaceTrackerFactory() { /***************/ }
        @Override
        public Tracker<Face> create(Face face) { return new EyesTracker(); }//end create
    }//end class FaceTrackerFactory

    private void closeApplication(){ // Linked to button press - Does exactly what you think it does
        finish();
        moveTaskToBack(true);
    }//end closeApplication

    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE) // Original FAST_MODE -- ACCURATE_MODE enables getEulerY method
                .build();
        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerFactory()).build());

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                //    here to request the missing permissions, and then overriding
                //    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                           int[] grantResults)
                //    to handle the case where the user grants the permission. See the documentation
                //    for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }//end createCameraSource

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSource != null) { // Checks if camera is being used or not - If not used then Heads Up will use camera
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    //    here to request the missing permissions, and then overriding
                    //    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                           int[] grantResults)
                    //    to handle the case where the user grants the permission. See the documentation
                    //    for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraSource.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }//end try
        }//end if cameraSource
    }//end onResume

    // On application pause temporarily give up use of camera
    @Override
    protected void onPause() { // On application pause (If app is minimized)
        super.onPause();
        if (cameraSource!=null) {
            cameraSource.stop();
        }
    }//end onPause

    @Override
    protected void onDestroy() { // Cleans up when app closes
        super.onDestroy();
        mp.stop();
        mp.release();
        if (cameraSource!=null) {
            cameraSource.release();
        }
    }//end onDestroy

    // Show status of facial view
    public void showStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() { textView.setText(message); } // Used to update text notification
        });
    }//end showStatus

}//end class MainActivity