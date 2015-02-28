package com.hackillinois2015.rumor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

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


    final static double d60_DEGREES = Math.PI/3;
    final static double dHexagonSide = 10;
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

    static Integer[] xyToHexagon(int x, int y)
    {
        Integer[] approx = xyToUV(x,y);
        double dx = (approx[0]+approx[1]*Math.cos(d60_DEGREES))*dHexagonUseful;
        double dy = approx[1]*Math.sin(d60_DEGREES)*dHexagonUseful;
        double x2 = x-dx;
        double y2 = y-dy;
        if(y2<(-x2/dRootThree+2*dHexagonSide)&&x2<dRootThree*dHexagonSide)
            ;
        else
        {
            if(y2<=x2/dRootThree)
                approx[0]+=1;
            else approx[1]+=1;
        }
        return approx;
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
