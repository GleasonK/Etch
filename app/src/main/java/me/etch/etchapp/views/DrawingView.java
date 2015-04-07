package me.etch.etchapp.views;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Spinner;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import me.etch.etchapp.R;
import me.etch.etchapp.adt.DrawPoint;
import me.etch.etchapp.util.Config;

/**
 * Created by GleasonK on 1/25/15.
 */
public class DrawingView extends View {
    public Pubnub pubnub;

    private Map<String,Integer> remoteUsers;
    private Path remotePath;
    private Paint remotePaint;

    private List<Path> remotePaths;
    private List<Paint> remotePaints;

    private List<DrawPoint> publishPoints;
    private Set<Integer> usedColors;

    String AUTH_KEY;
    String UUID;

    public static final int DRAW_TIME = 3;
    private Path drawPath;
    private Paint drawPaint, eraserPaint, canvasPaint;
    private int drawColor, colorID;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    SharedPreferences mSharedPrefs;


    private float brushSize;

    public static int[] colors = {
            R.color.blue,
            R.color.green,
            R.color.orange,
            R.color.pink,
            R.color.red,
            R.color.yellow,
            R.color.white
    };

    public DrawingView(Context context){
        super(context);
        setupDrawing();
        setupErasing();
        initPubNub();
        setupRemoteDrawing();
        subscribe();
    }

