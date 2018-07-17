package com.android.realTimeAudioProcessing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import static android.Manifest.permission.RECORD_AUDIO;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity {

    TextView disp;
    TextView dispbot;
    TextView dispmid;
    String TAG;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecorder = null;
    private boolean isRecording = false;
    private int bufferSize;
    final int framesToSent = 63;
    final int tempframe = framesToSent/3;
    final int maxframesToAnalyse = 600;
    final double threshold = 12;
    String retVal;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    int frame = 0;
    int code = 0;
    boolean Requested = false;
    int NumOfReq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        disp = (TextView) findViewById(R.id.display);
        dispmid = (TextView) findViewById(R.id.displaymid);
        dispbot = (TextView) findViewById(R.id.displaybot);
        ((Button) findViewById(R.id.btStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btStart)).setEnabled(true);

    }
    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick (View v){
            if (checkPermission()){
                if (v.getId() == R.id.btStart) {
                    Toast.makeText(MainActivity.this, "Recording Started ",
                            Toast.LENGTH_LONG).show();
                    recordAudio();
                }

            }
            else {
                requestAudioPermissions();
            }
        }
    };
    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    //Requesting run-time permissions

    //Create placeholder for user's consent to record_audio permission.
    //This will be used in handling callback
    ;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            recordAudio();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public class SendSong extends AsyncTask<String, Void, String> {

        private String songName = "not sent.... check connection";

        protected void onPreExecute(){}

        protected String doInBackground(String... arg0) {

            try {

                //URL url = new URL("http://192.169.30.125:5000");
                URL url = new URL("http://192.169.33.167:8080/identify_debug");

                String ofSong = retVal;
                //String ofSong = getEncodedFile(AudioSavePathInDevice);




                JSONObject postDataParams = new JSONObject();

                postDataParams.put("audiofile", ofSong);
                postDataParams.put("device_id", "song");
                postDataParams.put("bg_track", "blue_eyes");
                Log.d("params",postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(50000 /* milliseconds */);
                conn.setConnectTimeout(50000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty( "charset", "UTF-8");



                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(postDataParams.toString());

                writer.flush();
                writer.close();
                os.close();


                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in=new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line=in.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();

                    JSONObject responseObject = new JSONObject(sb.toString());
                    songName = responseObject.getString("response");
                    code = Integer.parseInt(responseObject.getString("identified"));

                    return sb.toString();

                }
                else {
                    return new String("false : "+responseCode);
                }
            }
            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }

        }

        @Override
        protected void onPostExecute(String result) {

            dispmid.setText(songName+ " code "+ code);
            Requested = false;
        }
    }

    private void ConvertToString(byte audioToSend[]){
        retVal = Base64.encodeToString(audioToSend, Base64.DEFAULT);
        final int size = audioToSend.length;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dispbot.setText("sending on server with length: "+ size);
            }
        });
        new SendSong().execute();

    }
    private  double RMS(byte audioData[]) {
        double sum = 0.0;
        short[] shorts = new short[audioData.length/2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        for (int i = 0; i < shorts.length; i++) {
            sum = sum + ((double) shorts[i] * (double) shorts[i] / 10000.0);
        }
        sum = sum / shorts.length;
        return sqrt(sum);
    }
    private void recordAudio() {
        resetValue();
        Log.i(TAG, "in the recording");
        int desiredRate = 0;
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        if (bufferSize > 0) {
            audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                desiredRate = -1;
                Log.i(TAG, "state not good");
                audioRecorder.release();
            } else {
                audioRecorder.startRecording();
                isRecording = true;
                frame = 0;

                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        byte audioData[] = new byte[bufferSize];
                        byte audioToSend[] = new byte[bufferSize*framesToSent];
                        Queue<Byte> myQueue = new LinkedList<Byte>();
                        int offset = 0;
                        double rmsValues[] = new double[maxframesToAnalyse];
                        double rmsAvg = 0.0, rmsAvg1 = 0.0, rmsAvg2 = 0.0, rmsAvg3 = 0.0;
                        boolean SongFound = false;
                        frame = 0;
                        int TotalFrame = 0;
                        boolean NeedToSkip = false;
                        boolean haveData = false;
                        while (isRecording) {

                            if (SongFound ){
                                if(!haveData){
                                    for (int i = 0; i < bufferSize * framesToSent; i++) {
                                        audioToSend[i] = myQueue.poll();
                                    }
                                    haveData = true;
                                }
                                if(NumOfReq ==0){
                                    ConvertToString(audioToSend);
                                    haveData = false;
                                    NumOfReq += 1;
                                    Requested = true;
                                    SongFound = false;
                                    NeedToSkip = false;
                                    frame = 0;
                                    rmsAvg = 0.0;
                                    rmsAvg1 = 0.0;
                                    rmsAvg2 = 0.0;
                                    rmsAvg3 = 0.0;
                                }
                                else {
                                    if (Requested) {
                                        NeedToSkip = true;
                                    }
                                    if (!Requested) {
                                        if (code != 1) {
                                            ConvertToString(audioToSend);
                                        }
                                        NumOfReq += 1;
                                        haveData = false;
                                        Requested = true;
                                        SongFound = false;
                                        NeedToSkip = false;
                                        frame = 0;
                                        rmsAvg = 0.0;
                                        rmsAvg1 = 0.0;
                                        rmsAvg2 = 0.0;
                                        rmsAvg3 = 0.0;
                                    }
                                }
                            }
                            TotalFrame += 1;
                            frame = frame + 1;
                            double rms;
                            int bufferCount = audioRecorder.read(audioData, offset, bufferSize);
                            rms = RMS(audioData);
                            if(NeedToSkip){
                                continue;
                            }
                            rmsValues[frame-1] = rms;
                            for (int i = 0; i < bufferCount; i++) {
                                myQueue.add(audioData[i]);
                            }
                            if (frame == framesToSent){
                                for (int i=0;i<framesToSent;i++){
                                    rmsAvg = rmsAvg+ rmsValues[i];
                                }
                                for (int i=0;i<tempframe;i++){
                                    rmsAvg1 = rmsAvg1 + rmsValues[i];
                                    rmsAvg2 = rmsAvg2 + rmsValues[i+tempframe];
                                    rmsAvg3 = rmsAvg3 + rmsValues[i+2*tempframe];

                                }
                                rmsAvg = rmsAvg/framesToSent;
                                rmsAvg1 = rmsAvg1/ tempframe;
                                rmsAvg2 = rmsAvg2/ tempframe;
                                rmsAvg3 = rmsAvg3/ tempframe;
                            }
                            else if(frame > framesToSent){
                                for (int i = 0; i < bufferCount; i++) {
                                    myQueue.poll();
                                }
                                rmsAvg = (rmsAvg*framesToSent - rmsValues[frame-framesToSent-1] + rmsValues[frame-1])/framesToSent;
                                rmsAvg1 = (rmsAvg1*tempframe - rmsValues[frame-3*tempframe-1] + rmsValues[frame-2*tempframe-1])/tempframe;
                                rmsAvg2 = (rmsAvg2*tempframe - rmsValues[frame-2*tempframe-1] + rmsValues[frame-tempframe-1])/tempframe;
                                rmsAvg3 = (rmsAvg3*tempframe - rmsValues[frame-tempframe-1] + rmsValues[frame-1])/tempframe;

                            }

                            final  int frm = frame;
                            final  int tfrm = TotalFrame;
                            final double avg = rmsAvg;
                            final double avg1 = rmsAvg1;
                            final double avg2 = rmsAvg2;
                            final double avg3 = rmsAvg3;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    disp.setText("Total frame: "+ tfrm +"Frame: "+ frm + " rms avg: "+ avg+ " "+ avg1 +" "+ avg2+ " "+avg3);
                                }
                            });
                            if (rmsAvg> threshold && rmsAvg1> threshold - 1 && rmsAvg2> threshold - 1 && rmsAvg3> threshold - 1){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dispbot.setText("music found");
                                    }
                                });
                                SongFound = true;
                            }

                            if (TotalFrame == maxframesToAnalyse || code ==1  || NumOfReq >= 3){
                                audioRecorder.release();
                                break;
                            }
                        }
                    }
                });
                t1.start();
            }
        }
    }

    private void resetValue() {
        isRecording = false;
        frame = 0;
         code = 0;
         Requested = false;
         NumOfReq = 0;

    }

}


