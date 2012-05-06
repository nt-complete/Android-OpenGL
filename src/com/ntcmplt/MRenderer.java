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
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 4/10/12
 * Time: 7:54 PM
 */


public class MRenderer implements GLSurfaceView.Renderer {

    private int iters;

    private FloatBuffer triangleVB, normalVB, colorVB;
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
            "	vec3 lightVector = normalize(uLightPosition - modelViewVertex);\n " +
            "\n " +
            "	float LambertTerm = max(dot(modelViewNormal, lightVector), 0.5);\n " +
            "	float diffuse = LambertTerm * (1.0 / (1.0 + (0.1 * distance * distance)));\n " +
            " \n " +
            "	 vec3 R = reflect(-lightVector, modelViewNormal); \n " +
            "    vec3 vEye = vec3(0.0, 0.0, 1.0); \n " +
            "	 float specular = 1.0 * 1.0 * pow(max(dot(R, vEye), 0.0), 1.0);  \n " +
            "\n " +
            "	varColor = ((vColor * 0.2) + vColor * diffuse * specular) ;\n " +
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

        GLES20.glGetShaderInfoLog(vertexShader);
        GLES20.glGetProgramInfoLog(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "uLightPosition");
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");

        prevTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;

        float ratio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, ratio, -ratio, -1, 1, 2, 7);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix");
        GLES20.glUniform3f(mLightPosHandle, 0,0,5.0f);

        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, triangleVB);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, true, 0, normalVB);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, true, 0, colorVB);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        angle += 1.0f;
        //horizontalAngle += 1.0f;
        zTrans += 0.05;

        //Log.d("TILLER", "Horizontal Angle in: " + horizontalAngle);
        if(touched) {
            if(touch != null) {
                if(touch.getX() > MRenderer.this.width / 2) {
                    horizontalAngle -= 1.0f;
                } else {
                    horizontalAngle += 1.0f;
                }

            }
        }
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        //GLES20.glDrawArrays(GLES20.GL_LINES, 0, 36);

        frameCount++;

        if(Calendar.getInstance().getTimeInMillis() - prevTime >= 1000) {
            //Log.d("TILLER", "FPS: " + frameCount);

            Activity mActivity = (Activity) context;
            final TextView textView = (TextView) mActivity.findViewById(R.id.FPS_text);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText("FPS: " + frameCount);

                }
            });

            prevTime = Calendar.getInstance().getTimeInMillis();
            frameCount = 0;

        }
    }


    private void initShapes() {

        float squareCoords[] = generateTorus();
        squareCoords = arrangeTorusCoords(squareCoords);
        /*float squareCoords[] = {
                // Back face
                -0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,

                // Right face
                0.5f, 0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, -0.5f,

                // Front face
                -0.5f, 0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,

                // Left face
                -0.5f, 0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, 0.5f,

                // Top face
                -0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, 0.5f,

                // Bottom face
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, -0.5f
        };*/


        ArrayList<Float> squareNormals = new ArrayList<Float>();
        int i = 0;
        while( i < squareCoords.length ) {
            float point1[] = {squareCoords[i++], squareCoords[i++], squareCoords[i++]  } ;
            float point2[] = {squareCoords[i++], squareCoords[i++], squareCoords[i++]  } ;
            float point3[] = {squareCoords[i++], squareCoords[i++], squareCoords[i++]  } ;

            float vec11[] = {point1[0] - point2[0], point1[1] - point2[1], point1[2] - point2[2]};
            float vec21[] = {point1[0] - point3[0], point1[1] - point3[1], point1[2] - point3[2]};
            float normal1[] = {
                    vec11[1] * vec21[2] - vec11[2] * vec21[1],
                    vec11[2] * vec21[0] - vec11[0] * vec21[2],
                    vec11[0] * vec21[1] - vec11[1] * vec21[0]
            };

            for (float aNormal : normal1) {
                squareNormals.add(aNormal);
            }


            float vec22[] = {point2[0] - point1[0], point2[1] - point1[1], point2[2] - point1[2]};
            float vec12[] = {point2[0] - point3[0], point2[1] - point3[1], point2[2] - point3[2]};
            float normal2[] = {
                    vec12[1] * vec22[2] - vec12[2] * vec22[1],
                    vec12[2] * vec22[0] - vec12[0] * vec22[2],
                    vec12[0] * vec22[1] - vec12[1] * vec22[0]
            };

            for (float aNormal : normal2) {
                squareNormals.add(aNormal);
            }


            float vec13[] = {point3[0] - point1[0], point3[1] - point1[1], point3[2] - point1[2]};
            float vec23[] = {point3[0] - point2[0], point3[1] - point2[1], point3[2] - point2[2]};
            float normal3[] = {
                    vec13[1] * vec23[2] - vec13[2] * vec23[1],
                    vec13[2] * vec23[0] - vec13[0] * vec23[2],
                    vec13[0] * vec23[1] - vec13[1] * vec23[0]
            };

            for (float aNormal : normal3) {
                squareNormals.add(aNormal);
            }
        }

        float squareNormalsArray[] = new float[squareNormals.size()];
        for(i = 0; i < squareNormals.size(); i++) {
            squareNormalsArray[i] = squareNormals.get(i);

        }

        for(i = 0; i < squareNormalsArray.length;) {
           Log.d("Tiller", "Normal: " + squareNormalsArray[i++] + ", " + squareNormalsArray[i++] + ", " + squareNormalsArray[i++]) ;
        }

        float squareColors[] = {
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,

                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,

                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                0.5f, 0.0f, 0.5f, 1.0f,
                0.5f, 0.0f, 0.5f, 1.0f,
                0.5f, 0.0f, 0.5f, 1.0f,
                0.5f, 0.0f, 0.5f, 1.0f,
                0.5f, 0.0f, 0.5f, 1.0f,
                0.5f, 0.0f, 0.5f, 1.0f,

                0.0f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.5f, 0.5f, 1.0f,
                0.0f, 0.5f, 0.5f, 1.0f,

                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f

        };

        /*ByteBuffer vbb = ByteBuffer.allocateDirect(
                squareCoords.length * 4
        );
        vbb.order(ByteOrder.nativeOrder());
        triangleVB = vbb.asFloatBuffer();
        //triangleVB.put(squareCoords);
        triangleVB.put(squareAndNorms);
        triangleVB.position(0);*/
        triangleVB = ByteBuffer.allocateDirect(squareCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVB.put(squareCoords).position(0);

        normalVB = ByteBuffer.allocateDirect(squareNormalsArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(squareNormalsArray).position(0);

        colorVB = ByteBuffer.allocateDirect(squareColors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorVB.put(squareColors).position(0);
    }


    private float[] generateTorus() {
        ArrayList<Float> torusArrayList = new ArrayList<Float>();
        float interval = (float) (Math.PI / 2);
       float r = 0.5f;
       float R = 0.75f;
       for(float u = 0; u < 2 * Math.PI; u += interval ) {
           iters++;
           for(float v = 0; v < 2 * Math.PI; v += interval ) {
               float x = (float) ((R + r * Math.cos(v)) * Math.cos(u));
               float y = (float) ((R + r * Math.cos(v)) * Math.sin(u));
               float z = (float) (r * Math.sin(v));
               torusArrayList.add(x);
               torusArrayList.add(y);
               torusArrayList.add(z);

           }
       }

        Log.d("Tiller", "iters: " + iters);
       float torusCoords[] = new float[torusArrayList.size()];
        for(int i = 0; i < torusArrayList.size(); i++) {
           torusCoords[i] = torusArrayList.get(i);
        }


        return torusCoords;
    }

    private float[] arrangeTorusCoords(float[] torusCoords) {
        int references[] = new int[3 * iters * iters];
        int refCount = 0;

        for(int i = 0; i < iters; i++) {
            for(int j = 0; j < iters; j++) {
                if( j % 2 == 0) {
                    references[refCount++] = i*iters + j;
                    references[refCount++] = i*iters + j + 1;
                    references[refCount++] = (i+1)*iters + j;
                } else {
                    references[refCount++] = i*iters + j;
                    references[refCount++] = (i+1)*iters + j;
                    references[refCount++] = (i+1)*iters + j+1;
                }
            }

        }

        Log.d("Tiller", "TorusCoords length: " + torusCoords.length);
        Log.d("Tiller", "references length: " + references.length);
        float referencedCoords[] = new float[3 * torusCoords.length];
        for(int i = 0; i < references.length; i++) {
            int point = i * 3;
            int coordPoint = references[i];
            referencedCoords[point++] = torusCoords[coordPoint++];
            referencedCoords[point++] = torusCoords[coordPoint++];
            referencedCoords[point] = torusCoords[coordPoint];

        }

        return referencedCoords;
    }

    public int loadShader(int type, String shaderStr) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderStr);
        GLES20.glCompileShader(shader);
        return shader;
    }

}

