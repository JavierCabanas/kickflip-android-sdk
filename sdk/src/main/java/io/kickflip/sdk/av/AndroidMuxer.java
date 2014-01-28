package io.kickflip.sdk.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by davidbrodsky on 1/23/14.
 */
public class AndroidMuxer extends Muxer {
    private static final String TAG = "AndroidMuxer";
    private static final boolean VERBOSE = false;

    private MediaMuxer mMuxer;
    private boolean mStarted;

    private AndroidMuxer(String outputFile, FORMAT format){
        super();
        try {
            switch(format){
                case MPEG4:
                    mMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized format!");
            }
        } catch (IOException e) {
            throw new RuntimeException("MediaMuxer creation failed", e);
        }
        mStarted = false;
    }

    public static AndroidMuxer create(String outputFile, FORMAT format) {
        return new AndroidMuxer(outputFile, format);
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        if(mStarted)
            throw new RuntimeException("format changed twice");
        int track = mMuxer.addTrack(trackFormat);

        if(allTracksAdded()){
           start();
        }
        return track;
    }

    @Override
    public void start() {
        mMuxer.start();
        mStarted = true;
    }

    @Override
    public void stop() {
        mMuxer.stop();
        mStarted = false;
    }

    @Override
    public void release() {
        mMuxer.release();
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(trackIndex, encodedData, bufferInfo);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // MediaMuxer gets the codec config info via the addTrack command
            if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            return;
        }

        if (!mStarted) {
            Log.e(TAG, "writeSampleData called before muxer started. Track index: " + trackIndex + " tracks added: " + mNumTracks);
            return;
            //throw new RuntimeException("muxer hasn't started");
        }

        mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);

        if(allTracksFinished()){
            stop();
        }
    }
}