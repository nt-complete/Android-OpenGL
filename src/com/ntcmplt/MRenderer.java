package com.ntcmplt;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 4/10/12
 * Time: 7:54 PM
 */
public class MRenderer implements GLSurfaceView.Renderer {

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
            "	float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n " +
            "	//diffuse *= (1.0 / (1.0 + (0.25 * distance * distance)));\n " +
            "\n " +
            "	varColor = vColor * diffuse ;\n " +
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

    private int mProgram;
    private int mPositionHandle, mMVMatrixHandle, mMVPMatrixHandle, mLightPosHandle, mColorHandle, mNormalHandle;
    private float[] mMVPMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mMVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float angle = 0.0f;
    public float horizontalAngle = 0.0f;
    private float zTrans = 0.0f;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        //GLES20.glCullFace(GLES20.GL_FRONT);
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
        //mPositionHandle = GLES20.glGetUniformLocation(mProgram, "vPosition");
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

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
        horizontalAngle += 1.0f;
        zTrans += 0.05;

        Matrix.setIdentityM(mMMatrix, 0);
        Matrix.setRotateM(mMMatrix, 0, angle, 1.0f, 0, 0);
        Matrix.setRotateM(mMMatrix, 0, horizontalAngle, 1.0f, 1.0f, 0);
        //Matrix.translateM(mMMatrix,0,0,zTrans,0);
        Matrix.multiplyMM(mMVMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        //GLES20.glUniform3f(mLightPosHandle, -3.0f, 5.0f, 5.0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }


    private void initShapes() {
        float squareCoords[] = {
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
        };


        float squareNormals[] = {
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,

                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,

                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,

                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f

        };

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

        ByteBuffer vbb = ByteBuffer.allocateDirect(
                squareCoords.length * 4
        );
        vbb.order(ByteOrder.nativeOrder());
        triangleVB = vbb.asFloatBuffer();
        triangleVB.put(squareCoords);
        triangleVB.position(0);

        normalVB = ByteBuffer.allocateDirect(squareNormals.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalVB.put(squareNormals).position(0);

        colorVB = ByteBuffer.allocateDirect(squareColors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorVB.put(squareColors).position(0);
    }




    public int loadShader(int type, String shaderStr) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderStr);
        GLES20.glCompileShader(shader);
        return shader;
    }

}

