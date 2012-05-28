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
            "uniform mat4 uMVPMatrix; \n " +
            "uniform mat4 uMVMatrix; \n " +
            "uniform vec3 uLightPosition;\n " +
            "\n " +
            "varying vec4 varColor;\n " +
            "\n " +
            "void main()  {\n " +
            "	// For transferring the vertex position in eye-space\n " +
            "	vec3 modelViewVertex = vec3(uMVMatrix * vPosition);\n " +
            "\n " +
            "	// Obtains the normal\n " +
            "	vec3 modelViewNormal = vec3(uMVMatrix * vec4(vNormal, 0.0));\n " +
            "\n " +
            "	float distance = length(uLightPosition - modelViewVertex);\n " +
            "	vec3 lightVector = -normalize(uLightPosition - modelViewVertex);\n " +
            "\n " +
            "	float LambertTerm = max(dot(modelViewNormal, lightVector), 0.5);\n " +
            "	float diffuse = LambertTerm * (1.0 / (1.0 + (0.1 * distance * distance)));\n " +
            " \n " +
            "	 vec3 R = reflect(-lightVector, modelViewNormal); \n " +
            "    vec3 vEye = vec3(0.0, 0.0, 1.0); \n " +
            "	 float specular = 1.0 * 1.0 * pow(max(dot(R, vEye), 0.0), 1.0);  \n " +
            "\n " +
            "	//varColor = ((vColor * 0.2) + vColor * diffuse * specular) ;\n " +
            "	varColor = vColor * diffuse;\n " +
            "	//varColor = vColor ;\n " +
            "\n " +
            "	gl_Position = uMVPMatrix * vPosition; \n " +
            "\n " +
            "\n " +
            "} \n ";

    private final String fragmentShaderStr =
            "precision mediump float; \n" +
                    "varying vec4 varColor;\n" +
                    "void main() { \n" +
                    "  gl_FragColor = varColor; \n" +
                    "}  \n";

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
        Matrix.frustumM(mProjMatrix, 0, ratio, -ratio, -1, 1, 1, 7);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix");
        GLES20.glUniform3f(mLightPosHandle, 0,0,-5.0f);

        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

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
        if(touched && !updated) {
            if(touch != null) {
                if(touch.getX() > MRenderer.this.width - 40) {
                    //Log.d("Tiller", "IntervalNum: " + intervalNum);
                    intervalNum += 1.0;
                    //horizontalAngle -= 1.0f;
                    System.gc();
                    updateShapes();
                    Log.d("Tiller", "Added triangles.");
                }
                if(touch.getX() < 40) {
                    if(intervalNum > 2) {
                        //Log.d("Tiller", "IntervalNum: " + intervalNum);
                        intervalNum -= 1.0;
                        if(intervalNum < 2)
                            intervalNum = 2;
                        //horizontalAngle += 1.0f;
                        System.gc();
                        updateShapes();
                        Log.d("Tiller", "Removed triangles.");
                    }
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
        //Matrix.translateM(mMMatrix,0,0,zTrans,0);
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
        // NOTE: For square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6 * iters * iters, GLES20.GL_UNSIGNED_INT, indicesVB );
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

        float squareCoords[] = generateTorus();
        // Note: For square
        int squareIndices[] = arrangeTorusCoords();
        /*int squareIndices[] = new int[6];
        Log.d("Tiller", "Indices Len: " + squareIndices.length);
        for(int i = 0; i < squareIndices.length; i++) {
            squareIndices[i] = i;
        }*/

        float squareNormalsArray[] = generateNormals(squareCoords, squareIndices);

        int x = 0;
        // Note: For Square
        float squareColors[] = new float[pointCount * 4];
        //float squareColors[] = new float[6 * 4];
        for(int i = 0; i < pointCount * 4; ) {
        /*for(int i = 0; i < 6 * 4; ) {
            squareColors[i++] = 1.0f;
            squareColors[i++] = 0.0f;
            squareColors[i++] = 0.0f;
            squareColors[i++] = 1.0f;*/

            /*switch(x % 2) {
                case 0:*/
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 1.0f;
                    /*break;
                case 1:
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 1.0f;
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 1.0f;
                    break;
                /*case 2:
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 1.0f;
                    squareColors[i++] = 1.0f;
                    break;
            }*/
            x++;
        }

        int err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "INIT 1. Error num: " + err);
        }

        Log.d("Tiller", "Setting torus buffers");
        Log.d("Tiller", "SquareCoords: " + squareCoords.length);
        torusVB = ByteBuffer.allocateDirect(squareCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        torusVB.put(squareCoords).position(0);

        Log.d("Tiller", "SquareNormals: " + squareNormalsArray.length);
        normalVB = ByteBuffer.allocateDirect(squareNormalsArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(squareNormalsArray).position(0);

        Log.d("Tiller", "SquareColors: " + squareColors.length);
        colorVB = ByteBuffer.allocateDirect(squareColors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorVB.put(squareColors).position(0);

        Log.d("Tiller", "SquareIndices: " + squareIndices.length);
        indicesVB = ByteBuffer.allocateDirect(squareIndices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        indicesVB.put(squareIndices).position(0);


        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "INIT 2. Error num: " + err);
        }
    }


    public void updateShapes() {

        float squareCoords[] = generateTorus();
        int squareIndices[] = arrangeTorusCoords();
        /*for(int i = 0; i < squareIndices.length; i++) {
            squareIndices[i] = i;
        }*/

        float squareNormalsArray[] = generateNormals(squareCoords, squareIndices);


        float squareColors[] = new float[pointCount * 4];
        int x = 0;
        for(int i = 0; i < pointCount * 4; ) {
            /*switch(x % 2) {
                case 0:*/
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 0.5f;
                    squareColors[i++] = 1.0f;
                   /* break;
                case 1:
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 1.0f;
                    squareColors[i++] = 1.0f;
                    break;
                /*case 2:
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 0.0f;
                    squareColors[i++] = 1.0f;
                    squareColors[i++] = 1.0f;
                    break;
            }*/
            x++;
        }

        Log.d("Tiller", "Setting torus buffers");
        torusVB.clear();
        Log.d("Tiller", "SquareCoords: " + squareCoords.length);
        torusVB = ByteBuffer.allocateDirect(squareCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        torusVB.put(squareCoords).position(0);

        Log.d("Tiller", "SquareNormals: " + squareNormalsArray.length);
        normalVB.clear();
        normalVB = ByteBuffer.allocateDirect(squareNormalsArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(squareNormalsArray).position(0);

        Log.d("Tiller", "SquareColors: " + squareColors.length);
        colorVB.clear();
        colorVB = ByteBuffer.allocateDirect(squareColors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorVB.put(squareColors).position(0);

        Log.d("Tiller", "SquareIndices: " + squareIndices.length);
        indicesVB.clear();
        indicesVB = ByteBuffer.allocateDirect(squareIndices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        indicesVB.put(squareIndices).position(0);


        Log.d("Tiller", "Torus buffers have been set");
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
    private float[] generateTorus() {
        ArrayList<Float> torusArrayList = new ArrayList<Float>();
        //Log.d("Tiller", "intervalNum: " + intervalNum);
        float interval = (float) (Math.PI / intervalNum);
        float max = intervalNum * interval * 2;
        float r = 0.5f;
        float R = 1.2f;
        iters = 0;
        pointCount = 0;
        float u = 0;
        for(int i = 0; i < intervalNum * 2; i++) {
            iters++;
            //Log.d("Tiller", "interval: " + interval);
            float v = 0;
            for(int j = 0; j < intervalNum * 2; j++) {
                pointCount++;
                float x = (float) ((R + r * Math.cos(v)) * Math.cos(u));
                float y = (float) ((R + r * Math.cos(v)) * Math.sin(u));
                float z = (float) (r * Math.sin(v));
                torusArrayList.add(x);
                torusArrayList.add(y);
                torusArrayList.add(z);
                //Log.d("Tiller",pointCount + ":: " + u + " " + v + ": " + x + ", " + y + ", " + z);
                v += interval;

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


    private float[] generateNormals(float[] squareCoords, int[] indices) {
        ArrayList<Float> squareNormals = new ArrayList<Float>();

        int i = 0;
        while( i < indices.length ) {
            int i1 = indices[i++];
            /*int i2 = indices[i++];
            int i3 = indices[i++];*/

            int p1 = i1 * 3;
            Log.d("Tiller", "i1: " + i1 + " p1: " + p1);
            /*int p2 = i2 * 3;
            Log.d("Tiller", "i2: " + i2 + " p2: " + p2);
            int p3 = i3 * 3;
            Log.d("Tiller", "i3: " + i3 + " p3: " + p3);
            */

            float x = squareCoords[p1];
            float y = squareCoords[p1+1];
            float z = squareCoords[p1+2];

            float len = (float)(Math.sqrt(x * x + y * y + z * z));
            x /= len;
            y /= len;
            z /= len;

           squareNormals.add(x);
            squareNormals.add(y);
            squareNormals.add(z);


            /*float point1[] = {squareCoords[p1], squareCoords[p1+1], squareCoords[p1+2]  } ;
            float point2[] = {squareCoords[p2], squareCoords[p2+1], squareCoords[p2+2]  } ;
            float point3[] = {squareCoords[p3], squareCoords[p3+1], squareCoords[p3+2]  } ;

            float veca1[] = {point1[0] - point2[0], point1[1] - point2[1], point1[2] - point2[2]};
            float vecb1[] = {point1[0] - point3[0], point1[1] - point3[1], point1[2] - point3[2]};
            float normal1[] = {
                    3 * (veca1[1] * vecb1[2] - veca1[2] * vecb1[1]),
                    3 * (veca1[2] * vecb1[0] - veca1[0] * vecb1[2]),
                    3 * (veca1[0] * vecb1[1] - veca1[1] * vecb1[0])
            };

            Log.d("Tiller", point1[0] + ", " + point1[1] + ", " + point1[2] + "::" + normal1[0] + ", " + normal1[1] + ", " + normal1[2]);
            for (float aNormal : normal1) {
                squareNormals.add(aNormal);
            }

            float veca2[] = {point2[0] - point3[0], point2[1] - point3[1], point2[2] - point3[2]};
            float vecb2[] = {point2[0] - point1[0], point2[1] - point1[1], point2[2] - point1[2]};
            float normal2[] = {
                    3 * (veca2[1] * vecb2[2] - veca2[2] * vecb2[1]),
                    3 * (veca2[2] * vecb2[0] - veca2[0] * vecb2[2]),
                    3 * (veca2[0] * vecb2[1] - veca2[1] * vecb2[0])
            };

            Log.d("Tiller", point2[0] + ", " + point2[1] + ", " + point2[2] + "::" + normal2[0] + ", " + normal2[1] + ", " + normal2[2]);
            for (float aNormal : normal2) {
                squareNormals.add(aNormal);
            }

            float veca3[] = {point3[0] - point1[0], point3[1] - point1[1], point3[2] - point1[2]};
            float vecb3[] = {point3[0] - point2[0], point3[1] - point2[1], point3[2] - point2[2]};
            float normal3[] = {
                    3 * (veca3[1] * vecb3[2] - veca3[2] * vecb3[1]),
                    3 * (veca3[2] * vecb3[0] - veca3[0] * vecb3[2]),
                    3 * (veca3[0] * vecb3[1] - veca3[1] * vecb3[0])
            };

            Log.d("Tiller", point3[0] + ", " + point3[1] + ", " + point3[2] + "::" + normal3[0] + ", " + normal3[1] + ", " + normal3[2]);
            for (float aNormal : normal3) {
                squareNormals.add(aNormal);
            }*/

        }

        float squareNormalsArray[] = new float[squareNormals.size()];
        for(i = 0; i < squareNormals.size(); i++) {
            squareNormalsArray[i] = squareNormals.get(i);
        }


        return squareNormalsArray;
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

