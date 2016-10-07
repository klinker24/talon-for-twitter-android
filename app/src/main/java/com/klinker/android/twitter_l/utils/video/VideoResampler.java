package com.klinker.android.twitter_l.utils.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

@TargetApi( Build.VERSION_CODES.JELLY_BEAN_MR2 )
public class VideoResampler {

    private static final String MIME_TYPE = "video/mp4";

    private static final int TIMEOUT_USEC = 10000;

    public static final int WIDTH_QCIF = 176;
    public static final int HEIGHT_QCIF = 144;
    public static final int BITRATE_QCIF = 1000000;

    public static final int WIDTH_QVGA = 320;
    public static final int HEIGHT_QVGA = 240;
    public static final int BITRATE_QVGA = 2000000;

    public static final int WIDTH_720P = 1280;
    public static final int HEIGHT_720P = 720;
    public static final int BITRATE_720P = 6000000;

    private static final String TAG = "VideoResampler";
    private static final boolean WORK_AROUND_BUGS = false; // avoid fatal codec bugs
    private static final boolean VERBOSE = true; // lots of logging

    // parameters for the encoder
    public static final int FPS_30 = 30; // 30fps
    public static final int FPS_15 = 15; // 15fps
    public static final int IFRAME_INTERVAL_10 = 10; // 10 seconds between I-frames

    // size of a frame, in pixels
    private int mWidth = WIDTH_720P;
    private int mHeight = HEIGHT_720P;

    // bit rate, in bits per second
    private int mBitRate = BITRATE_720P;

    private int mFrameRate = FPS_30;

    private int mIFrameInterval = IFRAME_INTERVAL_10;

    // private Uri mInputUri;
    private Uri mOutputUri;

    InputSurface mInputSurface;

    OutputSurface mOutputSurface;

    MediaCodec mEncoder = null;

    MediaMuxer mMuxer = null;
    int mTrackIndex = -1;
    boolean mMuxerStarted = false;

    // MediaExtractor mExtractor = null;

    // MediaFormat mExtractFormat = null;

    // int mExtractIndex = 0;

    List<SamplerClip> mClips = new ArrayList<SamplerClip>();

    // int mStartTime = -1;

    // int mEndTime = -1;

    long mLastSampleTime = 0;

    long mEncoderPresentationTimeUs = 0;

    public VideoResampler() {

    }

   /*
    * public void setInput( Uri intputUri ) { mInputUri = intputUri; }
    */

    public void addSamplerClip( SamplerClip clip ) {
        mClips.add( clip );
    }

    public void setOutput( Uri outputUri ) {
        mOutputUri = outputUri;
    }

    public void setOutputResolution( int width, int height ) {
        if ( ( width % 16 ) != 0 || ( height % 16 ) != 0 ) {
            Log.w( TAG, "WARNING: width or height not multiple of 16" );
        }
        mWidth = width;
        mHeight = height;
    }

    public void setOutputBitRate( int bitRate ) {
        mBitRate = bitRate;
    }

    public void setOutputFrameRate( int frameRate ) {
        mFrameRate = frameRate;
    }

    public void setOutputIFrameInterval( int IFrameInterval ) {
        mIFrameInterval = IFrameInterval;
    }

    /*
     * public void setStartTime( int startTime ) { mStartTime = startTime; }
     *
     * public void setEndTime( int endTime ) { mEndTime = endTime; }
     */
    public void start() throws Throwable {
        VideoEditWrapper wrapper = new VideoEditWrapper();
        Thread th = new Thread( wrapper, "codec test" );
        th.start();
        th.join();
        if ( wrapper.mThrowable != null ) {
            throw wrapper.mThrowable;
        }
    }

    /**
     * Wraps resampleVideo, running it in a new thread. Required because of the way
     * SurfaceTexture.OnFrameAvailableListener works when the current thread has a Looper configured.
     */
    private class VideoEditWrapper implements Runnable {
        private Throwable mThrowable;

        @Override
        public void run() {
            try {
                resampleVideo();
            } catch ( Throwable th ) {
                mThrowable = th;
            }
        }
    }

