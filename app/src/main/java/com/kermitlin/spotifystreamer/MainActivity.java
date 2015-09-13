package com.kermitlin.spotifystreamer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class MainActivity extends Activity implements MainActivityFragment.Callback {

    private static final String TOPTRACKFRAGMENT_TAG = "TTFTAG";
    public boolean mTwoPane;
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_toptrack_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_toptrack_container, new TopTrackActivityFragment(), TOPTRACKFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Fire_setShareIntent"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_now_play) {
            if (PlayerService.mMediaPlayer != null && PlayerService.mMediaPlayer.isPlaying()) {
                Intent intent = new Intent(this, TrackPlayerActivity.class);
                startActivity(intent);
                return true;
            } else {
                Toast.makeText(this, "No music playing!", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Bundle toptrackExtra) {
        if (mTwoPane) {

            TopTrackActivityFragment fragment = new TopTrackActivityFragment();
            fragment.setArguments(toptrackExtra);

            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_toptrack_container, fragment, TOPTRACKFRAGMENT_TAG)
                    .commit();
        } else {

            Intent intent = new Intent(this, TopTrackActivity.class).putExtras(toptrackExtra);
            startActivity(intent);
        }
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GlobalVariable globalVariable = (GlobalVariable) context.getApplicationContext();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Current track: " + globalVariable.trackName[globalVariable.positionNow]
                    + "\nPreview URL: " + globalVariable.trackPreviewURL[globalVariable.positionNow]);
            setShareIntent(shareIntent);
        }
    };

    public void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}
