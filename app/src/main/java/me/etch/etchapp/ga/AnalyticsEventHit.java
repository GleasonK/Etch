package me.etch.etchapp.ga;

import android.app.Application;
import android.os.AsyncTask;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import me.etch.etchapp.MyApplication;


/**
 * Created by GleasonK on 1/2/15.
 */
public class AnalyticsEventHit extends AsyncTask<Void, Void, Boolean> {
    private Application mApp;
    private String category, action, label;
    private Long id;


    public AnalyticsEventHit(Application app, String category, String action, String label, Long id){
        this.mApp = app;
        this.category=category;
        this.action=action;
        this.label=label;
        this.id=id;
    }

    @Override
    public Boolean doInBackground(Void... params){
        // Analytics - Get tracker. > Set Screenname > Send Screen View
        Tracker t = ((MyApplication) mApp).getTracker(MyApplication.TrackerName.APP_TRACKER);

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(id)
                .build());
        return true;
    }

}
