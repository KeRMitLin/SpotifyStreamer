package com.kermitlin.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener {
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    static final String ACTION_FOREGROUND = "com.kermitlin.spotifystreamer.PlayerService.FOREGROUND";
    public static MediaPlayer mMediaPlayer;
    public static boolean isPrepared = false;
    int NOTIFICATION_ID = 101;
    private String videoUrl;
    private Thread backgroundThread, seekbarThread;
    private MediaSession mediaSession;
    private Notification noti_pause, noti_play;
    private String TAG = "services";

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalVariable globalVariable = (GlobalVariable) this.getApplicationContext();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);

        backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playMusic();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        seekbarThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int CurrentPosition = 0;
                int total = mMediaPlayer.getDuration();

                while (mMediaPlayer != null && CurrentPosition < total) {
                    try {
                        Thread.sleep(1000);
                        if (mMediaPlayer != null) {
                            CurrentPosition = mMediaPlayer.getCurrentPosition();

                            updateSeekbarBroadcast(String.valueOf(CurrentPosition));
                            updateTimeBroadcast(getTimeString(CurrentPosition));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Create a new MediaSession
        mediaSession = new MediaSession(this, "debug tag");
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadata.Builder()
//                .putint(MediaMetadata.METADATA_KEY_ALBUM_ART, R.drawable.now_playing)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Pink Floyd")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "The Great Gig in the Sky")
                .build());
        // Indicate you're ready to receive media commands
        mediaSession.setActive(true);
        // Attach a new Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSession.Callback() {

            // Implement your callbacks

        });
        // Indicate you want to receive transport controls via your Callback
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);

        // Create a new Notification
        noti_pause = new Notification.Builder(this)
                // Hide the timestamp
                .setShowWhen(false)
                        // Set the Notification style
                .setStyle(new Notification.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                                // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2))
                        // Set the Notification color
                .setColor(0xFFDB4437)
                        // Set the large and small icons
                .setLargeIcon(globalVariable.bitmapArt[globalVariable.positionNow])
                .setSmallIcon(R.drawable.now_playing)
                        // Set Notification content information
                .setContentTitle(globalVariable.trackName[globalVariable.positionNow])
                .setContentText(globalVariable.artistName)
                        // Add some playback controls
                .addAction(android.R.drawable.ic_media_previous, "prev", retreivePlaybackAction(3))
                .addAction(android.R.drawable.ic_media_pause, "pause", retreivePlaybackAction(1))
                .addAction(android.R.drawable.ic_media_next, "next", retreivePlaybackAction(2))
                .build();

        noti_play = new Notification.Builder(this)
                // Hide the timestamp
                .setShowWhen(false)
                        // Set the Notification style
                .setStyle(new Notification.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                                // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2))
                        // Set the Notification color
                .setColor(0xFFDB4437)
                        // Set the large and small icons
                .setLargeIcon(globalVariable.bitmapArt[globalVariable.positionNow])
                .setSmallIcon(R.drawable.now_playing)
                        // Set Notification content information
                .setContentTitle(globalVariable.trackName[globalVariable.positionNow])
                .setContentText(globalVariable.artistName)
                        // Add some playback controls
                .addAction(android.R.drawable.ic_media_previous, "prev", retreivePlaybackAction(3))
                .addAction(android.R.drawable.ic_media_play, "play", retreivePlaybackAction(1))
                .addAction(android.R.drawable.ic_media_next, "next", retreivePlaybackAction(2))
                .build();

        // Do something with your TransportControls
        MediaController.TransportControls controls = mediaSession.getController().getTransportControls();

        Boolean option = globalVariable.noti_option;
        if (option) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, noti_pause);
        }
    }

    private void setMaxSeekbarBroadcast(String max) {
        Intent intent = new Intent("setMaxSeekbar");
        intent.putExtra("MAX", max);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateSeekbarBroadcast(String pos) {
        Intent intent = new Intent("updateSeekbar");
        intent.putExtra("POS", pos);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateTimeBroadcast(String pos) {
        Intent intent = new Intent("updateElapsedTime");
        intent.putExtra("POS", pos);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private String getTimeString(long millis) {
        StringBuffer buf = new StringBuffer();

        int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

        buf.append(String.format("%01d", minutes))
                .append(":")
                .append(String.format("%02d", seconds));

        return buf.toString();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ACTION_FOREGROUND.equals(intent.getAction())) {
            Log.i(TAG, "Start Play Music in Foreground Service");
            videoUrl = intent.getStringExtra("URL");

            try {
                mMediaPlayer.setDataSource(videoUrl);
            } catch (IllegalArgumentException e) {
            } catch (SecurityException e) {
            } catch (IllegalStateException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }

            mMediaPlayer.setOnPreparedListener(this);
            try {
                mMediaPlayer.prepareAsync();
            } catch (IllegalStateException e) {
            }

            if (!mMediaPlayer.isPlaying()) {
                Log.i(TAG, "Starting Thread...");
                backgroundThread.start();
            } else {
                Log.i(TAG, "Still playing - no new thread started");
            }
        }

        if (ACTION_PREVIOUS.equals(intent.getAction())) {
            GlobalVariable globalVariable = (GlobalVariable) this.getApplicationContext();

            if (globalVariable.positionNow != 0) {
                isPrepared = false;
                mMediaPlayer.stop();

                globalVariable.positionNow = globalVariable.positionNow - 1;
                Intent intent_pre = new Intent(this, TrackPlayerActivity.class);
                intent_pre.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_pre);
            } else {
                Toast toast = Toast.makeText(this.getApplicationContext(), "There is no previous preview music.", Toast.LENGTH_SHORT);
                toast.show();
            }

        }

        if (ACTION_PLAY.equals(intent.getAction())) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                TrackPlayerActivityFragment.bt_Play.setVisibility(View.VISIBLE);
                TrackPlayerActivityFragment.bt_Pause.setVisibility(View.GONE);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, noti_play);
            } else {
                mMediaPlayer.start();
                TrackPlayerActivityFragment.bt_Play.setVisibility(View.GONE);
                TrackPlayerActivityFragment.bt_Pause.setVisibility(View.VISIBLE);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, noti_pause);
            }
        }

        if (ACTION_NEXT.equals(intent.getAction())) {
            GlobalVariable globalVariable = (GlobalVariable) this.getApplicationContext();

            if (globalVariable.positionNow != 9) {
                isPrepared = false;
                mMediaPlayer.stop();

                globalVariable.positionNow = globalVariable.positionNow + 1;
                Intent intent_next = new Intent(this, TrackPlayerActivity.class);
                intent_next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_next);
            } else {
                Toast toast = Toast.makeText(this.getApplicationContext(), "There is no next preview music.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        isPrepared = true;
        TrackPlayerActivityFragment.bt_Play.setVisibility(View.GONE);
        TrackPlayerActivityFragment.bt_Pause.setVisibility(View.VISIBLE);
        setMaxSeekbarBroadcast(String.valueOf(mMediaPlayer.getDuration()));
    }

    private void playMusic() throws InterruptedException {
        Log.i(TAG, "Playing music in Service");
        if (isPrepared) {
            mMediaPlayer.start();
            seekbarThread.start();
        } else {
            backgroundThread.sleep(3000);
            playMusic();
        }
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            isPrepared = false;
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mediaSession.release();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        }
    }

    private PendingIntent retreivePlaybackAction(int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(this, PlayerService.class);
        switch (which) {
            case 1:
                // Play and pause
                action = new Intent(ACTION_PLAY);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 1, action, 0);
                return pendingIntent;
            case 2:
                // Skip tracks
                action = new Intent(ACTION_NEXT);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 2, action, 0);
                return pendingIntent;
            case 3:
                // Previous tracks
                action = new Intent(ACTION_PREVIOUS);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 3, action, 0);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }
}
