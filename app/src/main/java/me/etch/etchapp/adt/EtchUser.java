package me.etch.etchapp.adt;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by GleasonK on 1/31/15.
 */
public class EtchUser {
    private Path path;
    private Paint paint;
    private int color;

    public EtchUser(Path path, Paint paint, int color){
        this.path=path;
        this.paint=paint;
        this.color=color;
    }

    public Path getPath() {
        return path;
    }

    public Paint getPaint() {
        return paint;
    }

    public int getColor() {
        return color;
    }
}
