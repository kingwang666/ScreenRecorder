package com.wang.screenavrecorder;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by wang
 * on 2016/12/7
 */

public class AudioRecord {

    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private boolean isRecord = false;

    private MediaRecorder mMediaRecorder;

    private String mCurrentPath;

    private AudioRecord() {
    }

    private static AudioRecord mInstance;

    public synchronized static AudioRecord getInstance() {
        if (mInstance == null)
            mInstance = new AudioRecord();
        return mInstance;
    }

    public int startRecordAndFile(String dir) {
        //判断是否有外部存储设备sdcard
        if (true) {
            if (isRecord) {
                return 1000;
            } else {
                if (mMediaRecorder == null)
                    mMediaRecorder = new MediaRecorder();
                mMediaRecorder.reset();
                createMediaRecord(dir);
                try {
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                    // 让录制状态为true
                    isRecord = true;
                    return 1;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return 1001;
                }
            }

        } else {
            return 1002;
        }
    }


    public void stopRecordAndFile() {
        close();

    }


    private void createMediaRecord(String dir) {
         /* ①Initial：实例化MediaRecorder对象 */

        /* setAudioSource/setVedioSource*/
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//设置麦克风

        /* 设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default
         * THREE_GPP(3gp格式，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
         */
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);

         /* 设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default */
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setAudioChannels(1);

        File dirFile = new File(dir);
        if (!dirFile.exists()){
            dirFile.mkdirs();
        }

         /* 设置输出文件的路径 */
        File file = new File(dirFile, getDateTimeString() + ".amr");
        if (file.exists()) {
            file.delete();
        }
        mCurrentPath = file.getPath();
        mMediaRecorder.setOutputFile(mCurrentPath);
    }

    public String getCurrentPath(){
        return mCurrentPath;
    }

    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }


    private void close() {
        if (mMediaRecorder != null) {
            isRecord = false;
            mMediaRecorder.stop();
        }
    }
}
