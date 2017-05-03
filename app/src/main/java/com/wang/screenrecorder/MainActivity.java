package com.wang.screenrecorder;

import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.wang.screenavrecorder.MediaAudioEncoder;
import com.wang.screenavrecorder.MediaEncoder;
import com.wang.screenavrecorder.MediaMuxerWrapper;
import com.wang.screenavrecorder.AudioRecord;
import com.wang.screenavrecorder.MediaVideoEncoder;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final String DIR_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ScreenRecorder";

    private final String DIR_SCREEN = DIR_NAME + File.separator + "video";

    private final String DIR_AUDIO = DIR_NAME + File.separator + "audio";

    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaMuxerWrapper mMuxer;

    private Button mScreenRecBtn;
    private Button mAudioRecBtn;
    private Button mAudioPlayBtn;

    private MediaPlayer mMediaPlayer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScreenRecBtn = (Button) findViewById(R.id.screen_rec_btn);
        mAudioRecBtn = (Button) findViewById(R.id.audio_rec_btn);
        mAudioPlayBtn = (Button) findViewById(R.id.play_audio_btn);
        mScreenRecBtn.setOnClickListener(this);
        mAudioRecBtn.setOnClickListener(this);
        mAudioPlayBtn.setOnClickListener(this);
        //noinspection ResourceType
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mMediaPlayer = new MediaPlayer();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mMediaProjection == null) {
            Log.e(TAG, "media projection is null");
            return;
        }
        final int width = 720;
        final int height = 1280;
        startRecording(width, height);
        mScreenRecBtn.setText("stop video recorder");
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.screen_rec_btn:
                if (mMuxer != null || mScreenRecBtn.getText().equals("stop video recorder")) {
                    stopRecording();
                    mScreenRecBtn.setText("start video recorder");
                } else {
                    Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE);
                    mScreenRecBtn.setText("stop video recorder");
                }
                break;
            case R.id.audio_rec_btn:
                if (mAudioRecBtn.getText().equals("stop audio recorder")) {
                    AudioRecord.getInstance().stopRecordAndFile();
                    mAudioRecBtn.setText("start audio recorder");
                } else {
                    AudioRecord.getInstance().startRecordAndFile(DIR_AUDIO);
                    mAudioRecBtn.setText("stop audio recorder");
                }
                break;
            case R.id.play_audio_btn:
                if (mAudioPlayBtn.getText().equals("play audio")) {
                    String currentPath = AudioRecord.getInstance().getCurrentPath();
                    if (TextUtils.isEmpty(currentPath)){
                        Toast.makeText(this, "no audio to play", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startPlaying(currentPath);
                    mAudioPlayBtn.setText("stop play");
                } else {
                    stopPlaying();
                    mAudioPlayBtn.setText("play audio");
                }
                break;

        }

    }


    private void startPlaying(String currentPath) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "over");
                    stopPlaying();
                }
            });
            mMediaPlayer.setDataSource(currentPath);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
        } catch (IOException e) {
            Log.e("error", "prepare() failed");
            mAudioPlayBtn.setText("play audio");
        }
    }

    private void stopPlaying() {
        mMediaPlayer.stop();
        mAudioPlayBtn.setText("play audio");
    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     *
     * @param width
     * @param height
     */
    private void startRecording(int width, int height) {
        Log.v(TAG, "startRecording:");
        try {
            mMuxer = new MediaMuxerWrapper(DIR_SCREEN ,".mp4");    // if you record audio only, ".m4a" is also OK.
            // for video capturing
            new MediaVideoEncoder(mMuxer, mListener, width, height);
            // for audio capturing
            new MediaAudioEncoder(mMuxer, mListener);
            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    private void stopRecording() {
        Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();

        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    private MediaEncoder.MediaEncoderListener mListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder) {
                MediaVideoEncoder videoEncoder = (MediaVideoEncoder) encoder;
                mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                        videoEncoder.getWidth(), videoEncoder.getHeight(), 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        videoEncoder.getSurface(), null, null);
            }
        }

        @Override
        public void onStopped(MediaEncoder encoder) {
        }
    };

}
