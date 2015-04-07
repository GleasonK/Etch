package me.etch.etchapp.ga;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import me.etch.etchapp.MyApplication;


/**
 * Created by GleasonK on 1/2/15.
 */
public class AnalyticsHit extends AsyncTask<Void, Void, Boolean> {
    private Activity mActivity;
    private String screenName;

    public AnalyticsHit(Activity activity, String screenName){
        this.mActivity = activity;
        this.screenName = screenName;
    }

    @Override
    public Boolean doInBackground(Void... params){
        // Analytics - Get tracker. > Set Screenname > Send Screen View
        Tracker t = ((MyApplication) mActivity.getApplication()).getTracker(MyApplication.TrackerName.APP_TRACKER);
        t.setScreenName(this.screenName);
        t.send(new HitBuilders.AppViewBuilder().build());
        return true;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }
}
