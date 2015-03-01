package com.hackillinois2015.rumor;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLES31Ext;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

/**
 * Created by Mark on 2/28/2015.
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {


    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** Store our model data in a float buffer. */
    private FloatBuffer mapTrigBuf;
    private FloatBuffer mapUVBuf;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    private int mBTextureHandle;

    private int mTextureSamplerHandle;

    private int mMapUVHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** How many elements per vertex. */
    private final int mStrideBytes = 7 * mBytesPerFloat;

    /** Offset of the position data. */
    private final int mPositionOffset = 0;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Offset of the color data. */
    private final int mColorOffset = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to gray.
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.

                        + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
                        + "attribute vec2 mapUV;             \n"
                        + "varying vec2 v_UV;             \n"

                        + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
                        + "varying vec4 orig_Position;    \n"

                        + "void main()                    \n"		// The entry point for our vertex shader.
                        + "{                              \n"
                        + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader.
                        + "   v_UV = mapUV;               \n"		// Pass the color through to the fragment shader.
                        // It will be interpolated across the triangle.
                        + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
                        + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                        + "}                              \n";    // normalized screen coordinates.

        final String fragmentShader =
                "precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a
                        // precision in the fragment shader.
                        + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                        + "varying vec4 orig_Position;    \n"
                        + "varying vec2 v_UV;             \n"
                        + "uniform float bTexture;"
                        + "uniform sampler2D myTextureSampler; \n"
                        // triangle per fragment.
                        + "void main()                    \n"		// The entry point for our fragment shader.
                        + "{                              \n"
                        + "   if(bTexture>0.5) gl_FragColor = texture2D(myTextureSampler,vec2(v_UV[0], 1.0-v_UV[1])).rgba;\n"
                        + "   else gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.
                        + "}                              \n";

        // Load in the vertex shader.
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
        mMapUVHandle = GLES20.glGetAttribLocation(programHandle, "mapUV");
        mBTextureHandle = GLES20.glGetUniformLocation(programHandle, "bTexture");
        mTextureSamplerHandle = GLES20.glGetUniformLocation(programHandle, "myTextureSampler");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle);
    }

    float YMAX = 1f , YMIN = 1f;
    int ViewPortWidth, ViewPortHeight;
    float XMAX() {
        return YMAX*ViewPortWidth/(float)ViewPortHeight;
    }
    float XMIN() {
        return YMIN*ViewPortWidth/(float)ViewPortHeight;
    }
    final float HexagonSideInPixel = 94;
    float getHexagonSideInGLUnit() {
        return 1.0f * HexagonSideInPixel/(float)ORIGINAL_MAP_HEIGHT;        //or: XMAX()*HexagonSideInPixel/(float)ORIGINAL_MAP_WIDTH
    }

    final static float fRoot3 = (float)Math.sqrt(3);
    final static float fHalfRoot3 = (float)Math.sqrt(3)/2;
    final int ORIGINAL_MAP_HEIGHT = 4096;
    final int ORIGINAL_MAP_WIDTH = 8192;

    float [][] unitHexagon = null;
    FloatBuffer[] unitHexagonBuffs = null;

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, ViewPortWidth = width, ViewPortHeight = height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        //Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        float u = getHexagonSideInGLUnit();
        numHexagonY = ORIGINAL_MAP_HEIGHT/94/3*2 + 1;
        numHexagonX = (int)(ORIGINAL_MAP_WIDTH/94/dRootThree) + 1;
        grids = new HexGroup(numHexagonX ,numHexagonY);
    }

    HexGroup grids = null;

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable (GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        GLES20.glUniform1f(mBTextureHandle, 0.1f);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, 2, 2, 2);
        Matrix.translateM(mModelMatrix, 0, -1, -0.5f, 0);
        Matrix.translateM(mModelMatrix, 0, -cameraX*XMAX()/ViewPortWidth, cameraY*YMAX/ViewPortHeight, 0);

        drawMap();

        drawManyTriangles(grids.info, grids.numOfTrigs);
    }

    Integer actualTextureHandle = null;

    private void drawManyTriangles(final FloatBuffer aTriangleBuffer, int size)
    {
        // Pass in the position information
        aTriangleBuffer.position(mPositionOffset);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        aTriangleBuffer.position(mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, size*3);
    }

    private void drawMap()
    {
        // Pass in the position information
        mapTrigBuf.position(mPositionOffset);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, mapTrigBuf);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        mapTrigBuf.position(mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, mapTrigBuf);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        mapUVBuf.position(0);
        GLES20.glVertexAttribPointer(mMapUVHandle, 2, GLES20.GL_FLOAT, false,
                0, mapUVBuf);
        GLES20.glEnableVertexAttribArray(mMapUVHandle);

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        if(actualTextureHandle == null) {
            if(map == null)
                map = loadDrawable(R.drawable.map, ViewPortWidth, ViewPortHeight);
            actualTextureHandle = loadGLTexture(map);
        }


        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, actualTextureHandle);
        GLES20.glUniform1i(mTextureSamplerHandle, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1f(mBTextureHandle, 0.9f);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisable(GLES20.GL_TEXTURE_2D);
        GLES20.glUniform1f(mBTextureHandle, 0.1f);
    }

    public static int loadGLTexture(Bitmap image) {
        int[] newTextureHandles = new int[1];
        GLES20.glGenTextures(1, newTextureHandles, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, newTextureHandles[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
        return newTextureHandles[0];
    }
    public static void deleteGLTexture(int textureHandle) {
        int[] handles = new int[]{textureHandle};
        GLES20.glDeleteTextures(1, handles, 0);
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------

    void initData(){

        // This triangle is red, green, and blue.

        final float mapMaxY = 1.0f;
        final float mapMaxX = mapMaxY/ORIGINAL_MAP_HEIGHT*ORIGINAL_MAP_WIDTH;

        final float[] mapTrig = {
                // X, Y, Z,
                // R, G, B, A
                mapMaxX, 0f, -0.5f,
                1.0f, 0.0f, 0.0f, 1.0f,

                mapMaxX, 1f, -0.5f,
                0.0f, 0.0f, 1.0f, 1.0f,

                0f, 1f, -0.5f,
                0.0f, 1.0f, 0.0f, 1.0f,

                0f, 0f, -0.5f,
                1.0f, 1.0f, 0.0f, 1.0f,

                mapMaxX, 0f, -0.5f,
                0.0f, 1.0f, 1.0f, 1.0f,

                0f, 1f, -0.5f,
                1.0f, 0.0f, 1.0f, 1.0f};

        final float[] fMapUV = {
                1f, 0f,
                1f, 1f,
                0f, 1f,
                0f, 0f,
                1f, 0f,
                0f, 1f
        } ;

        // Initialize the buffers.
        mapTrigBuf = ByteBuffer.allocateDirect(mapTrig.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mapUVBuf =  ByteBuffer.allocateDirect(fMapUV.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mapTrigBuf.put(mapTrig).position(0);
        mapUVBuf.put(fMapUV).position(0);
    }

    public GameView(Context context) {
        super(context);
        setEGLContextClientVersion( 2 );
        initData();
        setRenderer(this);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion( 2 );
        initData();
        setRenderer(this);
    }

    Random random =  new Random();

    Bitmap map = null;
    float cameraX=0, cameraY=0;

    double loadDrawable_xScale;
    double loadDrawable_yScale;
    int loadDrawable_width, loadDrawable_height;
    public android.graphics.Bitmap loadDrawable(int resID, int displayWidth, int displayHeight) {
        Resources res = getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //First, decode the image's header without loading its content
        BitmapFactory.decodeResource(res,resID,options);

        loadDrawable_width = options.outWidth;
        loadDrawable_height = options.outHeight;
        loadDrawable_xScale =  (double)options.outWidth/displayWidth;
        loadDrawable_yScale =  (double)options.outHeight/displayHeight;

        int inZoomRatio = 1;
        if(options.outHeight>displayHeight || options.outWidth>displayWidth)
            while ((options.outHeight/2/inZoomRatio)>displayHeight
                    &&(options.outWidth/2/inZoomRatio)>displayWidth)
                inZoomRatio*=2;     //inZoomRatio had better be a power of 2

        options.inSampleSize = inZoomRatio; //zoom out
        options.inJustDecodeBounds = false;

        Bitmap ans=BitmapFactory.decodeResource(res,resID,options);
        return ans;
    }

    /**
     * Draw the hexagons and the map.
     * The size of the hexagons should be large enough to be able to cut the map into 611 pieces..
     * @param canvas
     *
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(-cameraX,-cameraY);

        if(map==null)
            map=loadDrawable(R.drawable.map, getWidth(), getHeight());
        canvas.drawBitmap(map, 0, 0, new Paint());

        double xScale = loadDrawable_xScale;
        double yScale = loadDrawable_yScale;
        int MAP_HEIGHT = loadDrawable_height;
        int MAP_WIDTH = loadDrawable_width;

        if(xScale>yScale) dHexagonSide = 94/xScale;
        else dHexagonSide = 94/yScale;


        numHexagonY = MAP_HEIGHT/94/3*2 + 1;
        numHexagonX = (int)(MAP_WIDTH/94/dRootThree) + 1;
        if(isHexFilled == null)
        {
            try {
                ObjectInputStream in = new ObjectInputStream(getResources().getAssets().open("hasHex.map"));
                isHexFilled = (boolean[][])in.readObject();
            } catch (Exception err) {
                //throw (RuntimeException)(new RuntimeException("Cannot open Maps!").initCause(err));
            }
        }

        HexCoord test = new HexCoord();
        for(int i=-numHexagonY; i<numHexagonX; i++) for(int j=0; j<numHexagonY; j++) {
            test.update(i, j, 0, 0);
            test.drawXY(canvas, 0, 0, Color.argb(100, 0, 0, 0));
        }

    }*/


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


    boolean isHexFilled[][] = null;
    double dHexagonSide = 30;
    int numHexagonX, numHexagonY;
    double dHexagonUseful()
    { return dHexagonSide*dRootThree;}

    final static double d60_DEGREES = Math.PI/3;
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

            realX1 = (float)((x+y*Math.cos(d60_DEGREES))*dRootThree);
            realX3 = realX1 + (float)dRootThree/2;
            realX4 = realX1 + (float)dRootThree;
            realY1 = (float)(y*Math.sin(d60_DEGREES)*dRootThree);
            realY2 = realY1;
            realY3 = realY2+0.5f;
            realY6 = realY1-0.5f;
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

    HexCoord xyToHexagon(int x, int y)
    {
        HexCoord ans = new HexCoord();
        Integer[] approx = xyToUV(x,y);
        double dx = (approx[0]+approx[1]*Math.cos(d60_DEGREES))*dHexagonUseful();
        double dy = approx[1]*Math.sin(d60_DEGREES)*dHexagonUseful();
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

    Integer[] xyToUV(int x,int y)
    {
        double u=x-y/Math.tan(d60_DEGREES);
        double v=y/Math.sin(d60_DEGREES);
        return new Integer[]{(int)Math.floor(u/dHexagonUseful()), (int)Math.floor(v/dHexagonUseful())};
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