    public DrawingView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setupDrawing();
        setupErasing();
        initPubNub();
        setupRemoteDrawing();
        subscribe();
    }

    private void setupDrawing(){
        this.usedColors = new HashSet<Integer>();
        this.publishPoints = new ArrayList<DrawPoint>();
        mSharedPrefs = getContext().getSharedPreferences(Config.APP_SP_ID, Context.MODE_PRIVATE);

        drawPath = new Path();
        drawPaint = new Paint();
        colorID = mSharedPrefs.getInt(Config.MY_COLOR,0);
        drawColor = getResources().getColor(colors[colorID]);
        this.usedColors.add(colorID);
        drawPaint.setColor(drawColor);
        setBrushSize(10);
        drawPaint.setStrokeWidth(this.brushSize);

        // Initial path properties
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        //Makes it appear smoother
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void notifyColorChanged(){
        this.usedColors.remove(colorID);
        this.colorID=mSharedPrefs.getInt(Config.MY_COLOR,0);
        this.usedColors.add(colorID);
        this.drawColor = getResources().getColor(colors[colorID]);
        this.drawPaint.setColor(drawColor);
    }

    private void setupRemoteDrawing(){
        remoteUsers = new HashMap<String, Integer>();
        remotePaths = new ArrayList<Path>();
        remotePaints = new ArrayList<Paint>();

        remotePath = new Path();
        // Draw paint will hold the color.
        remotePaint = new Paint();
        int remoteColor = chooseColor();
        remotePaint.setColor(remoteColor);
        remotePaint.setStrokeWidth(this.brushSize);
        // Initial path properties
        remotePaint.setAntiAlias(true);
        remotePaint.setStyle(Paint.Style.STROKE);
        remotePaint.setStrokeJoin(Paint.Join.ROUND);
        remotePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private void setupRemoteUser(String UUID){
        Path remotePath = new Path();
        // Draw paint will hold the color.
        Paint remotePaint = new Paint();
        int remoteColor = chooseColor();
        remotePaint.setColor(remoteColor);
        remotePaint.setStrokeWidth(this.brushSize);
        // Initial path properties
        remotePaint.setAntiAlias(true);
        remotePaint.setStyle(Paint.Style.STROKE);
        remotePaint.setStrokeJoin(Paint.Join.ROUND);
        remotePaint.setStrokeCap(Paint.Cap.ROUND);
        int id = this.remoteUsers.size();
        this.remoteUsers.put(UUID,id);
        this.remotePaints.add(id, remotePaint);
        this.remotePaths.add(id, remotePath);
    }

    private void setupErasing(){
        eraserPaint = new Paint();
        eraserPaint.setColor(Color.TRANSPARENT);

        eraserPaint.setStrokeWidth(this.brushSize + 10);
        // Initial path properties

        eraserPaint.setAntiAlias(true);
        eraserPaint.setStyle(Paint.Style.STROKE);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
        eraserPaint.setStrokeCap(Paint.Cap.ROUND);
        //Makes it appear smoother
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH){
        super.onSizeChanged(w, h, oldW, oldH);
        canvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas){
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
        for (int i=0; i<this.remotePaths.size(); i++){
            canvas.drawPath(this.remotePaths.get(i), this.remotePaints.get(i));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                publishWithThreshold(touchX, touchY, Config.PUB_ACTION_DOWN);
                drawPath.moveTo(touchX, touchY);
                drawPath.lineTo(touchX+0.01f,touchY+0.01f);
                break;
            case MotionEvent.ACTION_MOVE:
                publishWithThreshold(touchX, touchY, Config.PUB_ACTION_MOVE);
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                publishWithThreshold(touchX, touchY, Config.PUB_ACTION_UP);
                drawCanvas.drawPath(drawPath, drawPaint);
                if (mSharedPrefs.getInt(Config.TIME_SELECTOR_INDEX, 1) != 5) {
                    fadeOut(drawPath); //TODO Handler to delete.
                }
                drawPath = new Path();
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();  //TODO: Will have to invalidate after push notification
        return true;
    }

    public void setBrushSize(float newSize){
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, getResources().getDisplayMetrics());
        this.brushSize = pixelAmount;
    }

    public void fadeOut(final Path path){
        Thread fadeRunner = new Thread(){

            @Override
            public void run() {
                int i = 0;
                try {
                    long drawTime = mSharedPrefs.getInt(Config.TIME_SELECTOR_VALUE, DRAW_TIME) * 1000;
                    //Log.d("DV-dT", "DrawTime: " + drawTime);
                    Thread.sleep(drawTime);
                } catch ( InterruptedException e) { e.printStackTrace(); }

                while (i < 100){
                    if (doFade(path,i) >= 40)
                        break;
                    i++;
                    try { Thread.sleep(20); } catch ( InterruptedException e) { e.printStackTrace(); }
                }
                doRemoval(path);
            }
        };
        fadeRunner.start();
    }

    public int doFade(Path path, int iter){
        //Removal
        int alpha = iter;
        eraserPaint.setAlpha(alpha);
        drawCanvas.drawPath(path, eraserPaint);
        postInvalidate();
        return alpha;
    }

    public void doRemoval(Path path){
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        //Removal
        eraserPaint.setColor(Color.TRANSPARENT);
        drawCanvas.drawPath(path, eraserPaint);

        //Cleanup
        eraserPaint.setXfermode(null);
        ColorDrawable cd = (ColorDrawable) getBackground();
        eraserPaint.setColor(cd.getColor());

        postInvalidate();
    }

    public JSONArray jsonArrayPoints() throws JSONException{
        JSONArray jArray = new JSONArray();
        for(DrawPoint dp : this.publishPoints){
            jArray.put(dp.toString());
        }
        return jArray;
    }

    public void initPubNub(){
        this.pubnub = new Pubnub(
                Config.PUBLISH_KEY,
                Config.SUBSCRIBE_KEY,
                Config.SECRET_KEY,
                Config.CIPHER_KEY,
                Config.SSL
        );
        this.pubnub.setCacheBusting(false);
        this.pubnub.setOrigin(Config.ORIGIN);
        this.pubnub.setAuthKey(AUTH_KEY);
        setHeartbeat(10);
        this.UUID = this.pubnub.getUUID();
    }

    public void setHeartbeat(int hb){
        pubnub.setHeartbeat(hb);
    }

    public void publishWithThreshold(float touchX, float touchY, String action){
        if (action.equals(Config.PUB_ACTION_DOWN)){
            this.publishPoints.add(new DrawPoint(touchX,touchY));
            _publishWithThreshold(action);
        }
        else if (action.equals(Config.PUB_ACTION_MOVE)){
            this.publishPoints.add(new DrawPoint(touchX, touchY));
            if (publishPoints.size() > Config.PUB_THRESHOLD)
                _publishWithThreshold(action);
        }
        else if (action.equals(Config.PUB_ACTION_UP)){
            if (publishPoints.size() > 0)
                _publishWithThreshold(Config.PUB_ACTION_MOVE); //Clear moving data.
            this.publishPoints.add(new DrawPoint(touchX, touchY));
            _publishWithThreshold(action);
        }
    }

    private void _publishWithThreshold(String action){
        JSONObject js = new JSONObject();
        try {
            //System.out.println(jsonArrayPoints());
            js.put("pathCoords", jsonArrayPoints());
            js.put("colorID", mSharedPrefs.getInt(Config.MY_COLOR,0));
            js.put("action", action);
            js.put("UUID",this.UUID);
        } catch (JSONException e) { e.printStackTrace(); }

        Callback publishCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                notifyUser("PUBLISH : " + message);
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                notifyUser("PUBLISH : " + error);
            }
        };
        String channel = mSharedPrefs.getString(Config.CHAT_ROOM, getContext().getString(R.string.global_chat_1));
        pubnub.publish(channel, js, publishCallback);

        this.publishPoints.clear();
    }

    public void publishLine(DrawPoint from, DrawPoint to){
        drawPath.moveTo(from.getX(), from.getY());
        drawPath.lineTo(to.getX(), to.getY());
        publishWithThreshold(from.getX(), from.getY(), Config.PUB_ACTION_DOWN);
        publishWithThreshold(to.getX(), to.getY(), Config.PUB_ACTION_MOVE);
        publishWithThreshold(to.getX(), to.getY(), Config.PUB_ACTION_UP);
        postInvalidate();
    }

    public void publish(Object message, String action){
        JSONObject js = new JSONObject();
        try {
            js.put("message", message);
            js.put("UUID",this.UUID);
            js.put("action", action);
        } catch (JSONException e) { e.printStackTrace(); }

        Callback publishCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                notifyUser("PUBLISH : " + message);
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                notifyUser("PUBLISH : " + error);
            }
        };
        String channel = mSharedPrefs.getString(Config.CHAT_ROOM, getContext().getString(R.string.global_chat_1));
        pubnub.publish(channel, js, publishCallback);
    }


    public void subscribe(){
        try {
            String channel = mSharedPrefs.getString(Config.CHAT_ROOM, getContext().getString(R.string.global_chat_1));
            pubnub.subscribe(channel, new Callback() {
                @Override
                public void connectCallback(String channel, Object message) {
                    notifyUser("SUBSCRIBE : CONNECT on channel:"
                            + channel + " : " + message.getClass() + " : " + message.toString());
                }

                @Override
                public void disconnectCallback(String channel, Object message) {
                    notifyUser("SUBSCRIBE : DISCONNECT on channel:"
                            + channel + " : " + message.getClass() + " : " + message.toString());
                }

                @Override
                public void reconnectCallback(String channel, Object message) {
                    notifyUser("SUBSCRIBE : RECONNECT on channel:"
                            + channel + " : " + message.getClass() + " : " + message.toString());
                }

                @Override
                public void successCallback(String channel, Object message) {
                    notifyUser("SUBSCRIBE : " + channel + " : " + message.getClass() + " : " + message.toString());
                    dispatchPubNubMessage(message);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    notifyUser("SUBSCRIBE : ERROR on channel " + channel + " : " + error.toString());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispatchPubNubMessage(Object message) {
        try {
            if (message instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) message;
                String otherUUID = jsonObject.getString("UUID");
                String action = jsonObject.getString("action");
                if (!this.UUID.equals(otherUUID)) {
                    if (!this.remoteUsers.containsKey(otherUUID)) {
                        setupRemoteUser(otherUUID);
                    }
                    int id = this.remoteUsers.get(otherUUID);

                    if (action.equals(Config.PUB_ACTION_CLEAR)) {
                        doClear();
                        return;
                    }
                    if (action.equals(Config.PUB_ACTION_TIMER)){
                        JSONObject js = new JSONObject(jsonObject.getString("message"));
                        doUpdateTimer(js.getInt("index"), js.getInt("value"));
                        return;
                    }
                    JSONArray pointArray = jsonObject.getJSONArray("pathCoords");
                    int colorID = jsonObject.getInt("colorID");
                    for (int i = 0; i < pointArray.length(); i++) {
                        String s = pointArray.getString(i);
                        String[] pts = s.split(":");
                        float xCoord = Float.parseFloat(pts[0]);
                        float yCoord = Float.parseFloat(pts[1]);
                        dispatchDrawPath(xCoord, yCoord, action, colorID, id);
                    }

                }
            } else if (message instanceof String) {
                String stringMessage = (String) message;
            } else if (message instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) message;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispatchDrawPath(float touchX, float touchY, String action, int colorID, int id){
        Path remotePath = this.remotePaths.get(id);
        Paint remotePaint = this.remotePaints.get(id);
        remotePaint.setColor(getResources().getColor(colors[colorID]));
        if (action.equals(Config.PUB_ACTION_DOWN)){
            remotePath.moveTo(touchX, touchY);
            remotePath.lineTo(touchX+0.1f,touchY+0.1f);
        }
        else if (action.equals(Config.PUB_ACTION_MOVE)){
            remotePath.lineTo(touchX,touchY);
        }
        else if (action.equals(Config.PUB_ACTION_UP)){
            drawCanvas.drawPath(remotePath, remotePaint);
            fadeOut(remotePath);
            remotePath = new Path();
            remotePath.reset();
            this.remotePaths.remove(id);
            this.remotePaths.add(id, remotePath);
        }
        postInvalidate();
    }

    private int chooseColor(){
        Random r = new Random();
        int colorID = (r.nextInt(this.colors.length-1)+1 % this.colors.length);
        while (this.usedColors.size() < this.colors.length && this.usedColors.contains(colorID)){
            colorID=(colorID+1)%this.colors.length;
        }
        return getResources().getColor(colors[colorID]);
    }

    private void notifyUser(Object message) {
        try {
            if (message instanceof JSONObject) {
                final JSONObject obj = (JSONObject) message;
                //Log.i("Received msg : ", String.valueOf(obj));
            }
            else if (message instanceof String) {
                final String obj = (String) message;
                //Log.i("Received msg : ", obj.toString());
            }
            else if (message instanceof JSONArray) {
                final JSONArray obj = (JSONArray) message;
                //Log.i("Received msg : ", obj.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setChatRoom(String chatRoom){
        String channel = mSharedPrefs.getString(Config.CHAT_ROOM, getContext().getString(R.string.global_chat_1));
        SharedPreferences.Editor edit = mSharedPrefs.edit();
        edit.putString(Config.CHAT_ROOM, chatRoom);
        edit.apply();
        pubnub.unsubscribe(channel);
        subscribe();
    }

    public void clearCanvas(){
        publish("clear", Config.PUB_ACTION_CLEAR);
        doClear();
    }

    private void doClear(){
        drawCanvas.drawColor(Color.BLACK);
        postInvalidate();
    }

    public void updateTimer(int index, int value){
        try {
            JSONObject js = new JSONObject();
            js.put("index", index);
            js.put("value", value);
            publish(js.toString(), Config.PUB_ACTION_TIMER);
        } catch (JSONException e){ e.printStackTrace(); }
    }
    private void doUpdateTimer(final int index, int value){
        SharedPreferences.Editor edit = mSharedPrefs.edit();
        edit.putInt(Config.TIME_SELECTOR_INDEX,index);
        edit.putInt(Config.TIME_SELECTOR_VALUE,value);
        edit.apply();
        final ViewGroup parent = (ViewGroup) this.getParent();

        Activity activity = (Activity) getContext();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Spinner mTimeSpinner = (Spinner) parent.findViewById(R.id.times_spinner);
                mTimeSpinner.setSelection(index);
            }
        });
    }

    public void makeTicTacToe(){
        clearCanvas();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = getWidth();
        int height = getHeight();
        publishLine(new DrawPoint(width/3, height/10), new DrawPoint(width/3, (height/10)*9));        //Left Vert
        publishLine(new DrawPoint(2*(width/3), height/10), new DrawPoint(2*(width/3), (height/10)*9));//Right Vert
        publishLine(new DrawPoint(width/10, height/3), new DrawPoint(9*(width/10), height/3));        //Top Horiz
        publishLine(new DrawPoint(width / 10, 2 * (height / 3)), new DrawPoint(9 * (width / 10), 2 * (height / 3)));//Bottom Horiz
    }


}
