package com.example.voicerecordingapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;
    private Chronometer chronometer;
    private TextView id_txt_start_time,id_txt_end_time,id_txt_recorder_name;
    private Button id_btn_play,id_btn_pause,id_btn_forwad,id_btn_rewind;
    private SeekBar id_seekbar;

    private double startTime = 0;
    private double finalTime = 0;

    private Handler myHandler = new Handler();;
    private int forwardTime = 3000;
    private int backwardTime = 3000;
    public static int oneTimeOnly = 0;


    private ImageView imageViewRecord, imageViewStop, imageViewPause, imageViewResume;
    private LinearLayout linearLayoutRecorder, linearLayoutPlay,linearViewRecord,linearViewStop;
    private MediaRecorder mRecorder;
    private MediaPlayer mediaPlayer;

    private String fileName = null;
    private int RECORD_AUDIO_REQUEST_CODE = 123;

    private boolean isRecording = false;
    private long pauseOffset = 0;

    private ArrayList<String > arrayListFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
        }

        initViews();
    }

    private void initViews()
    {
        /** setting up the toolbar  **/
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Voice Recorder");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);

        arrayListFileName = new ArrayList<>();
        linearLayoutRecorder = (LinearLayout) findViewById(R.id.linearLayoutRecorder);
        chronometer = (Chronometer) findViewById(R.id.chronometerTimer);
        imageViewRecord = (ImageView) findViewById(R.id.imageViewRecord);
        imageViewStop = (ImageView) findViewById(R.id.imageViewStop);
        imageViewPause = (ImageView) findViewById(R.id.imageViewPause);
        imageViewResume = (ImageView) findViewById(R.id.imageViewResume);
        linearLayoutPlay = (LinearLayout) findViewById(R.id.linearLayoutPlay);
        linearViewRecord = (LinearLayout) findViewById(R.id.linearViewRecord);
        linearViewStop = (LinearLayout) findViewById(R.id.linearViewStop);

        id_txt_start_time = findViewById(R.id.id_txt_start_time);
        id_txt_end_time = findViewById(R.id.id_txt_end_time);
        id_txt_recorder_name = findViewById(R.id.id_txt_record_name);

        id_btn_play = findViewById(R.id.id_btn_play);
        id_btn_pause = findViewById(R.id.ud_btn_pause);
        id_btn_forwad = findViewById(R.id.id_btn_forward);
        id_btn_rewind = findViewById(R.id.id_btn_rewind);

        id_seekbar = findViewById(R.id.id_seek_bar);

        id_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (mediaPlayer != null && fromUser)
                {
                    mediaPlayer.seekTo(progress);
                    startTime = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        imageViewRecord.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
        imageViewPause.setOnClickListener(this);
        imageViewResume.setOnClickListener(this);
        id_btn_play.setOnClickListener(this);
        id_btn_pause.setOnClickListener(this);
        id_btn_forwad.setOnClickListener(this);
        id_btn_rewind.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.item_list:

                if(!isRecording)
                {
                    if(arrayListFileName.size() != 0)
                    {
                        confirmationDialog("list");
                    }
                    else
                    {
                        gotoRecodingListActivity();
                    }
                }
                else
                {
                    Toast.makeText(this, getString(R.string.stop_recording), Toast.LENGTH_SHORT).show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void gotoRecodingListActivity() {
        Intent intent = new Intent(this, RecordingListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view)
    {
        if (view == imageViewRecord)
        {
            imageViewResume.setVisibility(View.GONE);

            prepareforRecording();
            startRecording();
        }
        else if (view == imageViewResume)
        {
            imageViewResume.setVisibility(View.GONE);
            imageViewPause.setVisibility(View.VISIBLE);
            prepareforRecording();
            startRecording();
        }
        else if (view == imageViewStop)
        {
            imageViewPause.setVisibility(View.VISIBLE);
            imageViewResume.setVisibility(View.VISIBLE);
            prepareforStop();
            stopRecording();

            preparePlaying();
        }
        else if (view == imageViewPause)
        {
            imageViewPause.setVisibility(View.GONE);
            imageViewResume.setVisibility(View.VISIBLE);
            prepareforPause();
            pauseRecording();
        }
        else if (view == id_btn_play)
        {
            startPlaying();
        }
        else if (view == id_btn_pause)
        {
            pausePlaying();
        }
        else if (view == id_btn_forwad)
        {
            forwardPlaying();
        }
        else if (view == id_btn_rewind)
        {
            rewindPlaying();
        }
    }

    private void prepareforStop() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        linearViewRecord.setVisibility(View.VISIBLE);
        linearViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.VISIBLE);
    }

    private void prepareforPause() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        linearViewRecord.setVisibility(View.GONE);
        linearViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);
    }

    private void prepareforRecording() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        linearViewRecord.setVisibility(View.GONE);
        linearViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);
    }


    private void startRecording()
    {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/HealthConcept/Audios");

        if (!file.exists())
        {
            file.mkdirs();
        }

        fileName = root.getAbsolutePath() + "/HealthConcept/Audios/" + String.valueOf(System.currentTimeMillis()+"_P"+ ".mp3");
        Log.d("filename", fileName);

        arrayListFileName.add(fileName);

        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try
        {
            mRecorder.prepare();
            mRecorder.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // making the imageview a stop button
        //starting the chronometer
        startChronometer();
    }

    private void pauseRecording()
    {
        try
        {
            mRecorder.stop();
            mRecorder.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        mRecorder = null;

        //pausing the chronometer
        pauseChromometer();
    }

    private void stopRecording()
    {
        try
        {
            mRecorder.stop();
            mRecorder.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        mRecorder = null;

        //starting the chronometer
        stopChronometer();

        saveAllFiles();
    }

    private void saveAllFiles()
    {
        File root = android.os.Environment.getExternalStorageDirectory();
        String targetFile = root.getAbsolutePath() + "/HealthConcept/Audios/" + String.valueOf(System.currentTimeMillis()+"_F"+ ".mp3");

        fileName = targetFile;

        /*ArrayList to Array Conversion */
        String stringFileArray[] = arrayListFileName.toArray(new String[arrayListFileName.size()]);

        Boolean response = mergeMediaFiles(true, stringFileArray,targetFile);

        if(response)
        {
            deleteAllFiles();
        }

        //showing the play button
        Toast.makeText(this, "Recording saved successfully.", Toast.LENGTH_SHORT).show();
    }

    private void deleteAllFiles()
    {
        for (int i = 0; i < arrayListFileName.size() ; i++)
        {
            File file = new File(arrayListFileName.get(i));

            if(file.exists())
            {
                file.delete();
            }
        }
        arrayListFileName = new ArrayList<String>();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio()
    {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);

        }
    }

    // Callback with the request from calling requestPermissions(...)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                //Toast.makeText(this, "Record Audio permission granted", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

    }

    public static boolean mergeMediaFiles(boolean isAudio, String sourceFiles[], String targetFile)
    {
        try
        {
            String mediaKey = isAudio ? "soun" : "vide";
            List<Movie> listMovies = new ArrayList<>();

            for (String filename : sourceFiles)
            {
                listMovies.add(MovieCreator.build(filename));
            }

            List<Track> listTracks = new LinkedList<>();

            for (Movie movie : listMovies)
            {
                for (Track track : movie.getTracks())
                {
                    if (track.getHandler().equals(mediaKey))
                    {
                        listTracks.add(track);
                    }
                }
            }

            Movie outputMovie = new Movie();

            if (!listTracks.isEmpty())
            {
                outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
            }
            Container container = new DefaultMp4Builder().build(outputMovie);
            FileChannel fileChannel = new RandomAccessFile(String.format(targetFile), "rw").getChannel();
            container.writeContainer(fileChannel);
            fileChannel.close();
            return true;
        }
        catch (IOException e) {
            Log.e(TAG, "Error merging media files. exception: "+e.getMessage());
            return false;
        }
    }

    @Override
    public void onBackPressed()
    {
        if(!isRecording)
        {
            if(arrayListFileName.size() != 0)
            {
                confirmationDialog("onBack");
            }
            else
            {
                super.onBackPressed();
            }
        }
        else
        {
            Toast.makeText(this, getString(R.string.stop_recording), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmationDialog(final String activity)
    {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation !");
        builder.setMessage("Do you want to SAVE Recorded File ?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Save All Recorded Files
                saveAllFiles();

                if(activity.equals("list"))
                {
                    gotoRecodingListActivity();
                }
                else
                {
                    onBackPressed();
                }
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Delete All Recorded Files
                deleteAllFiles();

                if(activity.equals("list"))
                {
                    gotoRecodingListActivity();
                }
                else
                {
                    onBackPressed();
                }
            }
        });

        builder.show();
    }

    private void startChronometer()
    {
        if(!isRecording)
        {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isRecording = true;
        }
    }

    private void pauseChromometer()
    {
        if(isRecording)
        {
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isRecording = false;
        }
    }

    private void stopChronometer()
    {
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        isRecording = false;
    }

    private void preparePlaying()
    {
        try
        {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepare();

            id_txt_recorder_name.setText(fileName.substring(fileName.lastIndexOf(File.separator) + 1));
        }
        catch (IOException e)
        {
            Log.e("LOG_TAG", "prepare() failed");
        }
    }

    private void startPlaying()
    {
        mediaPlayer.start();

        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0)
        {
            id_seekbar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }

        id_txt_end_time.setText(String.format("%02d : %02d",
                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                finalTime)))
        );

        id_txt_start_time.setText(String.format("%02d : %02d",
                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                startTime)))
        );

        id_seekbar.setProgress((int)startTime);
        myHandler.postDelayed(UpdateSongTime,100);
        id_btn_pause.setEnabled(true);
        id_btn_play.setEnabled(false);
    }

    private void pausePlaying()
    {
        mediaPlayer.pause();
        id_btn_pause.setEnabled(false);
        id_btn_play.setEnabled(true);
    }

    private void forwardPlaying()
    {
        int temp = (int)startTime;

        if((temp+forwardTime)<=finalTime)
        {
            startTime = startTime + forwardTime;
            mediaPlayer.seekTo((int) startTime);
        }
    }

    private void rewindPlaying()
    {
        int temp = (int)startTime;

        if((temp-backwardTime)>0)
        {
            startTime = startTime - backwardTime;
            mediaPlayer.seekTo((int) startTime);
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            id_txt_start_time.setText(String.format("%02d : %02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            id_seekbar.setProgress((int)startTime);
            myHandler.postDelayed(this, 100);
        }
    };


}
