package com.kermitlin.spotifystreamer;

import android.app.Application;
import android.graphics.Bitmap;

public class GlobalVariable extends Application {
    public String artistName = "";
    public String[] albumName = new String[10];
    public String[] albumPic = new String[10];
    public String[] trackName = new String[10];
    public String[] trackPreviewURL = new String[10];
    public int positionNow = 0;
    public int positionPre = 11;
    public Bitmap[] bitmapArt = new Bitmap[10];
    public Boolean noti_option = true;
}
