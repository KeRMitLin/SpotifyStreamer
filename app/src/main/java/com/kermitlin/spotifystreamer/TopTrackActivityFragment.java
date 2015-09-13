package com.kermitlin.spotifystreamer;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

/**
 * A placeholder fragment containing a simple view.
 */
public class TopTrackActivityFragment extends Fragment {

    private static final String KEY_Track_LIST = "tracks";
    private static final String KEY_ARTIST_NAME = "artistName";
    private static final String KEY_ALBUM_NAME = "albumName";
    private static final String KEY_ALBUM_PIC = "albumPic";
    private static final String KEY_TRACK_NAME = "trackName";
    private static final String KEY_PREVIEW_URL = "previewURL";
    private String selectName, selectID;
    private String[] track = {}, album = {}, pic = {}, trackNameTemp, albumNameTemp, albumPicTemp, trackPreviewURL;
    private CustomTracksList tracksAdapter;
    private ListView tracksList;
    private ArrayList<TrackList> mTracks;
    private Thread bitmapThread;

    public TopTrackActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);

        Bundle arguments = getArguments();
        if (arguments != null) {
            selectName = getArguments().getString("EXTRA_NAME");
            selectID = getArguments().getString("EXTRA_ID");
        }

        bitmapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

                for (int i = 0; i < pic.length; i = i + 1) {
                    try {
                        URL url = new URL(pic[i]);
                        Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        globalVariable.bitmapArt[i] = image;
                    } catch (IOException e) {
                        // Log exception
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_Track_LIST, mTracks);

        outState.putString(KEY_ARTIST_NAME, selectName);
        outState.putStringArray(KEY_ALBUM_NAME, album);
        outState.putStringArray(KEY_ALBUM_PIC, pic);
        outState.putStringArray(KEY_TRACK_NAME, track);
        outState.putStringArray(KEY_PREVIEW_URL, trackPreviewURL);

        super.onSaveInstanceState(outState);
    }

    public void updateTrack(String str) {
        if (isNetworkConnectionAvailable() && str != null) {
            FetchTrackTask TrackTask = new FetchTrackTask(str);
            TrackTask.execute();
        } else if (str == null) {
            //First time to launch app.
        } else {
            Toast.makeText(getActivity(), "No network connection!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_track, container, false);

        actionBarSetup();

        tracksList = (ListView) rootView.findViewById(R.id.list_track_listview);

        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_Track_LIST);
            tracksAdapter = new CustomTracksList();
            tracksList.setAdapter(tracksAdapter);

            selectName = savedInstanceState.getString(KEY_ARTIST_NAME);
            album = savedInstanceState.getStringArray(KEY_ALBUM_NAME);
            pic = savedInstanceState.getStringArray(KEY_ALBUM_PIC);
            track = savedInstanceState.getStringArray(KEY_TRACK_NAME);
            trackPreviewURL = savedInstanceState.getStringArray(KEY_PREVIEW_URL);

        } else if (savedInstanceState == null) {
            mTracks = new ArrayList<TrackList>();
            updateTrack(selectID);
        }

        tracksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                //Stage 2
                GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
                globalVariable.artistName = selectName;
                globalVariable.albumName = album;
                globalVariable.albumPic = pic;
                globalVariable.trackName = track;
                globalVariable.trackPreviewURL = trackPreviewURL;
                globalVariable.positionNow = position;

                Bundle extras = new Bundle();
                extras.putString("EXTRA_ARTIST_NAME", selectName);
                extras.putStringArray("EXTRA_ALBUM_NAME", album);
                extras.putStringArray("EXTRA_ALBUM_PIC", pic);
                extras.putStringArray("EXTRA_TRACK_NAME", track);
                extras.putStringArray("EXTRA_PREVIEW_URL", trackPreviewURL);
                extras.putInt("EXTRA_POS", position);

                Intent intent = new Intent(getActivity(), TrackPlayerActivity.class).putExtras(extras);
                startActivity(intent);
            }
        });

        return rootView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionBarSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActivity().getActionBar();
            ab.setTitle("Spotify Streamer");
            ab.setSubtitle(selectName);
        }
    }

    boolean isNetworkConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        TextView txtSubTitle;
    }

    //Adapter
    public class CustomTracksList extends ArrayAdapter<TrackList> {

        public CustomTracksList() {
            super(getActivity(), R.layout.list_item_track);
        }

        @Override
        public int getCount() {
            return mTracks.size();
        }

        @Override
        public TrackList getItem(int position) {
            return mTracks.get(position);
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (rowView == null) {
                // inflate the ListView item layout
                LayoutInflater inflater = LayoutInflater.from(getContext());
                rowView = inflater.inflate(R.layout.list_item_track, parent, false);

                // initialize the view holder
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.list_pic_imageview);
                viewHolder.txtTitle = (TextView) rowView.findViewById(R.id.list_track_textview);
                viewHolder.txtSubTitle = (TextView) rowView.findViewById(R.id.list_album_textview);
                rowView.setTag(viewHolder);
            } else {
                // recycle the already inflated view
                viewHolder = (ViewHolder) rowView.getTag();
            }

            TrackList trackList = getItem(position);
            Picasso.with(getActivity()).load(trackList.getPic()).into(viewHolder.imageView);
            viewHolder.txtTitle.setText(trackList.getTrack());
            viewHolder.txtSubTitle.setText(trackList.getAlbum());

            return rowView;
        }
    }

    public class FetchTrackTask extends AsyncTask<String, Void, MultipleContent> {

        //        private final String LOG_TAG = FetchTrackTask.class.getSimpleName();
        private String searchIDTemp;

        public FetchTrackTask(String searchID) {
            searchIDTemp = searchID;
        }

        @Override
        protected MultipleContent doInBackground(String... params) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String country = prefs.getString(getString(R.string.pref_country_key), getString(R.string.pref_country_default));

            HashMap map = new HashMap();
            map.put("country", country);

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            Tracks results = null;
            try {
                results = spotify.getArtistTopTrack(searchIDTemp, map);
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                // handle error
                Log.d("ArtistTopTrack failure", spotifyError.toString());
            }

            //Check null
            if (results.tracks.size() == 0) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Tracks not found! Please refine your search :D", Toast.LENGTH_SHORT).show();
                    }
                });
                return null;
            } else {
                trackNameTemp = new String[results.tracks.size()];
                albumNameTemp = new String[results.tracks.size()];
                albumPicTemp = new String[results.tracks.size()];
                trackPreviewURL = new String[results.tracks.size()];

                for (int i = 0; i < results.tracks.size(); i = i + 1) {
                    trackNameTemp[i] = results.tracks.get(i).name;
                    albumNameTemp[i] = results.tracks.get(i).album.name;

                    //Use this in Stage 2.
                    trackPreviewURL[i] = results.tracks.get(i).preview_url;

                    //Check tracks pic available.
                    if (results.tracks.get(i).album.images.size() != 0) {
                        //If yes, then show the first one.
                        albumPicTemp[i] = results.tracks.get(i).album.images.get(0).url;
                    } else {
                        //If not, show not found(404).
                        albumPicTemp[i] = "http://dev-cms.puttiapps.com/images/404-not-found.gif";
                    }
                }

                return new MultipleContent(trackNameTemp, albumNameTemp, albumPicTemp);
            }
        }

        @Override
        protected void onPostExecute(MultipleContent Content) {
            if (Content != null) {

                track = new String[trackNameTemp.length];
                album = new String[albumNameTemp.length];
                pic = new String[albumPicTemp.length];

                //All in one for loop because the length of three string array are equal.
                for (int i = 0; i < trackNameTemp.length; i = i + 1) {
                    track[i] = Content.track[i];
                    album[i] = Content.album[i];
                    pic[i] = Content.pic[i];
                }

                for (int i = 0; i < track.length; i = i + 1) {
                    mTracks.add(new TrackList(track[i], album[i], pic[i]));
                }

                tracksAdapter = new CustomTracksList();
                tracksList.setAdapter(tracksAdapter);
                bitmapThread.start();
            }
        }
    }

    public class MultipleContent {
        private String[] track, album, pic;

        public MultipleContent(String[] trackTemp, String[] albumTemp, String[] picTemp) {
            track = trackTemp;
            album = albumTemp;
            pic = picTemp;
        }
    }
}