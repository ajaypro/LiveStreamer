package com.droid.poc.livestreamplayer;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.gms.measurement.AppMeasurement;
import com.google.firebase.analytics.FirebaseAnalytics;


/**
 * Created by Ajay Deepak on 27-11-2018.
 */
public class PlayerActivity extends AppCompatActivity {

    SimpleExoPlayer mSimpleExoPlayer;

    private FirebaseAnalytics mFirebaseAnalytics;

    private boolean mPlayWhenReady = true;
    private int mCurrentWindow;
    private long mPlaybackPosition;
    private final static String TAG = "PlayerActivity";
    private ComponentListener mComponentListener;

    PlayerView mPlayerView;

    private static final DefaultBandwidthMeter BANDWIDTH_METER =
            new DefaultBandwidthMeter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mComponentListener = new ComponentListener();
        mPlayerView = findViewById(R.id.video_view);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();

            Bundle params = new Bundle();
            params.putInt("Initialized player", Player.STATE_READY);

            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.APP_OPEN, params);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT <= 23 || mSimpleExoPlayer == null)) {
            initializePlayer();
        }
    }

    // hiding systemUI for full screen experience
    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        mPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void initializePlayer(){
        if( mSimpleExoPlayer == null) {

            // a factory to create an AdaptiveVideoTrackSelection
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

            mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
                    new DefaultRenderersFactory(this),
                    new DefaultTrackSelector(adaptiveTrackSelectionFactory),
                    new DefaultLoadControl());
            // Adding componentlistener to listen to player states
            mSimpleExoPlayer.addListener(mComponentListener);
            mSimpleExoPlayer.addVideoListener(mComponentListener);

            mPlayerView.setPlayer(mSimpleExoPlayer);

            mSimpleExoPlayer.setPlayWhenReady(mPlayWhenReady);
            mSimpleExoPlayer.seekTo(mCurrentWindow, mPlaybackPosition);


        }
        // Preparing media

        Uri uri = Uri.parse(getString(R.string.media_rtmp_stream));
        MediaSource media = buildMediaSource(uri);
        mSimpleExoPlayer.prepare(media);
    }

    private MediaSource buildMediaSource(Uri uri) {

        /**
         * RTMP for boradcasting live streaming vidoe to server and also to playback from server
         * Here we fetch from server
         */

        RtmpDataSourceFactory rtmpDataSourceFactory = new RtmpDataSourceFactory();

        MediaSource mediaSource = new ExtractorMediaSource
                .Factory(rtmpDataSourceFactory)
                .createMediaSource(uri);

        return mediaSource;

        //Below code is for other mediasource formats


        /*DefaultExtractorsFactory extractorsFactory =
                new DefaultExtractorsFactory();
        DefaultHttpDataSourceFactory dataSourceFactory =
                new DefaultHttpDataSourceFactory( "user-agent");

        ExtractorMediaSource videoSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(uri);

        Uri audioUri = Uri.parse(getString(R.string.media_url_mp3));
        ExtractorMediaSource audioSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(audioUri);
                        return new ConcatenatingMediaSource(audioSource, videoSource);

       // Dash for streaming videos

        Uri liveUri = Uri.parse(getString(R.string.media_live));

        DataSource.Factory manifestDataSourceFactory =
                new DefaultHttpDataSourceFactory("ua");
        DashChunkSource.Factory dashChunkSourceFactory =
                new DefaultDashChunkSource.Factory(
                        new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
        return new DashMediaSource.Factory(dashChunkSourceFactory,
                manifestDataSourceFactory).createMediaSource(dashUri);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "Exo2"), BANDWIDTH_METER);

        // Produces Extractor instances for parsing the media data.



        // This is the MediaSource representing the media to be played.
       return new HlsMediaSource.Factory(dataSourceFactory)
                .setExtractorFactory(new DefaultHlsExtractorFactory())
                .createMediaSource(liveUri);*/

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            // Save instance of current player

        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (mSimpleExoPlayer != null) {
            mCurrentWindow = mSimpleExoPlayer.getCurrentWindowIndex();
            mPlayWhenReady = mSimpleExoPlayer.getPlayWhenReady();
            mPlaybackPosition = mSimpleExoPlayer.getCurrentPosition();
            mSimpleExoPlayer.removeListener(mComponentListener);
            mSimpleExoPlayer.removeVideoListener(mComponentListener);
            mSimpleExoPlayer.release();
            mSimpleExoPlayer = null;
        }
    }

    private class ComponentListener extends Player.DefaultEventListener implements
            VideoListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            switch (playbackState){
                case Player.STATE_IDLE:
                    stateString = "Player.STATE_IDLE      -";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "Player.STATE_BUFFERING -";
                    break;
                case Player.STATE_READY:
                    stateString = "Player.STATE_READY     -";
                    break;
                case Player.STATE_ENDED:
                    stateString = "Player.STATE_ENDED     -";
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString
                    + " playWhenReady: " + playWhenReady);

            Toast.makeText(PlayerActivity.this, "changed state" + stateString, Toast.LENGTH_SHORT).show();

            /*Bundle params = new Bundle();
            params.putString(stateString, Event.);

            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.APP_OPEN, params)*/;


            }

        /**
         * Called each time there's a change in the size of the video being rendered.
         *
         */
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {


            Log.d(TAG, "Video size changed" + width + height);

        }

        /**
         * Called when a frame is rendered for the first time since setting the surface, and when a
         * frame is rendered for the first time since a video track was selected.
         */
        @Override
        public void onRenderedFirstFrame() {
            Log.d(TAG, "First Frame detected");
            Toast.makeText(PlayerActivity.this, "First frame", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups,
                TrackSelectionArray trackSelections) {
            super.onTracksChanged(trackGroups, trackSelections);

            Toast.makeText(PlayerActivity.this, "track changed", Toast.LENGTH_SHORT).show();

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SOURCE, String.valueOf(trackSelections));
            mFirebaseAnalytics.logEvent("track_changed", bundle);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            super.onLoadingChanged(isLoading);

            if(isLoading) {
                Toast.makeText(PlayerActivity.this, "loading changed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            super.onPlayerError(error);
            Toast.makeText(PlayerActivity.this, "Error occured :" + error , Toast.LENGTH_SHORT).show();
            releasePlayer();
        }
    }

    }


