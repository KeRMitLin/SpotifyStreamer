package com.kermitlin.spotifystreamer;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private String[] name = {}, pic = {}, artistNameTemp, artistPicTemp, artistIDTemp;
    private CustomArtistsList artistsAdapter;
    private ListView artistsList;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    private static final String SEARCHED_KEY = "searched_Before";
    private EditText searchEditText;
    private String searchTextBefore = "", searchTextNow = "";

    public interface Callback {
        public void onItemSelected(Bundle toptrackExtra);
    }

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
            outState.putString(SEARCHED_KEY, searchTextBefore);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent();
        intent.setClass(getActivity(), PlayerService.class);
        getActivity().stopService(intent);
        super.onDestroy();
    }

    private void updateArtist(String str) {

        if (isNetworkConnectionAvailable()) {
            FetchArtistTask artistTask = new FetchArtistTask(str);
            artistTask.execute();
        } else {
            Toast.makeText(getActivity(), "No network connection!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        searchEditText = (EditText) rootView.findViewById(R.id.search_artist_edittext);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                searchTextNow = searchEditText.getText().toString();
                if (!searchTextBefore.equals(searchTextNow)) {
                    mPosition = ListView.INVALID_POSITION;
                }

                if (!"".equals(searchEditText.getText().toString())) {
                    updateArtist(searchEditText.getText().toString());
                }

                searchTextBefore = searchTextNow;
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        artistsList = (ListView) rootView.findViewById(R.id.list_artist_listview);
        artistsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                String selectArtistName = artistNameTemp[position];
                String selectArtistID = artistIDTemp[position];

                Bundle extras = new Bundle();
                extras.putString("EXTRA_NAME", selectArtistName);
                extras.putString("EXTRA_ID", selectArtistID);

                ((Callback) getActivity()).onItemSelected(extras);

                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
            searchTextBefore = savedInstanceState.getString(SEARCHED_KEY);
        }

        return rootView;
    }

    //Adapter
    public class CustomArtistsList extends ArrayAdapter<String> {

        private final String[] artist;
        private final String[] image;

        public CustomArtistsList(String[] artist, String[] image) {
            super(getActivity(), R.layout.list_item_artist, artist);
            this.artist = artist;
            this.image = image;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (rowView == null) {
                // inflate the ListView item layout
                LayoutInflater inflater = getActivity().getLayoutInflater();
                rowView = inflater.inflate(R.layout.list_item_artist, null, true);

                // initialize the view holder
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.list_item_imageview);
                viewHolder.txtTitle = (TextView) rowView.findViewById(R.id.list_item_textview);
                rowView.setTag(viewHolder);
            } else {
                // recycle the already inflated view
                viewHolder = (ViewHolder) rowView.getTag();
            }

            Picasso.with(getActivity()).load(image[position]).into(viewHolder.imageView);
            viewHolder.txtTitle.setText(artist[position]);

            return rowView;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
    }

    public class FetchArtistTask extends AsyncTask<String, Void, MultipleContent> {

        private String searchTextTemp;

        public FetchArtistTask(String searchText) {
            searchTextTemp = searchText;
        }

        @Override
        protected MultipleContent doInBackground(String... params) {

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            ArtistsPager results = null;
            try {
                results = spotify.searchArtists(searchTextTemp);
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
            }

            //Check null
            if (results.artists.total == 0) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Artists not found! Please refine your search :D", Toast.LENGTH_SHORT).show();
                    }
                });
                return null;
            } else if (results.artists.total < 20) {
                //parameter: limit. The maximum number of objects to return. Default: 20.
                artistNameTemp = new String[results.artists.total];
                artistPicTemp = new String[results.artists.total];
                artistIDTemp = new String[results.artists.total];

                for (int i = 0; i < results.artists.total; i = i + 1) {
                    artistNameTemp[i] = results.artists.items.get(i).name;

                    //Check artists pic available.
                    if (results.artists.items.get(i).images.size() != 0) {
                        //If yes, then show the first one.
                        artistPicTemp[i] = results.artists.items.get(i).images.get(0).url;
                    } else {
                        //If not, show not found(404).
                        artistPicTemp[i] = "http://dev-cms.puttiapps.com/images/404-not-found.gif";
                    }

                    artistIDTemp[i] = results.artists.items.get(i).id;
                }

                return new MultipleContent(artistNameTemp, artistPicTemp);
            } else {
                //parameter: limit. The maximum number of objects to return. Default: 20.
                artistNameTemp = new String[20];
                artistPicTemp = new String[20];
                artistIDTemp = new String[20];

                for (int i = 0; i < 20; i = i + 1) {
                    artistNameTemp[i] = results.artists.items.get(i).name;

                    if (results.artists.items.get(i).images.size() != 0) {
                        artistPicTemp[i] = results.artists.items.get(i).images.get(0).url;
                    } else {
                        artistPicTemp[i] = "http://dev-cms.puttiapps.com/images/404-not-found.gif";
                    }

                    artistIDTemp[i] = results.artists.items.get(i).id;
                }

                return new MultipleContent(artistNameTemp, artistPicTemp);
            }
        }

        @Override
        protected void onPostExecute(MultipleContent Content) {
            if (Content != null) {
                name = new String[artistNameTemp.length];
                pic = new String[artistPicTemp.length];

                //All in one for loop because the length of two string array are equal.
                for (int i = 0; i < artistNameTemp.length; i = i + 1) {
                    name[i] = Content.name[i];
                    pic[i] = Content.pic[i];
                }

                artistsAdapter = new CustomArtistsList(name, pic);
                artistsList.setAdapter(artistsAdapter);
                changeListPosition();
            }
        }
    }

    public void changeListPosition() {
        //Wait until artistsList complete populate.
        artistsList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                artistsList.removeOnLayoutChangeListener(this);
                if (mPosition != ListView.INVALID_POSITION) {
                    artistsList.smoothScrollToPosition(mPosition);
                    artistsList.setItemChecked(mPosition, true);
                }
            }
        });
        artistsAdapter.notifyDataSetChanged();
    }

    public class MultipleContent {
        private String[] name, pic;

        public MultipleContent(String[] nameTemp, String[] picTemp) {
            name = nameTemp;
            pic = picTemp;
        }
    }

    boolean isNetworkConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }


}