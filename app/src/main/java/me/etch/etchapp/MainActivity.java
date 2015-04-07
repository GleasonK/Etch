package me.etch.etchapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import me.etch.etchapp.ga.AnalyticsHit;
import me.etch.etchapp.util.Config;
import me.etch.etchapp.views.DrawingView;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private DrawingView drawingView;
    private Spinner mTimeSpinner;
    private SharedPreferences mSharedPrefs;
    private BroadcastReceiver bReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AnalyticsHit.isOnline(this)) {
            new AnalyticsHit(this,"MainActivity").execute();
        }

        mSharedPrefs = getSharedPreferences(Config.APP_SP_ID, MODE_PRIVATE);
        SharedPreferences.Editor edit = mSharedPrefs.edit();
        if (!mSharedPrefs.contains(Config.MY_COLOR)) {
            edit.putInt(Config.MY_COLOR, 0);
        }
        if (!mSharedPrefs.contains(Config.TIME_SELECTOR_INDEX)) {
            edit.putInt(Config.TIME_SELECTOR_INDEX, 1);
        }
        if (!mSharedPrefs.contains(Config.TIME_SELECTOR_VALUE)) {
            edit.putInt(Config.TIME_SELECTOR_VALUE, 3);
        }
        if (!mSharedPrefs.contains(Config.CHAT_ROOM)) {
            edit.putString(Config.CHAT_ROOM, getString(R.string.global_chat_1));
        }
        edit.apply();
        setContentView(R.layout.activity_main);
        setupMenu(mSharedPrefs.getInt(Config.MY_COLOR, 0));
        setupSpinner(mSharedPrefs.getInt(Config.TIME_SELECTOR_INDEX, 1));
        DrawingView dv = (DrawingView) findViewById(R.id.drawing);
        this.drawingView = dv;
        drawingView.setChatRoom(mSharedPrefs.getString(Config.CHAT_ROOM, getString(R.string.global_chat_1)));
        registerTheReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_chat_room:
                changeChatRoom();
                return true;
            case R.id.action_invite_friend:
                smsInvite();
                return true;
            case R.id.action_join_group:
                joinGroupChat();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
//        AppEventsLogger.deactivateApp(this);
    }


    @Override
    protected void onStop()
    {
        unregisterReceiver(bReceiver);
        super.onStop();
    }

    @Override
    protected void onStart()
    {
        if (bReceiver!=null)
            registerReceiver(bReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        else
            registerTheReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Logs 'app deactivate' App Event.
//        AppEventsLogger.deactivateApp(this);
    }

    private void setupMenu(int colorID) {
        Button colorBtn = (Button) findViewById(R.id.etch_color_button);
        colorBtn.setTextColor(getResources().getColor(DrawingView.colors[colorID]));
    }

    private void setupSpinner(int index) {
        mTimeSpinner = (Spinner) findViewById(R.id.times_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.times_array, R.layout.spinner_layout);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeSpinner.setAdapter(adapter);
        mTimeSpinner.setOnItemSelectedListener(this);
        mTimeSpinner.setSelection(index);
    }

    public void dispatchColor(View view) {
        int numColors = 7;
        int colorID = mSharedPrefs.getInt(Config.MY_COLOR, 0);
        colorID = ++colorID % numColors;
        SharedPreferences.Editor edit = mSharedPrefs.edit();
        edit.putInt(Config.MY_COLOR, colorID);
        edit.apply();
        this.drawingView.notifyColorChanged();
        Button btn = (Button) view;
        btn.setTextColor(getResources().getColor(DrawingView.colors[colorID]));
    }

    /**
     * <item>1 second</item>
     * <item>3 seconds</item>
     * <item>5 seconds</item>
     * <item>10 seconds</item>
     * <item>60 seconds</item>
     * <item>Permanent Marker</item>
     *
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        SharedPreferences sp = getSharedPreferences(Config.APP_SP_ID, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        //Log.d("MA-spin", "Pos: " + pos + " Id: " + id);
        switch (pos) {
            case 0:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 0);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 1);
                break;
            case 1:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 1);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 3);
                break;
            case 2:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 2);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 5);
                break;
            case 3:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 3);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 10);
                break;
            case 4:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 4);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 60);
                break;
            case 5:
                edit.putInt(Config.TIME_SELECTOR_INDEX, 5);
                edit.putInt(Config.TIME_SELECTOR_VALUE, 2000);
                break;
        }
        edit.apply();
        int index = sp.getInt(Config.TIME_SELECTOR_INDEX, 1);
        int value = sp.getInt(Config.TIME_SELECTOR_VALUE, 3);
        drawingView.updateTimer(index, value);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void registerTheReceiver(){
        bReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                DrawingView dv = (DrawingView) findViewById(R.id.drawing);
                dv.pubnub.disconnectAndResubscribe();
            }
        };
        this.registerReceiver(bReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void clearCanvas(View view) {
        drawingView.clearCanvas();
    }

    public void dispatchGame(View view) {
        SharedPreferences sp = getSharedPreferences(Config.APP_SP_ID, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt(Config.TIME_SELECTOR_INDEX, 5);
        edit.putInt(Config.TIME_SELECTOR_VALUE, 2000);
        edit.apply();
        mTimeSpinner.setSelection(5);
        drawingView.updateTimer(5, 2000);
        drawingView.makeTicTacToe();
    }

    public void joinGroupChat(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.join_global_prompt));
        builder.setItems(R.array.global_chats, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] chatRooms = getResources().getStringArray(R.array.global_chats);
                drawingView.setChatRoom(chatRooms[which]);
            }
        });
        builder.show();
    }

    public void changeChatRoom(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.change_room_prompt));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String room = input.getText().toString();
                if (room.equals("")){
                    Toast.makeText(MainActivity.this, getString(R.string.empty_chat_error), Toast.LENGTH_SHORT).show();
                }
                else {
                    drawingView.setChatRoom(room);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void smsInvite(){ //http://play.google.com/store/apps/details?id=me.etch.etchapp
        String channel = mSharedPrefs.getString(Config.CHAT_ROOM, getString(R.string.global_chat_1));
        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        sendIntent.setData(Uri.parse("sms:"));
        sendIntent.putExtra("sms_body", getString(R.string.sms_invite, channel, getString(R.string.web_address)));
        startActivity(sendIntent);
    }
}
