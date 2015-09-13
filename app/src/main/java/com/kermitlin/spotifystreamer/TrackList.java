package com.kermitlin.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackList implements Parcelable {
    private final String track;
    private final String album;
    private final String pic;

    public TrackList(String track, String album, String image) {
        this.track = track;
        this.album = album;
        this.pic = image;
    }

    public String getTrack() {
        return track;
    }

    public String getAlbum() {
        return album;
    }

    public String getPic() {
        return pic;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.track);
        dest.writeString(this.album);
        dest.writeString(this.pic);
    }

    protected TrackList(Parcel in) {
        this.track = in.readString();
        this.album = in.readString();
        this.pic = in.readString();
    }

    public static final Creator<TrackList> CREATOR = new Creator<TrackList>() {
        public TrackList createFromParcel(Parcel source) {
            return new TrackList(source);
        }

        public TrackList[] newArray(int size) {
            return new TrackList[size];
        }
    };
}
