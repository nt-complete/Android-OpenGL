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
    /**
     * I have started experiencing slowdown at 72.
     *
     * 80: 35.0 fps :: 51200 triangles
     * 100: 24.0 fps
     * 144: 13 fps
     * 156: 12 fps
     * 157: 11.0 fps
     *
     * This is the max size
     *
     * 158: OOM
     */
    public float intervalNum = 3;
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
                    "	float LambertTerm = max(dot(modelViewNormal, lightVector), 0.1);\n " +
                    "	float diffuse = LambertTerm * (1.0 / (1.0 + (0.5 * distance * distance)));\n " +
                    " \n " +
                    "	 vec3 R = reflect(-lightVector, modelViewNormal); \n " +
                    "    vec3 vEye = vec3(0.0, 0.0, 3.0); \n " +
                    "	 float specular = 1.0 * 1.0 * pow(max(dot(R, vEye), 0.0), 2.0);  \n " +
                    "\n " +
                    "	varColor = ((vColor * 0.2) + vColor * diffuse * specular) ;\n " +
                    "	//varColor =  (vColor * 0.3) + vColor * LambertTerm;\n " +
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

    private Context context;
    private int mProgram;
    private int mPositionHandle, mModelViewMHandle, mModelViewProjMHandle, mLightPosHandle, mColorHandle, mNormalHandle;
    private float[] mModelViewProjM = new float[16];
    private float[] mModelM = new float[16];
    private float[] mViewM = new float[16];
    private float[] mModelViewM = new float[16];
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
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

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
        mModelViewProjMHandle = GLES20.glGetUniformLocation(mProgram, "uModelViewProjectionM");
        mModelViewMHandle = GLES20.glGetUniformLocation(mProgram, "uModelViewM");

        prevTime = Calendar.getInstance().getTimeInMillis();
        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "end of create. Error num: " + err);
        }

        Matrix.setLookAtM(mViewM, 0, 0.0f, 0.0f, 3.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        initShapes();
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
        //GLES20.glUniform3f(mLightPosHandle, 0,0,5.0f);
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 7);

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

        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 12, normalVB);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, true, 16, colorVB);
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
        //float[] mRMatrix = new float[16];

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "Before multiplication. Error num: " + err);
        }

        float[] lightPos = {0.0f, 0.0f, 5.0f};
        //GLES20.glUniform3f(mLightPosHandle, 0,0,-5.0f);
        GLES20.glUniform3fv(mLightPosHandle, 1, lightPos, 0);

        Matrix.setIdentityM(mModelM, 0);
        //Matrix.setIdentityM(mRMatrix, 0);
        Matrix.setRotateM(mModelM, 0, horizontalAngle, 0.0f, 1.0f, 0);

        Matrix.multiplyMM(mModelViewM, 0, mViewM, 0, mModelM, 0);
        Matrix.multiplyMM(mModelViewProjM, 0, mProjMatrix, 0, mModelViewM, 0);

        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "After multiplication. Error num: " + err);
        }
        GLES20.glUniformMatrix4fv(mModelViewMHandle, 1, false, mModelViewM, 0);
        GLES20.glUniformMatrix4fv(mModelViewProjMHandle, 1, false, mModelViewProjM, 0);



        err = GLES20.glGetError();
        if(err != GLES20.GL_NO_ERROR) {
            Log.e("Tiller", "Before draw elements. Error num: " + err);
        }
        // NOTE: For torus
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6 * iters * iters, GLES20.GL_UNSIGNED_INT, indicesVB );
        //GLES20.glDrawArrays(GLES20.GL_LINES, iters*iters, 2 * iters*iters );
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

        Log.d("Tiller", "INTERVALNUM: " + intervalNum);
        int intervalCount = (int) (12 * intervalNum * intervalNum);
        Log.d("Tiller", "INTERVALCOUNT: " + intervalCount);
        float torusNormals[] = new float[intervalCount];
        float torusCoords[] = generateTorus(torusNormals);
        //float torusNormals[] = new float[18];
        //float torusCoords[] = generateSquare(torusNormals);
        int torusIndices[] = arrangeTorusCoords();
        //float torusNormals[] = generateNormals(torusCoords);

        float torusNormalsArray[] = new float[3 * torusIndices.length];

        int k = 0;
        /*for(int i = 0; i < torusIndices.length; i++ ) {
            int point = torusIndices[i] * 3;
            torusNormalsArray[k++] = torusNormals[point];
            torusNormalsArray[k++] = torusNormals[point+1];
            torusNormalsArray[k++] = torusNormals[point+2];
            //Log.d("Tiller", "Point: " + point + " ARRAY: " + torusNormals[point] + ", " + torusNormals[point+1] + ", " + torusNormals[point+2]);
        }
        /*int torusIndices[] = new int[6];

        Log.d("Tiller", "Indices Len: " + torusIndices.length);
        for(int i = 0; i < torusIndices.length; i++) {
            torusIndices[i] = i;
        }*****


        //float torusNormalsArray[] = generateNormals(torusCoords, torusIndices);


        float fullShape[] = new float[torusCoords.length + torusNormals.length*2];
        System.arraycopy(torusCoords, 0, fullShape, 0, torusCoords.length);
        int j = torusCoords.length;
        //Log.d("Tiller", "CoordsLen: " + j + " , NormLen: " + torusNormals.length);
        for(int i = 0; i < torusCoords.length; ){
            //int normPoint = torusIndices[i] * 3;
            /*fullShape[j++] = torusCoords[point];
            fullShape[j++] = torusCoords[point+1];
            fullShape[j++] = torusCoords[point+2];

            int normPoint = i * 3;
            fullShape[j++] = torusNormalsArray[normPoint] + torusCoords[point];
            fullShape[j++] = torusNormalsArray[normPoint+1] + torusCoords[point+1];
            fullShape[j++] = torusNormalsArray[normPoint+2] + torusCoords[point+2];
        *****

            /*fullShape[j++] = 0.0f;
            fullShape[j++] = 0.0f;
            fullShape[j++] = 0.0f;
            *****
            fullShape[j++] = torusCoords[i];
            fullShape[j++] = torusCoords[i+1];
            fullShape[j++] = torusCoords[i+2];

            fullShape[j++] = ( 0.2f * torusNormals[i]) + torusCoords[i];
            fullShape[j++] = (0.2f * torusNormals[i+1]) + torusCoords[i+1];
            fullShape[j++] = (0.2f * torusNormals[i+2]) + torusCoords[i+2];
            //Log.d("Tiller", "i: " + i + " point: " + torusCoords[i] + ", " + torusCoords[i+1] + ", " + torusCoords[i+2]);
            //Log.d("Tiller", "i: " + i + " normal: " + torusNormals[i] + ", " + torusNormals[i+1] + ", " + torusNormals[i+2]);
            i += 3;
        }
        j = 0;
        //Log.d("Tiller", "Put in " + j + " elements.");

        for(int i = 0; i < fullShape.length; ) {
            Log.d("Tiller", j + ": " +  fullShape[i++] + ", " + fullShape[i++] + ", " + fullShape[i++]);
            j++;
        }
        //Log.d("Tiller", "Fullshape len: " + fullShape.length);
        */


        int x = 0;
        //float torusColors[] = new float[(fullShape.length/3) * 4];
        float torusColors[] = new float[(torusCoords.length/3) * 4];
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
        Log.d("Tiller", "TorusCoords: " + torusCoords.length);
        torusVB = ByteBuffer.allocateDirect(torusCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        torusVB.put(torusCoords).position(0);

        Log.d("Tiller", "SquareNormals: " + torusNormalsArray.length);
        if(normalVB != null) {
            normalVB.clear();
        }
        normalVB = ByteBuffer.allocateDirect(torusNormals.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(torusNormals).position(0);

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


    private float[] generateSquare(float[] normals) {

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
        torusCoords[0] = 1.0f; torusCoords[1] = 1.0f; torusCoords[2] = 1.0f;
        torusCoords[3] = -1.0f; torusCoords[4] = 1.0f; torusCoords[5] = 1.0f;
        torusCoords[6] = -1.0f; torusCoords[7] = -1.0f; torusCoords[8] =1.0f;

        torusCoords[9] = 1.0f; torusCoords[10] = 1.0f; torusCoords[11] = 1.0f;
        torusCoords[12] = -1.0f; torusCoords[13] = -1.0f; torusCoords[14] = 1.0f;
        torusCoords[15] = 1.0f; torusCoords[16] = -1.0f; torusCoords[17] = 1.0f;

        torusCoords[18] = -1.0f; torusCoords[10] = 1.0f; torusCoords[11] = 1.0f;
        torusCoords[12] = -1.0f; torusCoords[13] = -1.0f; torusCoords[14] = 1.0f;
        torusCoords[15] = -1.0f; torusCoords[16] = -1.0f; torusCoords[17] = 1.0f;

        for(int i = 0; i < 18; ) {
            normals[i++] = 0.0f;
            normals[i++] = 0.0f;
            normals[i++] = 1.0f;
        }
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

        /*float ringDelta = 2.0f * (float) Math.PI / (intervalNum*2);
        float sideDelta = 2.0f * (float) Math.PI / (intervalNum*2);
        float theta = 0.0f, cosTheta = 1.0f, sinTheta = 0.0f;

        iters = (int) intervalNum * 2;
        for (int i = ((int)intervalNum*2) - 1; i >= 0; i--) {
            float theta1 = theta + ringDelta;
            float cosTheta1 = (float) Math.cos(theta1);
            float sinTheta1 = (float) Math.sin(theta1);
            float phi = 0.0f;
            for (int j = ((int)intervalNum*2); j >= 0; j--) {
                phi += sideDelta;
                float cosPhi = (float) Math.cos(phi);
                float sinPhi = (float) Math.sin(phi);
                float dist = R + r * cosPhi;

                float x = cosTheta1 * dist;
                float y = -sinTheta1 * dist;
                float z = r * sinPhi;
                torusArrayList.add(x);
                torusArrayList.add(y);
                torusArrayList.add(z);

                x = cosTheta1 * cosPhi;
                y = -sinTheta1 * cosPhi;
                z = sinPhi;
                normals[k++] = x;
                normals[k++] = y;
                normals[k++] = z;

                x = cosTheta * dist;
                y = -sinTheta * dist;
                z = r * sinPhi;
                torusArrayList.add(x);
                torusArrayList.add(y);
                torusArrayList.add(z);

                x = cosTheta * cosPhi;
                y = -sinTheta * cosPhi;
                z = sinPhi;
                normals[k++] = x;
                normals[k++] = y;
                normals[k++] = z;
            }
            theta = theta1;
            cosTheta = cosTheta1;
            sinTheta = sinTheta1;
        }*/
        //Log.d("Tiller", "Normal size: " + normals.length);

        float dist;
        for(int i = 0; i < intervalNum * 2; i++) {
            iters++;
            //Log.d("Tiller", "interval: " + interval);
            float v = 0;
            for(int j = 0; j < intervalNum * 2; j++) {
                // cosPhi = Math.cos(v)
                // sinPhi = Math.sin(v)

                // cosTheta = Math.cos(u)
                // sinTheta = Math.sin(u)
                dist = (float) (R + r * Math.cos(v));


                float x =  (float) (dist * Math.cos(u));
                float y = (float) (dist * Math.sin(u));
                float z = (float) (r * Math.sin(v));
                torusArrayList.add(x);
                torusArrayList.add(y);
                torusArrayList.add(z);

                /*double length = Math.sqrt(x * x + y * y + z * z);
                x /= length;
                y /= length;
                z /= length;*/

                x = (float) (Math.cos(u) * Math.cos(v));
                y = (float) (Math.sin(u) * Math.cos(v));
                z = (float) (Math.sin(v));
                normals[k++] = x;
                normals[k++] = y;
                normals[k++] = z;
                //Log.d("Tiller", "normal " + pointCount + ": " + x + ", " + y + ", " + z);
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


    private float[] generateNormals(float[] torusCoords) {
        ArrayList<Float> torusNormals = new ArrayList<Float>();

        for(int i = 0; i < iters; i++) {
            for(int j = 0; j < iters; j++) {
                int tr = i*iters + j;
                int tl = (tr + 1 < (i + 1) * iters) ? tr + 1 : i * iters;
                int br = (tr + iters < iters * iters) ? tr + iters : j;
                int bl = (tl + iters < iters * iters) ? tl + iters : (j + 1 < iters) ? j + 1 : 0;

                float x, y, z;
                x = y = z = 0.0f;

                int p1, p2, p3;
                p1 = 3 * tr;
                p2 = 3 * tl;
                p3 = 3 * br;
                Log.d("Tiller", tr + ", " + tl + ", " + bl + ", " + br);


                /*float x = torusCoords[p1];
                float y = torusCoords[p1+1];
                float z = torusCoords[p1+2];

                float len = (float)(Math.sqrt(x * x + y * y + z * z));
                x /= len;
                y /= len;
                z /= len;*/
                //Log.d("Tiller", x + ", " + y + ", " + z);

               /*torusNormals.add(x);
                torusNormals.add(y);
                torusNormals.add(z);*/


                float point1[] = {torusCoords[p1], torusCoords[p1+1], torusCoords[p1+2]  } ;
                float point2[] = {torusCoords[p2], torusCoords[p2+1], torusCoords[p2+2]  } ;
                float point3[] = {torusCoords[p3], torusCoords[p3+1], torusCoords[p3+2]  } ;

                float veca1[] = {point1[0] - point2[0], point1[1] - point2[1], point1[2] - point2[2]};
                float vecb1[] = {point1[0] - point3[0], point1[1] - point3[1], point1[2] - point3[2]};
                x += veca1[1] * vecb1[2] - veca1[2] * vecb1[1];
                y += veca1[2] * vecb1[0] - veca1[0] * vecb1[2];
                z += veca1[0] * vecb1[1] - veca1[1] * vecb1[0];

                Log.d("Tiller", "POINT1: " + point1[0] + ", " + point1[1] + ", " + point1[2]);
                Log.d("Tiller", "POINT2: " + point2[0] + ", " + point2[1] + ", " + point2[2]);
                Log.d("Tiller", "POINT3: " + point3[0] + ", " + point3[1] + ", " + point3[2]);
                /*for (float aNormal : normal1) {
                    torusNormals.add(aNormal);
                }*/

                tr = ((i*iters + j - 1) >= i*iters) ? i*iters + j - 1 : (i+1)*iters - 1 ;
                tl = (tr + 1 < (i + 1) * iters) ? tr + 1 : i * iters;
                br = (tr + iters < iters * iters) ? tr + iters : j;
                bl = (tl + iters < iters * iters) ? tl + iters : (j + 1 < iters) ? j + 1 : 0;

                br = (tr + iters < iters * iters) ? tr + iters : j - 1 < 0 ? 0 : j-1;
                bl = (tl + iters < iters * iters) ? tl + iters : j;

                Log.d("Tiller", tr + ", " + tl + ", " + bl + ", " + br);
                p1 = 3 * tl;
                p2 = 3 * bl;
                p3 = 3 * br;

                point1 = new float[] {torusCoords[p1], torusCoords[p1+1], torusCoords[p1+2]  } ;
                point2 = new float[] {torusCoords[p2], torusCoords[p2+1], torusCoords[p2+2]  } ;
                point3 = new float[] {torusCoords[p3], torusCoords[p3+1], torusCoords[p3+2]  } ;


                float veca2[] = {point2[0] - point3[0], point2[1] - point3[1], point2[2] - point3[2]};
                float vecb2[] = {point2[0] - point1[0], point2[1] - point1[1], point2[2] - point1[2]};
                x += veca2[1] * vecb2[2] - veca2[2] * vecb2[1];
                y += veca2[2] * vecb2[0] - veca2[0] * vecb2[2];
                z += veca2[0] * vecb2[1] - veca2[1] * vecb2[0];

                //Log.d("Tiller", point2[0] + ", " + point2[1] + ", " + point2[2] + "::" + normal2[0] + ", " + normal2[1] + ", " + normal2[2]);
               /* for (float aNormal : normal2) {
                    torusNormals.add(aNormal);
                }*/

                /*p1 = 3 * tl;
                p2 = 3 * br;
                p3 = 3 * tr;

                point1 = new float[] {torusCoords[p1], torusCoords[p1+1], torusCoords[p1+2]  } ;
                point2 = new float[] {torusCoords[p2], torusCoords[p2+1], torusCoords[p2+2]  } ;
                point3 = new float[] {torusCoords[p3], torusCoords[p3+1], torusCoords[p3+2]  } ;

                float veca3[] = {point3[0] - point1[0], point3[1] - point1[1], point3[2] - point1[2]};
                float vecb3[] = {point3[0] - point2[0], point3[1] - point2[1], point3[2] - point2[2]};
                x += veca3[1] * vecb3[2] - veca3[2] * vecb3[1];
                y += veca3[2] * vecb3[0] - veca3[0] * vecb3[2];
                z += veca3[0] * vecb3[1] - veca3[1] * vecb3[0];*/

                //Log.d("Tiller", point3[0] + ", " + point3[1] + ", " + point3[2] + "::" + normal3[0] + ", " + normal3[1] + ", " + normal3[2]);
                /*for (float aNormal : normal3) {
                    torusNormals.add(aNormal);
                }*/

                float len = (float)(Math.sqrt(x * x + y * y + z * z));
                x /= len;
                y /= len;
                z /= len;
                Log.d("Tiller","NORMAL: " + x + ", " + y + ", " + z);

                torusNormals.add(x);
                torusNormals.add(y);
                torusNormals.add(z);
            }
        }

        float torusNormalsArray[] = new float[torusNormals.size()];
        for(int i = 0; i < torusNormals.size(); i++) {
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

