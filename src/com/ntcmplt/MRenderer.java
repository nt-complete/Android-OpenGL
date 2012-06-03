package com.ntcmplt;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 4/10/12
 * Time: 7:54 PM
 */


public class MRenderer implements GLSurfaceView.Renderer {

    private int iters, pointCount, triangleCount;
    public float intervalNum = 2;
    public boolean updated = true;

    private FloatBuffer torusVB, normalVB, colorVB;
    private IntBuffer indicesVB;
    private final String vertexShaderStr =
            "attribute vec4 vPosition; \n " +
                    "attribute vec4 vColor;\n " +
                    "attribute vec3 vNormal;\n " +
                    "\n " +
                    "uniform mat4 uModelViewProjectionM; \n " +
                    "uniform mat4 uModelViewM; \n " +
                    "uniform vec3 uLightPosition;\n " +
                    "\n " +
                    "varying vec4 varColor;\n " +
                    "\n " +
                    "void main()  {\n " +
                    "	// For transferring the vertex position in eye-space\n " +
                    "	vec3 modelViewVertex = vec3(uModelViewM * vPosition);\n " +
                    "\n " +
                    "	// Obtains the normal\n " +
                    "	vec3 modelViewNormal = normalize(vec3(uModelViewM * vec4(vNormal, 0.0)));\n " +
                    "\n " +
                    "	float distance = length(uLightPosition - modelViewVertex);\n " +
                    "	vec3 lightVector = normalize(uLightPosition - modelViewVertex);\n " +
                    "\n " +
                    "	float LambertTerm = max(dot(modelViewNormal, lightVector), 0.0);\n " +
                    "	float diffuse = LambertTerm * (1.0 / (1.0 + (0.4 * distance * distance)));\n " +
                    " \n " +
                    "	 vec3 R = reflect(-lightVector, modelViewNormal); \n " +
                    "    vec3 vEye = vec3(0.0, 0.0, 1.0); \n " +
                    "	 float specular = 1.0 * 1.0 * pow(max(dot(R, vEye), 0.0), 1.0);  \n " +
                    "\n " +
                    "	//varColor = ((vColor * 0.2) + vColor * diffuse * specular) ;\n " +
                    "	varColor = (vColor * 0.2) + vColor * diffuse;\n " +
                    "	//varColor = vColor ;\n " +
                    "\n " +
                    "	gl_Position = uModelViewProjectionM * vPosition; \n " +
                    "\n " +
                    "\n " +
                    "} \n ";

    private final String fragmentShaderStr =
            "precision mediump float; \n" +
                    "varying vec4 varColor;\n" +
                    "void main() { \n" +
                    "  gl_FragColor = varColor; \n" +
                    "}  \n";

/*
    private final String vertexShaderStr =
                  "attribute vec4 vPosition;\n " +
"attribute vec4 vColor; \n " +
"attribute vec3 vNormal; \n " +
"\n " +
"uniform mat4 uModelViewProjectionM;\n " +
"uniform mat4 uModelViewM;\n " +
"uniform vec3 uLightPosition; \n " +
"\n " +
"varying vec4 varColor; \n " +
"\n " +
"void main()  { \n " +
"\n " +
"	// For transferring the vertex position in eye-space \n " +
"	vec3 modelViewVertex = vec3(uModelViewM * vPosition); \n " +
"\n " +
"	// Obtains the normal \n " +
"	vec3 modelViewNormal = vec3(uModelViewM * vec4(vNormal, 0.0)); \n " +
"\n " +
"	vec3 L = normalize(uLightPosition - modelViewVertex);\n " +
"\n " +
"	float distance = length(uLightPosition - modelViewVertex);\n " +
"	float LambertTerm = max(dot(modelViewNormal, L), 0.0); \n " +
"	float diffuse = LambertTerm * (1.0 / (1.0  (0.25 * distance * distance))); \n " +
"	varColor = vColor * diffuse ; \n " +
"\n " +
"	gl_Position = uModelViewProjectionM * vPosition;\n " +
"\n " +
"\n " +
"} ;\n ";
*/
    private Context context;
    private int mProgram;
    private int mPositionHandle, mMVMatrixHandle, mMVPMatrixHandle, mLightPosHandle, mColorHandle, mNormalHandle;
    private float[] mMVPMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float angle = 0.0f;
    private float zTrans = 0.0f;
    private long prevTime;
    private float frameCount = 0;

