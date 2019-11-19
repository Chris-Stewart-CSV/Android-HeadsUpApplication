package com.pd.trackeye;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    - Generic Performance improvements [ ]
    - Improve formatting, readability, etc. [ ]
*/

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    EditText textView;                  // Shows eye tracking status / message to user
    MediaPlayer mp;                     // Declare media player (alarm: orb.mp3)
    MediaPlayer mpT;                    // Declare media player (button_ping: pingone.wav)
    CameraSource cameraSource;          // Declare cameraSource
    boolean startWasPressed = false;    // Used to check if "start" is pressed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                     // Display main view
        mp = MediaPlayer.create(this,R.raw.orb);            // Create media player
        mpT = MediaPlayer.create(this,R.raw.pingone);       // Create media player
        final Button startButton = findViewById(R.id.startButton);  // Refers to start button
        final Button closeButton = findViewById(R.id.closeButton);  // Refers to close button
        final Button devButton = findViewById(R.id.devButton);  // Refers to start button

        // Listen for Start button to be pressed
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWasPressed = true;                     // Trigger startWasPressed
                playPing();                                 // Play ping sound
                textView.setVisibility(View.INVISIBLE);     // Show text on app start
                startButton.setVisibility(View.INVISIBLE);  // Hide Start button on app start
                closeButton.setVisibility(View.VISIBLE);    // Show Close button on app start
            }
        });

        // Listen for close button to be pressed
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { playPing(); closeApplication(); }
        });

        devButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 textView.setVisibility(View.VISIBLE);
            }//end OnClick
        });//end OnClickListener

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

    // Used to play alarm when driver not paying attention
    public void playAlarm() {
        mp.start();
    } //end playAlarm

    // Currently not used
    // Used to pause alarm when driver pays attention again
    public void pauseAlarm() {
        mp.pause();
    } //end pauseAlarm

    private class EyesTracker extends Tracker<Face> {

        // Thresholds define the threshold of a face being detected 
        private final float EYES_THRESHOLD = 0.75f; // Original value = 0.75f;

        private EyesTracker() { /**/ }//end EyesTracker

        //Update Variable Initialization
        long last_time = System.nanoTime();
        boolean isAttentive = true;
        float inattentiveTime = 0.0f;
        final float INATTENTIVE_THRESHOLD = 2000;   // 2 seconds for timer

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            long time = System.nanoTime();
            long nanosPerSecond = 1000000;  // Number of nanoseconds per second
            float delta_time = (time - last_time) / nanosPerSecond;
            last_time = time;

            if (isAttentive) {
                inattentiveTime = 0;
            }
            else {
                showStatus("Not attentive for: " + inattentiveTime);
                inattentiveTime += delta_time;
            }

            if (inattentiveTime > INATTENTIVE_THRESHOLD) {
                playAlarm();
            }

            if(startWasPressed){

                boolean EyesClosed = EyesClosed(detections,face,EYES_THRESHOLD);
                float TURNING_LEFT_THRESHOLD = 35f;
                boolean HeadTurnedLeft = HeadTurnedLeft(detections,face, TURNING_LEFT_THRESHOLD);
                float TURNING_RIGHT_THRESHOLD = -35f;
                boolean HeadTurnedRight = HeadTurnedRight(detections,face, TURNING_RIGHT_THRESHOLD);

                // If eyes are determined to be OPEN then update text
                if (!EyesClosed || !HeadTurnedLeft || !HeadTurnedRight){
                    isAttentive = true;
                }
                // If eyes are CLOSED then update text and play alarm
                if (EyesClosed){
                    isAttentive = false;
                }
                // If head is turned too far RIGHT then update text and play alarm
                if(HeadTurnedRight){
                    isAttentive = false;
                }
                // If head is turned too far LEFT then update text and play alarm
                if(HeadTurnedLeft){
                    isAttentive = false;
                }
            }//end if startWasPressed
        }//end onUpdate

        // Boolean to detect if Eyes are open/closed based on EYES_THRESHOLD
        boolean EyesClosed(Detector.Detections<Face> detections, Face face, float threshold){
            boolean closed;
            closed = !(face.getIsLeftEyeOpenProbability() > EYES_THRESHOLD) || !(face.getIsRightEyeOpenProbability() > EYES_THRESHOLD);
            return closed;
        }//end EyesClosed

        // Boolean to detect if Head is turned LEFT based on TURNING_LEFT_THRESHOLD
        boolean HeadTurnedLeft(Detector.Detections<Face> detections, Face face, float threshold) {
            boolean turned;
            turned = face.getEulerY() > threshold;
            return turned;
        }//end HeadTurnedLeft

        // Boolean to detect if Head is turned RIGHT based on TURNING_RIGHT_THRESHOLD
        boolean HeadTurnedRight(Detector.Detections<Face> detections, Face face, float threshold) {
            boolean turned;
            turned = face.getEulerY() < threshold;
            return turned;
        }//end HeadTurnedRight

        // Called when a face is not detected within the camera view
        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            showStatus("Face Not Detected yet!");
            if(startWasPressed){
                isAttentive = false;
                playAlarm();
            }
        }//end onMissing

        @Override
        public void onDone() { super.onDone(); } //end onDone

    }//end EyeTracker class

    private class FaceTrackerFactory implements MultiProcessor.Factory<Face> {

        private FaceTrackerFactory() { /**/ }
        @Override
        public Tracker<Face> create(Face face) { return new EyesTracker(); }//end create

    }//end class FaceTrackerFactory

    // Linked to closeButton press - Does exactly what you think it does
    private void closeApplication(){
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
            }//end try/catch
        }//end if cameraSource != null
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
