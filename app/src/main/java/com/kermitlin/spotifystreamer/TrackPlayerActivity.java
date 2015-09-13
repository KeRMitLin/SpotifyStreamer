package com.kermitlin.spotifystreamer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class TrackPlayerActivity extends Activity implements TrackPlayerActivityFragment.Callback {

    private boolean mIsLargeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);

        if (savedInstanceState == null) {

            TrackPlayerActivityFragment fragment = new TrackPlayerActivityFragment();

            if(getIntent().getExtras() != null)
            {
                fragment.setArguments(getIntent().getExtras());
            }

            Intent intent = new Intent("Fire_setShareIntent");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            if (mIsLargeLayout) {
                fragment.show(getFragmentManager(), "dialog");
            } else {
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.fragment_trackplayer, fragment)
                        .commit();
            }
        }
    }

    @Override
    public void onPreviousOrNextClick(Bundle trackplayExtra) {
        Intent intent = new Intent(this, TrackPlayerActivity.class).putExtras(trackplayExtra);
        startActivity(intent);
        finish();
    }
}