    public float width = 0;
    public float height = 0;
    public float horizontalAngle = 0.0f;
    public float verticalAngle = 0.0f;
    public boolean touched = false;
    public MotionEvent touch = null;



    public MRenderer(Context context) {
        this.context = context;
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        initShapes();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderStr);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderStr);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "in create. Error num: " + err);
        }
        GLES20.glGetShaderInfoLog(vertexShader);
        GLES20.glGetProgramInfoLog(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPosition");
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");

        prevTime = Calendar.getInstance().getTimeInMillis();
        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "end of create. Error num: " + err);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;

        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "On surface changed. Error num: " + err);
        }
        float ratio = (float) width / height;
        GLES20.glUniform3f(mLightPosHandle, 0,0,5.0f);
        Matrix.frustumM(mProjMatrix, 0, ratio, -ratio, -1, 1, 1, 7);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uModelViewProjectionM");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uModelViewM");

        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 1.0f);

        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "End of surface changed. Error num: " + err);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //GLES20.glFlush();
        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "After flush. Error num: " + err);
        }
        GLES20.glUseProgram(mProgram);

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "Before handles set. Error num: " + err);
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, torusVB);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, normalVB);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, true, 0, colorVB);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "After handles set. Error num: " + err);
        }
        angle += 1.0f;
        horizontalAngle += 1.0f;
        zTrans += 0.05;

        //Log.d("TILLER", "Horizontal Angle in: " + horizontalAngle);
        if(touched ) {
            if(touch != null) {
                if(touch.getX() > MRenderer.this.width - 80) {
                    //Log.d("Tiller", "IntervalNum: " + intervalNum);
                    //intervalNum += 1.0;
                    horizontalAngle -= 2.0f;
                }
                else if(touch.getX() < 80) {
                 //   if(intervalNum > 2) {
                        //Log.d("Tiller", "IntervalNum: " + intervalNum);
                        /*intervalNum -= 1.0;
                        if(intervalNum < 2)
                            intervalNum = 2;*/
                        horizontalAngle += 2.0f;
                  //  }
                }
                else if(touch.getY() < 80) {
                       if(intervalNum > 2) {
                            Log.d("Tiller", "IntervalNum: " + intervalNum);
                            intervalNum -= 1.0;
                           if(intervalNum < 2)
                               intervalNum = 2;
                           System.gc();
                           initShapes();
                           Log.d("Tiller", "Added triangles.");
                       }

                } else if(touch.getY() > MRenderer.this.height - 80) {
                    //Log.d("Tiller", "IntervalNum: " + intervalNum);
                    intervalNum += 1.0;
                    System.gc();
                    initShapes();
                    Log.d("Tiller", "Removed triangles.");

                }

            }
            updated = true;
        }

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
           Log.e("Tiller", "Before Matrices set. Error num: " + err);
        }
        //verticalAngle += 1.0f;
        float[] mRMatrix = new float[16];
        Matrix.setIdentityM(mMMatrix, 0);
        Matrix.setIdentityM(mRMatrix, 0);
        //Matrix.setRotateM(mMMatrix, 0, angle, 1.0f, 0, 0);
        Matrix.setRotateM(mMMatrix, 0, horizontalAngle, 0.0f, 1.0f, 0);
        Matrix.setRotateM(mRMatrix, 0, verticalAngle, 1.0f, 0.0f, 0);
        Matrix.multiplyMM(mMMatrix, 0, mMMatrix, 0, mRMatrix, 0);
        //Matrix.translateM(mMMatrix,0,0,0, zTrans);
        Matrix.multiplyMM(mMVMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        //GLES20.glUniform3f(mLightPosHandle, -3.0f, 5.0f, 5.0f);
        //Log.d("Tiller", "drawing elements");
        //GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6 * iters * iters);

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "Before draw elements. Error num: " + err);
        }
        // NOTE: For torus
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6 * iters * iters, GLES20.GL_UNSIGNED_INT, indicesVB );
        GLES20.glDrawArrays(GLES20.GL_LINES, iters*iters, 2 * iters*iters );
        //GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, indicesVB );
        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
           Log.e("Tiller", "After draw elements. Error num: " + err);
        }
        //Log.d("Tiller", "Iter: " + 6 * iters * iters);
        //GLES20.glDrawArrays(GLES20.GL_LINES, 0, 36);

        //Log.d("Tiller", "drawn elements");
        frameCount++;

        if(Calendar.getInstance().getTimeInMillis() - prevTime >= 1000) {
            //Log.d("TILLER", "FPS: " + frameCount);

            Activity mActivity = (Activity) context;
            final TextView textView = (TextView) mActivity.findViewById(R.id.FPS_text);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText("Count: " + intervalNum + "\nFPS: " + frameCount + "\nTriangles: " + triangleCount);

                }
            });

            prevTime = Calendar.getInstance().getTimeInMillis();
            frameCount = 0;

        }
    }


    public void initShapes() {

        int intervalCount = 6 * (int) Math.pow(intervalNum, 3);
        float torusNormals[] = new float[intervalCount];
        float torusCoords[] = generateTorus(torusNormals);
        int torusIndices[] = arrangeTorusCoords();

        float torusNormalsArray[] = new float[3 * torusIndices.length];

        int k = 0;
        for(int i = 0; i < torusIndices.length; i++ ) {
            int point = torusIndices[i] * 3;
            torusNormalsArray[k++] = torusNormals[point];
            torusNormalsArray[k++] = torusNormals[point+1];
            torusNormalsArray[k++] = torusNormals[point+2];
            Log.d("Tiller", "Point: " + point + " ARRAY: " + torusNormals[point] + ", " + torusNormals[point+1] + ", " + torusNormals[point+2]);
        }
        /*int torusIndices[] = new int[6];

        Log.d("Tiller", "Indices Len: " + torusIndices.length);
        for(int i = 0; i < torusIndices.length; i++) {
            torusIndices[i] = i;
        }*/


        //float torusNormalsArray[] = generateNormals(torusCoords, torusIndices);


        float fullShape[] = new float[torusCoords.length + torusNormals.length*2];
        System.arraycopy(torusCoords, 0, fullShape, 0, torusCoords.length);
        int j = torusCoords.length;
        for(int i = 0; i < torusNormals.length; ){
            //int normPoint = torusIndices[i] * 3;
            /*fullShape[j++] = torusCoords[point];
            fullShape[j++] = torusCoords[point+1];
            fullShape[j++] = torusCoords[point+2];

            int normPoint = i * 3;
            fullShape[j++] = torusNormalsArray[normPoint] + torusCoords[point];
            fullShape[j++] = torusNormalsArray[normPoint+1] + torusCoords[point+1];
            fullShape[j++] = torusNormalsArray[normPoint+2] + torusCoords[point+2];
        */

            fullShape[j++] = 0.0f;
            fullShape[j++] = 0.0f;
            fullShape[j++] = 0.0f;

            fullShape[j++] = torusNormals[i];
            fullShape[j++] = torusNormals[i+1];
            fullShape[j++] = torusNormals[i+2];
            Log.d("Tiller", "i: " + i + " point: " + torusNormals[i] + ", " + torusNormals[i+1] + ", " + torusNormals[i+2]);
            i += 3;
        }
        //Log.d("Tiller", "Put in " + j + " elements.");

        j = 0;
        for(int i = 0; i < fullShape.length; ) {
            Log.d("Tiller", j + ": " +  fullShape[i++] + ", " + fullShape[i++] + ", " + fullShape[i++]);
            j++;
        }
        //Log.d("Tiller", "Fullshape len: " + fullShape.length);


        int x = 0;
        float torusColors[] = new float[(fullShape.length/3) * 4];
        //float torusColors[] = new float[6 * 4];
        for(int i = 0; i < pointCount * 4; ) {
            torusColors[i++] = 0.5f;
            torusColors[i++] = 0.5f;
            torusColors[i++] = 0.5f;
            torusColors[i++] = 1.0f;
        }

        Log.d("Tiller", "PointCount: " + pointCount);
        for(int i = pointCount * 4; i < torusColors.length;) {
            torusColors[i++] = 1.0f;
            torusColors[i++] = 0.0f;
            torusColors[i++] = 0.0f;
            torusColors[i++] = 1.0f;

        }

        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "INIT 1. Error num: " + err);
        }


        Log.d("Tiller", "Iters: " + iters);
        Log.d("Tiller", "Setting torus buffers");
        if(torusVB != null) {
            torusVB.clear();
        }
        Log.d("Tiller", "TorusCoords: " + fullShape.length);
        torusVB = ByteBuffer.allocateDirect(fullShape.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        torusVB.put(fullShape).position(0);

        Log.d("Tiller", "SquareNormals: " + torusNormalsArray.length);
        if(normalVB != null) {
            normalVB.clear();
        }
        normalVB = ByteBuffer.allocateDirect(torusNormalsArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(torusNormalsArray).position(0);

        Log.d("Tiller", "SquareColors: " + torusColors.length);
        if(colorVB != null) {
            colorVB.clear();
        }
        colorVB = ByteBuffer.allocateDirect(torusColors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorVB.put(torusColors).position(0);

        Log.d("Tiller", "SquareIndices: " + torusIndices.length);
        if(indicesVB != null) {
            indicesVB.clear();
        }
        indicesVB = ByteBuffer.allocateDirect(torusIndices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        indicesVB.put(torusIndices).position(0);


        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "INIT 2. Error num: " + err);
        }
    }


    private float[] generateSquare() {
        //Log.d("Tiller", "intervalNum: " + intervalNum);
        float interval = (float) (Math.PI / intervalNum);
        float max = intervalNum * interval * 2;
        float r = 0.5f;
        float R = 1.2f;
        iters = 0;
        pointCount = 0;
        float u = 0;

        //Log.d("Tiller", "iters: " + iters);
        //Log.d("Tiller", "pointCount: " + pointCount);
        float torusCoords[] = new float[18];
        torusCoords[0] = 0.5f; torusCoords[1] = 0.5f; torusCoords[2] = -0.5f;
        torusCoords[3] = -0.5f; torusCoords[4] = 0.5f; torusCoords[5] = -0.5f;
        torusCoords[6] = -0.5f; torusCoords[7] = -0.5f; torusCoords[8] = -0.5f;

        torusCoords[9] = 0.5f; torusCoords[10] = 0.5f; torusCoords[11] = -0.5f;
        torusCoords[12] = -0.5f; torusCoords[13] = -0.5f; torusCoords[14] = -0.5f;
        torusCoords[15] = 0.5f; torusCoords[16] = -0.5f; torusCoords[17] = -0.5f;


        return torusCoords;
    }
    private float[] generateTorus(float[] normals) {
        ArrayList<Float> torusArrayList = new ArrayList<Float>();
        //Log.d("Tiller", "intervalNum: " + intervalNum);
        float interval = (float) (Math.PI / intervalNum);
        float max = intervalNum * interval * 2;
        float r = 0.5f;
        float R = 1.2f;
        iters = 0;
        pointCount = 0;
        float u = 0;
        int k = 0;
        Log.d("Tiller", "Normal size: " + normals.length);

        for(int i = 0; i < intervalNum * 2; i++) {
            iters++;
            //Log.d("Tiller", "interval: " + interval);
            float v = 0;
            for(int j = 0; j < intervalNum * 2; j++) {
                float x = (float) ((R + r * Math.cos(v)) * Math.cos(u));
                float y = (float) ((R + r * Math.cos(v)) * Math.sin(u));
                float z = (float) (r * Math.sin(v));
                torusArrayList.add(x);
                torusArrayList.add(y);
                torusArrayList.add(z);

                /*x = (float) ((r * Math.cos(v)) * Math.cos(u));
                y = (float) ((r * Math.cos(v)) * Math.sin(u));
                z = (float) (r * Math.sin(v));*/

                double length = Math.sqrt(x * x + y * y + z * z);
                /*x /= length;
                y /= length;
                z /= length;*/
                normals[k++] = x;
                normals[k++] = y;
                normals[k++] = z;
                Log.d("Tiller", "normal " + pointCount + ": " + x + ", " + y + ", " + z);
                v += interval;
                pointCount++;

            }
            u += interval;
        }

        //Log.d("Tiller", "iters: " + iters);
        //Log.d("Tiller", "pointCount: " + pointCount);
       float torusCoords[] = new float[torusArrayList.size()];
        for(int i = 0; i < torusArrayList.size(); i++) {
           torusCoords[i] = torusArrayList.get(i);
        }


        return torusCoords;
    }

    private int[] arrangeTorusCoords() {
        int references[] = new int[6 * iters * iters];
        int refCount = 0;
        triangleCount = 0;


        for(int i = 0; i < iters; i++) {
            for(int j = 0; j < iters; j++) {
                int tr = i*iters + j;
                int tl = (tr + 1 < (i + 1) * iters) ? tr + 1 : i * iters;
                int br = (tr + iters < iters * iters) ? tr + iters : j;
                int bl = (tl + iters < iters * iters) ? tl + iters : (j + 1 < iters) ? j + 1 : 0;
                references[refCount] = tr;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;
                references[refCount] = tl;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;
                references[refCount] = br;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;

                references[refCount] = tl;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;
                references[refCount] = bl;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;
                references[refCount] = br;
                //Log.d("Tiller", "refCount: " + refCount + " reference: " + references[refCount]);
                refCount++;

                triangleCount += 2;
            }
        }

        return references;
    }


    private float[] generateNormals(float[] torusCoords, int[] indices) {
        ArrayList<Float> torusNormals = new ArrayList<Float>();

        int i = 0;
        while( i < indices.length ) {
            int i1 = indices[i++];
            int i2 = indices[i++];
            int i3 = indices[i++];

            int p1 = i1 * 3;
            //Log.d("Tiller", "i1: " + i1 + " p1: " + p1);
            int p2 = i2 * 3;
            //Log.d("Tiller", "i2: " + i2 + " p2: " + p2);
            int p3 = i3 * 3;
            //Log.d("Tiller", "i3: " + i3 + " p3: " + p3);

            float x = torusCoords[p1];
            float y = torusCoords[p1+1];
            float z = torusCoords[p1+2];

            float len = (float)(Math.sqrt(x * x + y * y + z * z));
            x /= len;
            y /= len;
            z /= len;
            //Log.d("Tiller", x + ", " + y + ", " + z);

           /*torusNormals.add(x);
            torusNormals.add(y);
            torusNormals.add(z);*/


            float point1[] = {torusCoords[p1], torusCoords[p1+1], torusCoords[p1+2]  } ;
            float point2[] = {torusCoords[p2], torusCoords[p2+1], torusCoords[p2+2]  } ;
            float point3[] = {torusCoords[p3], torusCoords[p3+1], torusCoords[p3+2]  } ;

            float veca1[] = {point1[0] - point2[0], point1[1] - point2[1], point1[2] - point2[2]};
            float vecb1[] = {point1[0] - point3[0], point1[1] - point3[1], point1[2] - point3[2]};
            float normal1[] = {
                    veca1[1] * vecb1[2] - veca1[2] * vecb1[1],
                    veca1[2] * vecb1[0] - veca1[0] * vecb1[2],
                    veca1[0] * vecb1[1] - veca1[1] * vecb1[0]
            };

            //Log.d("Tiller", point1[0] + ", " + point1[1] + ", " + point1[2] + "::" + normal1[0] + ", " + normal1[1] + ", " + normal1[2]);
            for (float aNormal : normal1) {
                torusNormals.add(aNormal);
            }

            float veca2[] = {point2[0] - point3[0], point2[1] - point3[1], point2[2] - point3[2]};
            float vecb2[] = {point2[0] - point1[0], point2[1] - point1[1], point2[2] - point1[2]};
            float normal2[] = {
                    veca2[1] * vecb2[2] - veca2[2] * vecb2[1],
                    veca2[2] * vecb2[0] - veca2[0] * vecb2[2],
                    veca2[0] * vecb2[1] - veca2[1] * vecb2[0]
            };

            //Log.d("Tiller", point2[0] + ", " + point2[1] + ", " + point2[2] + "::" + normal2[0] + ", " + normal2[1] + ", " + normal2[2]);
            for (float aNormal : normal2) {
                torusNormals.add(aNormal);
            }

            float veca3[] = {point3[0] - point1[0], point3[1] - point1[1], point3[2] - point1[2]};
            float vecb3[] = {point3[0] - point2[0], point3[1] - point2[1], point3[2] - point2[2]};
            float normal3[] = {
                    veca3[1] * vecb3[2] - veca3[2] * vecb3[1],
                    veca3[2] * vecb3[0] - veca3[0] * vecb3[2],
                    veca3[0] * vecb3[1] - veca3[1] * vecb3[0]
            };

            //Log.d("Tiller", point3[0] + ", " + point3[1] + ", " + point3[2] + "::" + normal3[0] + ", " + normal3[1] + ", " + normal3[2]);
            for (float aNormal : normal3) {
                torusNormals.add(aNormal);
            }

        }

        float torusNormalsArray[] = new float[torusNormals.size()];
        for(i = 0; i < torusNormals.size(); i++) {
            torusNormalsArray[i] = torusNormals.get(i);
        }


        return torusNormalsArray;
    }

    public int loadShader(int type, String shaderStr) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderStr);
        GLES20.glCompileShader(shader);
        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "In load shader. Error num: " + err);
        }
        return shader;
    }

}

