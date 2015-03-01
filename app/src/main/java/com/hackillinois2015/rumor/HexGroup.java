package com.hackillinois2015.rumor;

import android.content.res.Resources;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Mark on 2/28/2015.
 */
public class HexGroup {
    public final int GroupMaxX;
    public final int GroupSizeX;        //Then there will be 30x30 hexagons in a group
    public final int GroupSizeY;        //Then there will be 30x30 hexagons in a group
    public final int numOfTrigs;

    public FloatBuffer info;
    public boolean[][] hasHex = null;

    public int XYtoIndex(int hexX, int hexY) {
        int start = GroupSizeY*GroupSizeY;
        return start+hexX*GroupSizeY+hexY;
    }

    public void setColor(int hexX, int hexY, float r, float g, float b, float a) {
        if(hexX<0||hexY>=GroupSizeY||hexX>=GroupMaxX)
            return;
        int index = XYtoIndex(hexX,hexY);
        final int howManyFloatsPerEntry = 4*3*7;
        int indexInBuffer = index*howManyFloatsPerEntry;
        for (int i=0; i<4; i++) // Traverse Triangles
        {
            for (int j=0; j<3; j++)  //Traverse Vertices
            {
                int k = indexInBuffer + i*3*7 + j* 7;
                info.put(k+3, r);
                info.put(k+4, g);
                info.put(k+5, b);
                info.put(k+6, a);
            }
        }
    }

    public HexGroup(Resources res, int sizeX, int sizeY) {
        GroupMaxX = sizeX;
        GroupSizeX = sizeX+sizeY;
        GroupSizeY = sizeY;
        numOfTrigs = GroupSizeX*GroupSizeY*4;
        try {
            ObjectInputStream in = new ObjectInputStream(res.getAssets().open("hasHex.map"));
            hasHex = (boolean[][]) in.readObject();
            in.close();
        } catch (Exception err) {
            throw (RuntimeException)(new RuntimeException("Map files is lost!").initCause(err));
        }
        GameView.HexCoord hex =new GameView.HexCoord();
        info  = ByteBuffer.allocateDirect(mBytesPerFloat*numOfTrigs*21).order(ByteOrder.nativeOrder()).asFloatBuffer();
        info.position(0);
        for(int i=-sizeY; i<sizeX; i++) for(int j=0; j<sizeY; j++) {
            hex.update(i,j,0,0);
            HexDrawInfo hexDrawInfo= new HexDrawInfo(hex,i>=0?!hasHex[i][j]:true);
            float[][] currInfo = hexDrawInfo.unitHexagon;
            safePut(info,currInfo[0]);
            safePut(info,currInfo[1]);
            safePut(info,currInfo[2]);
            safePut(info,currInfo[3]);
        }
        info.position(0);
    }

    static void safePut(FloatBuffer buffer, float[] arr) {
        for(int i=0; i<arr.length; i++) {
            buffer.put(arr[i]);
        }
    }

    final static int mBytesPerFloat = 4;
}

class HexDrawInfo {
    public float[][] unitHexagon = null;
    public HexDrawInfo() {}

    public HexDrawInfo(GameView.HexCoord hex, boolean hidden) {

        float u = getHexagonSideInGLUnit();
		float dx = u*hex.realX1;
		float dy = u*hex.realY1;
        unitHexagon = new float[4][21];
        setTrig(unitHexagon[0],0+dx,0+dy,0+dx,u+dy,fHalfRoot3*u+dx,1.5f*u+dy, 0.2f,0.2f,1,hidden?0:0.5f);
        setTrig(unitHexagon[1],0+dx,0+dy,fHalfRoot3*u+dx,1.5f*u+dy, fHalfRoot3*u+dx,-0.5f*u+dy, 0.2f,0.2f,1,hidden?0:0.5f);
        setTrig(unitHexagon[2],fHalfRoot3*u+dx,1.5f*u+dy, fHalfRoot3*u+dx,-0.5f*u+dy, fRoot3*u+dx, u+dy, 0.2f,0.2f,1,hidden?0:0.5f);
        setTrig(unitHexagon[3], fHalfRoot3*u+dx,-0.5f*u+dy, fRoot3*u+dx, u+dy, fRoot3*u+dx, 0+dy, 0.2f,0.2f,1,hidden?0:0.5f);
    }

    final static float HexagonSideInPixel = 94;
    final static float fRoot3 = (float)Math.sqrt(3);
    final static float fHalfRoot3 = (float)Math.sqrt(3)/2;
    final static int ORIGINAL_MAP_HEIGHT = 4096;
    final static int ORIGINAL_MAP_WIDTH = 8192;
    static float getHexagonSideInGLUnit() {
        return 1.0f * HexagonSideInPixel/(float)ORIGINAL_MAP_HEIGHT;        //or: XMAX()*HexagonSideInPixel/(float)ORIGINAL_MAP_WIDTH
    }
    private static void setTrig(float[] trig, float x1, float y1, float x2, float y2, float x3, float y3, float r,float g, float b, float a) {
        trig[0] = x1;
        trig[1] = 1-y1;
        trig[2] = 0;
        trig[3] = r;
        trig[4] = g;
        trig[5] = b;
        trig[6] = a;

        trig[0+7] = x2;
        trig[1+7] = 1-y2;
        trig[2+7] = 0;
        trig[3+7] = r;
        trig[4+7] = g;
        trig[5+7] = b;
        trig[6+7] = a;

        trig[0+7+7] = x3;
        trig[1+7+7] = 1-y3;
        trig[2+7+7] = 0;
        trig[3+7+7] = r;
        trig[4+7+7] = g;
        trig[5+7+7] = b;
        trig[6+7+7] = a;
    }

}