    private void setupEncoder() {
        MediaFormat outputFormat = MediaFormat.createVideoFormat( MIME_TYPE, mWidth, mHeight );
        outputFormat.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
        outputFormat.setInteger( MediaFormat.KEY_BIT_RATE, mBitRate );

        outputFormat.setInteger( MediaFormat.KEY_FRAME_RATE, mFrameRate );
        outputFormat.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval );

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = new InputSurface(mEncoder.createInputSurface());
            mInputSurface.makeCurrent();
            mEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMuxer() {
        try {
            mMuxer = new MediaMuxer( mOutputUri.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
        } catch ( IOException ioe ) {
            throw new RuntimeException( "MediaMuxer creation failed", ioe );
        }
    }

    private void resampleVideo() {

        setupEncoder();
        setupMuxer();

        for ( SamplerClip clip : mClips ) {
            feedClipToEncoder( clip );
        }

        mEncoder.signalEndOfInputStream();

        releaseOutputResources();
    }

    private void feedClipToEncoder( SamplerClip clip ) {

        mLastSampleTime = 0;

        MediaCodec decoder = null;

        MediaExtractor extractor = setupExtractorForClip(clip);

        if(extractor == null ) {
            return;
        }

        int trackIndex = getVideoTrackIndex(extractor);
        extractor.selectTrack( trackIndex );

        MediaFormat clipFormat = extractor.getTrackFormat( trackIndex );

        if ( clip.getStartTime() != -1 ) {
            extractor.seekTo( clip.getStartTime() * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC );
            clip.setStartTime( extractor.getSampleTime() / 1000 );
        }

        try {
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);
            mOutputSurface = new OutputSurface();

            decoder.configure(clipFormat, mOutputSurface.getSurface(), null, 0);
            decoder.start();

            resampleVideo(extractor, decoder, clip);

        } catch (IOException e) {

        } finally {

            if ( mOutputSurface != null ) {
                mOutputSurface.release();
            }
            if ( decoder != null ) {
                decoder.stop();
                decoder.release();
            }

            if ( extractor != null ) {
                extractor.release();
                extractor = null;
            }
        }
    }

    private MediaExtractor setupExtractorForClip(SamplerClip clip ) {


        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource( clip.getUri().toString() );
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }

        return extractor;
    }

    private int getVideoTrackIndex(MediaExtractor extractor) {

        for ( int trackIndex = 0; trackIndex < extractor.getTrackCount(); trackIndex++ ) {
            MediaFormat format = extractor.getTrackFormat( trackIndex );

            String mime = format.getString( MediaFormat.KEY_MIME );
            if ( mime != null ) {
                if ( mime.equals( "video/avc" ) ) {
                    return trackIndex;
                }
            }
        }

        return -1;
    }

    private void releaseOutputResources() {

        if ( mInputSurface != null ) {
            mInputSurface.release();
        }

        if ( mEncoder != null ) {
            mEncoder.stop();
            mEncoder.release();
        }

        if ( mMuxer != null ) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    private void resampleVideo( MediaExtractor extractor, MediaCodec decoder, SamplerClip clip ) {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int outputCount = 0;

        long endTime = clip.getEndTime();

        if ( endTime == -1 ) {
            endTime = clip.getVideoDuration();
        }

        boolean outputDoneNextTimeWeCheck = false;

        boolean outputDone = false;
        boolean inputDone = false;
        boolean decoderDone = false;

        while ( !outputDone ) {
            if ( VERBOSE )
                Log.d( TAG, "edit loop" );
            // Feed more data to the decoder.
            if ( !inputDone ) {
                int inputBufIndex = decoder.dequeueInputBuffer( TIMEOUT_USEC );
                if ( inputBufIndex >= 0 ) {
                    if ( extractor.getSampleTime() / 1000 >= endTime ) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer( inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                        inputDone = true;
                        if ( VERBOSE )
                            Log.d( TAG, "sent input EOS (with zero-length frame)" );
                    } else {
                        // Copy a chunk of input to the decoder. The first chunk should have
                        // the BUFFER_FLAG_CODEC_CONFIG flag set.
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        inputBuf.clear();

                        int sampleSize = extractor.readSampleData( inputBuf, 0 );
                        if ( sampleSize < 0 ) {
                            Log.d( TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM" );
                            decoder.queueInputBuffer( inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                        } else {
                            Log.d( TAG, "InputBuffer ADVANCING" );
                            decoder.queueInputBuffer( inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0 );
                            extractor.advance();
                        }

                        inputChunk++;
                    }
                } else {
                    if ( VERBOSE )
                        Log.d( TAG, "input buffer not available" );
                }
            }

            // Assume output is available. Loop until both assumptions are false.
            boolean decoderOutputAvailable = !decoderDone;
            boolean encoderOutputAvailable = true;
            while ( decoderOutputAvailable || encoderOutputAvailable ) {
                // Start by draining any pending output from the encoder. It's important to
                // do this before we try to stuff any more data in.
                int encoderStatus = mEncoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
                if ( encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
                    // no output available yet
                    if ( VERBOSE )
                        Log.d( TAG, "no output from encoder available" );
                    encoderOutputAvailable = false;
                } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    if ( VERBOSE )
                        Log.d( TAG, "encoder output buffers changed" );
                } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {

                    MediaFormat newFormat = mEncoder.getOutputFormat();

                    mTrackIndex = mMuxer.addTrack( newFormat );
                    mMuxer.start();
                    mMuxerStarted = true;
                    if ( VERBOSE )
                        Log.d( TAG, "encoder output format changed: " + newFormat );
                } else if ( encoderStatus < 0 ) {
                    // fail( "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus );
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if ( encodedData == null ) {
                        // fail( "encoderOutputBuffer " + encoderStatus + " was null" );
                    }
                    // Write the data to the output "file".
                    if ( info.size != 0 ) {
                        encodedData.position( info.offset );
                        encodedData.limit( info.offset + info.size );
                        outputCount++;

                        mMuxer.writeSampleData( mTrackIndex, encodedData, info );

                        if ( VERBOSE )
                            Log.d( TAG, "encoder output " + info.size + " bytes" );
                    }
                    outputDone = ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0;

                    mEncoder.releaseOutputBuffer( encoderStatus, false );
                }

                if ( outputDoneNextTimeWeCheck ) {
                    outputDone = true;
                }

                if ( encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER ) {
                    // Continue attempts to drain output.
                    continue;
                }
                // Encoder is drained, check to see if we've got a new frame of output from
                // the decoder. (The output is going to a Surface, rather than a ByteBuffer,
                // but we still get information through BufferInfo.)
                if ( !decoderDone ) {
                    int decoderStatus = decoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
                    if ( decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
                        // no output available yet
                        if ( VERBOSE )
                            Log.d( TAG, "no output from decoder available" );
                        decoderOutputAvailable = false;
                    } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
                        // decoderOutputBuffers = decoder.getOutputBuffers();
                        if ( VERBOSE )
                            Log.d( TAG, "decoder output buffers changed (we don't care)" );
                    } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {
                        // expected before first buffer of data
                        MediaFormat newFormat = decoder.getOutputFormat();
                        if ( VERBOSE )
                            Log.d( TAG, "decoder output format changed: " + newFormat );
                    } else if ( decoderStatus < 0 ) {
                        // fail( "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus );
                    } else { // decoderStatus >= 0
                        if ( VERBOSE )
                            Log.d( TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")" );
                        // The ByteBuffers are null references, but we still get a nonzero
                        // size for the decoded data.
                        boolean doRender = ( info.size != 0 );
                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture. The API doesn't
                        // guarantee that the texture will be available before the call
                        // returns, so we need to wait for the onFrameAvailable callback to
                        // fire. If we don't wait, we risk rendering from the previous frame.
                        decoder.releaseOutputBuffer( decoderStatus, doRender );
                        if ( doRender ) {
                            // This waits for the image and renders it after it arrives.
                            if ( VERBOSE )
                                Log.d( TAG, "awaiting frame" );
                            mOutputSurface.awaitNewImage();
                            mOutputSurface.drawImage();
                            // Send it to the encoder.

                            long nSecs = info.presentationTimeUs * 1000;

                            if ( clip.getStartTime() != -1 ) {
                                nSecs = ( info.presentationTimeUs - ( clip.getStartTime() * 1000 ) ) * 1000;
                            }

                            Log.d( "this", "Setting presentation time " + nSecs / ( 1000 * 1000 ) );
                            nSecs = Math.max( 0, nSecs );

                            mEncoderPresentationTimeUs += ( nSecs - mLastSampleTime );

                            mLastSampleTime = nSecs;

                            mInputSurface.setPresentationTime( mEncoderPresentationTimeUs );
                            if ( VERBOSE )
                                Log.d( TAG, "swapBuffers" );
                            mInputSurface.swapBuffers();
                        }
                        if ( ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
                            // mEncoder.signalEndOfInputStream();
                            outputDoneNextTimeWeCheck = true;
                        }
                    }
                }
            }
        }
        if ( inputChunk != outputCount ) {
            // throw new RuntimeException( "frame lost: " + inputChunk + " in, " + outputCount + " out" );
        }
    }

}