package com.hackillinois2015.rumor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

/**
 * Created by Mark on 2/28/2015.
 */
public class GameView extends View {
    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    Random random =  new Random();

    float cameraX=0, cameraY=0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        HexCoord test = new HexCoord();
        for(int i=-3; i<3; i++) for(int j=-3; j<3; j++) {
            test.update(i, j, 0, 0);
            test.drawXY(canvas, cameraX, cameraY, Color.rgb(random.nextInt(255),random.nextInt(255),random.nextInt(255)));
        }
    }


    float downX;
    float downY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        boolean returnVal = false;
        String ans;
        HexCoord hex;
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                Log.e("action", "down");
                downX=event.getX();
                downY=event.getY();

                /*
                ans = "X="+event.getX()+"Y="+event.getY();
                hex = xyToHexagon((int)event.getX(),(int)event.getY());
                ans+=". HEX = ("+hex.x+","+hex.y+").";
                Toast.makeText(getContext(),ans, Toast.LENGTH_SHORT).show();*/

                returnVal=true;
                break;
            case MotionEvent.ACTION_UP: {
                Log.e("action", "up");

                float dx = event.getX() - downX;
                float dy = event.getY() - downY;

                cameraX -= dx;
                cameraY -= dy;

                postInvalidate();

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                Log.e("action", "move");

                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                downX = event.getX();
                downY = event.getY();

                cameraX -= dx;
                cameraY -= dy;

                postInvalidate();

                break;
            }
        }
        return returnVal;
    }

    final static double d60_DEGREES = Math.PI/3;
    final static double dHexagonSide = 250;
    final static double dHexagonUseful = dHexagonSide*Math.sqrt(3);
    final static double dRootThree = Math.sqrt(3);

    static <T> void print(T[] arr)
    {
        for(T x: arr)
        {
            System.out.print(x);
            System.out.print("  ");
        }
        System.out.print("\n");
    }

    static class HexCoord
    {
        public int x;
        public int y;
        public double dx;
        public double dy;

        float realX1, realX3, realX4;
        float realY1, realY2, realY3, realY6;

        public void update(int x, int y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;

            realX1 = (float)((x+y*Math.cos(d60_DEGREES))*dHexagonUseful);
            realX3 = realX1 + (float)dHexagonUseful/2;
            realX4 = realX1 + (float)dHexagonUseful;
            realY1 = (float)(y*Math.sin(d60_DEGREES)*dHexagonUseful);
            realY2 = realY1+(float)dHexagonSide;
            realY3 = realY2+(float)dHexagonSide/2;
            realY6 = realY1-(float)dHexagonSide/2;
        }

        public void drawXY(Canvas canvas, float camX, float camY, int color) {
            Paint linePaint = new Paint();
            linePaint.setColor(color);

            Path path = new Path();
            path.moveTo(realX1-camX, realY1-camY);
            path.lineTo(realX1-camX, realY2-camY);
            path.lineTo(realX3-camX, realY3-camY);
            path.lineTo(realX4-camX, realY2-camY);
            path.lineTo(realX4-camX, realY1-camY);
            path.lineTo( realX3-camX, realY6-camY);
            path.lineTo(realX1-camX, realY1-camY);
            canvas.drawPath(path, linePaint);
        }
    }

    static HexCoord xyToHexagon(int x, int y)
    {
        HexCoord ans = new HexCoord();
        Integer[] approx = xyToUV(x,y);
        double dx = (approx[0]+approx[1]*Math.cos(d60_DEGREES))*dHexagonUseful;
        double dy = approx[1]*Math.sin(d60_DEGREES)*dHexagonUseful;
        double x2 = x-dx;
        double y2 = y-dy;
        if(y2<(-x2/dRootThree+2*dHexagonSide)&&x2<dRootThree*dHexagonSide)
            ans.update(approx[0],approx[1],x2,y2);
        else
        {
            if(y2<=x2/dRootThree)
                ans.update(approx[0]+1,approx[1],x2,y2);
            else ans.update(approx[0],approx[1]+1,x2,y2);
        }
        return ans;
    }

    static Integer[] xyToUV(int x,int y)
    {
        double u=x-y/Math.tan(d60_DEGREES);
        double v=y/Math.sin(d60_DEGREES);
        return new Integer[]{(int)Math.floor(u/dHexagonUseful), (int)Math.floor(v/dHexagonUseful)};
    }

    static int floorDiv(int a, int b)
    {
        if(a%b==0)
            return a/b;
        boolean posA = a>0;
        boolean posB = b>0;
        if(posA==posB)
            return a/b;
        else return a/b-1;
    }
}
