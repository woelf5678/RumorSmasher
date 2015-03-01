package com.hackillinois2015.rumor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class GameActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //getActionBar().hide();
        setContentView(R.layout.activity_game);

        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        /*
        if(map==null)
            map=loadDrawable(R.drawable.map,screenSize.x,screenSize.y);
        ((ImageView)findViewById(R.id.mapImg)).setImageBitmap(map);
        ((RelativeLayout)findViewById(R.id.gameActivityLL)).bringChildToFront(findViewById(R.id.gameView));
        findViewById(R.id.gameActivityLL).invalidate();
        */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

/*
    Bitmap map = null;
    public android.graphics.Bitmap loadDrawable(int resID, int displayWidth, int displayHeight) {
        Resources res = getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //First, decode the image's header without loading its content
        BitmapFactory.decodeResource(res,resID,options);

        int inZoomRatio = 1;
        if(options.outHeight>displayHeight || options.outWidth>displayWidth)
            while ((options.outHeight/2/inZoomRatio)>displayHeight
                    &&(options.outWidth/2/inZoomRatio)>displayWidth)
                inZoomRatio*=2;     //inZoomRatio had better be a power of 2

        options.inSampleSize = inZoomRatio; //zoom out
        options.inJustDecodeBounds = false;

        Bitmap ans=BitmapFactory.decodeResource(res,resID,options);
        return ans;
    }*/
}
