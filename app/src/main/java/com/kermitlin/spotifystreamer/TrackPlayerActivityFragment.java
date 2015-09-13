package com.kermitlin.spotifystreamer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class TrackPlayerActivityFragment extends DialogFragment {

    private String artistName;
    private String[] albumName = {}, albumPic = {}, trackName = {}, trackPreviewURL = {};
    private int selectPosition;
    private  SeekBar skbProgress;
    private  TextView elapsed_time;
    private Button bt_Previous;
    static Button bt_Play;
    static Button bt_Pause;
    private Button bt_Next;
    //test commit

    public interface Callback {
        /**
         * TrackPlayerActivityFragmentCallback for when an item has been selected.
         */
        public void onPreviousOrNextClick(Bundle trackplayExtra);
    }

    public TrackPlayerActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);

        setStyle(STYLE_NO_FRAME, getTheme());
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

        Bundle arguments = getArguments();
        if (arguments != null) {
            artistName = getArguments().getString("EXTRA_ARTIST_NAME");
            albumName = getArguments().getStringArray("EXTRA_ALBUM_NAME");
            albumPic = getArguments().getStringArray("EXTRA_ALBUM_PIC");
            trackName = getArguments().getStringArray("EXTRA_TRACK_NAME");
            trackPreviewURL = getArguments().getStringArray("EXTRA_PREVIEW_URL");
            selectPosition = getArguments().getInt("EXTRA_POS");
        } else {
            artistName = globalVariable.artistName;
            albumName = globalVariable.albumName;
            albumPic = globalVariable.albumPic;
            trackName = globalVariable.trackName;
            trackPreviewURL = globalVariable.trackPreviewURL;
            selectPosition = globalVariable.positionNow;
        }

        if (selectPosition != globalVariable.positionPre) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), PlayerService.class);
            getActivity().stopService(intent);

            playMusic(trackPreviewURL[selectPosition]);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver_seekbar_max,
                new IntentFilter("setMaxSeekbar"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver_seekbar_update,
                new IntentFilter("updateSeekbar"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver_elapsedTime,
                new IntentFilter("updateElapsedTime"));
    }

    public BroadcastReceiver mMessageReceiver_seekbar_max = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String max = intent.getStringExtra("MAX");
            skbProgress.setMax(Integer.parseInt(max));
        }
    };

    public BroadcastReceiver mMessageReceiver_seekbar_update = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String pos = intent.getStringExtra("POS");
            skbProgress.setProgress(Integer.parseInt(pos));
        }
    };

    public BroadcastReceiver mMessageReceiver_elapsedTime = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String pos = intent.getStringExtra("POS");
            elapsed_time.setText(pos);
        }
    };

    public void playMusic(String url) {
        Intent intent = new Intent(PlayerService.ACTION_FOREGROUND);
        intent.setClass(getActivity(), PlayerService.class);
        intent.putExtra("URL", url);
        intent.putExtra("PIC", albumPic[selectPosition]);
        intent.putExtra("ARTIST_NAME", artistName);
        intent.putExtra("TRACK_NAME", trackName[selectPosition]);
        getActivity().startService(intent);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

                if (keyCode == KeyEvent.KEYCODE_BACK) {
//                    Intent intent = new Intent();
//                    intent.setClass(getActivity(), PlayerService.class);
//                    getActivity().stopService(intent);
                    globalVariable.positionPre = selectPosition;

                    getActivity().finish();
                    return true;
                }
                return false;
            }
        });

        return dialog;
    }

    @Override
    public void onResume() {

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = getResources().getDimensionPixelSize(R.dimen.popup_width);
            int height = getResources().getDimensionPixelSize(R.dimen.popup_height);
            getDialog().getWindow().setLayout(width, height);
        }

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        globalVariable.positionPre = selectPosition;

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        TextView tv_artistName = (TextView) rootView.findViewById(R.id.artist_name);
        TextView tv_albumName = (TextView) rootView.findViewById(R.id.album_name);
        ImageView iv_albumPic = (ImageView) rootView.findViewById(R.id.album_pic);
        TextView tv_trackName = (TextView) rootView.findViewById(R.id.track_name);
        elapsed_time = (TextView) rootView.findViewById(R.id.duration_start);
        skbProgress = (SeekBar) rootView.findViewById(R.id.scrub_bar);
        skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());

        tv_artistName.setText(artistName);
        tv_albumName.setText(albumName[selectPosition]);
        Picasso.with(getActivity()).load(albumPic[selectPosition]).into(iv_albumPic);
        tv_trackName.setText(trackName[selectPosition]);

        bt_Previous = (Button) rootView.findViewById(R.id.bt_previous);
        bt_Play = (Button) rootView.findViewById(R.id.bt_play);
        bt_Pause = (Button) rootView.findViewById(R.id.bt_pause);
        bt_Next = (Button) rootView.findViewById(R.id.bt_next);

        if (PlayerService.mMediaPlayer != null && PlayerService.mMediaPlayer.isPlaying()) {
            bt_Play.setVisibility(View.GONE);
            bt_Pause.setVisibility(View.VISIBLE);
        }

        bt_Previous.setOnClickListener(onClickPrevious);
        bt_Play.setOnClickListener(onClickPlay);
        bt_Pause.setOnClickListener(onClickPause);
        bt_Next.setOnClickListener(onClickNext);

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

                if (keyCode == KeyEvent.KEYCODE_BACK) {
//                    Intent intent = new Intent();
//                    intent.setClass(getActivity(), PlayerService.class);
//                    getActivity().stopService(intent);
                    globalVariable.positionPre = selectPosition;

                    getActivity().finish();
                    return true;
                }
                return false;
            }
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        switch (item.getItemId()) {
            case android.R.id.home:
                globalVariable.positionPre = selectPosition;
                Log.v("sssssss", "sssssss");
//                getActivity().finish();
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Button.OnClickListener onClickPrevious = new Button.OnClickListener() {
        public void onClick(View v) {
            GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

            if (selectPosition != 0) {
                globalVariable.positionPre = selectPosition;

                selectPosition = selectPosition - 1;

//                Intent intent = new Intent();
//                intent.setClass(getActivity(), PlayerService.class);
//                getActivity().stopService(intent);

                globalVariable.artistName = artistName;
                globalVariable.albumName = albumName;
                globalVariable.albumPic = albumPic;
                globalVariable.trackName = trackName;
                globalVariable.trackPreviewURL = trackPreviewURL;
                globalVariable.positionNow = selectPosition;

                Bundle extras = new Bundle();
                extras.putString("EXTRA_ARTIST_NAME", artistName);
                extras.putStringArray("EXTRA_ALBUM_NAME", albumName);
                extras.putStringArray("EXTRA_ALBUM_PIC", albumPic);
                extras.putStringArray("EXTRA_TRACK_NAME", trackName);
                extras.putStringArray("EXTRA_PREVIEW_URL", trackPreviewURL);
                extras.putInt("EXTRA_POS", selectPosition);

                ((Callback) getActivity()).onPreviousOrNextClick(extras);

            } else {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "There is no previous preview music.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    private Button.OnClickListener onClickPlay = new Button.OnClickListener() {
        public void onClick(View v) {
            if (PlayerService.isPrepared) {
                bt_Play.setVisibility(View.GONE);
                bt_Pause.setVisibility(View.VISIBLE);
                PlayerService.mMediaPlayer.start();
            } else {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Loading...", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    private Button.OnClickListener onClickPause = new Button.OnClickListener() {
        public void onClick(View v) {
            bt_Play.setVisibility(View.VISIBLE);
            bt_Pause.setVisibility(View.GONE);

            PlayerService.mMediaPlayer.pause();
        }
    };

    private Button.OnClickListener onClickNext = new Button.OnClickListener() {
        public void onClick(View v) {
            GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
            if (selectPosition != 9) {
                globalVariable.positionPre = selectPosition;

                selectPosition = selectPosition + 1;

                globalVariable.artistName = artistName;
                globalVariable.albumName = albumName;
                globalVariable.albumPic = albumPic;
                globalVariable.trackName = trackName;
                globalVariable.trackPreviewURL = trackPreviewURL;
                globalVariable.positionNow = selectPosition;

                Bundle extras = new Bundle();
                extras.putString("EXTRA_ARTIST_NAME", artistName);
                extras.putStringArray("EXTRA_ALBUM_NAME", albumName);
                extras.putStringArray("EXTRA_ALBUM_PIC", albumPic);
                extras.putStringArray("EXTRA_TRACK_NAME", trackName);
                extras.putStringArray("EXTRA_PREVIEW_URL", trackPreviewURL);
                extras.putInt("EXTRA_POS", selectPosition);

                ((Callback) getActivity()).onPreviousOrNextClick(extras);
            } else {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "There is no next preview music.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {

        int pos = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            pos = progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            PlayerService.mMediaPlayer.seekTo(pos);
        }
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            skbProgress.setProgress(PlayerService.mMediaPlayer.getCurrentPosition());
//            //your code
//        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            skbProgress.setProgress(PlayerService.mMediaPlayer.getCurrentPosition());
//            //your code
//
//        }
//    }
}
