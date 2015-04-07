package me.etch.etchapp.adt;

/**
 * Created by GleasonK on 1/30/15.
 */
public class DrawPoint {
    private float xCoord, yCoord;

    public DrawPoint(float xCoord, float yCoord){
        this.xCoord=xCoord;
        this.yCoord=yCoord;
    }

    public float getX() {
        return xCoord;
    }

    public float getY() {
        return yCoord;
    }

    public String toString(){
        return this.xCoord + ":" + this.yCoord;
    }
}
