package com.ntcmplt;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

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

    private FloatBuffer triangleVB;
    private final String vertexShaderStr =
            "attribute vec4 vPosition; \n" +
            "void main() { \n" +
            "  gl_Position = vPosition; \n" +
            "}  \n";

    private final String fragmentShaderStr =
            "precision mediump float; \n" +
                    "void main() { \n" +
                    "  gl_FragColor = vec4(0.63671875, 0.76953125, 0.22265625, 1.0); \n" +
                    "}  \n";

    private int mProgram;
    private int mPositionHandle;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.5f,0.5f,0.5f, 1.0f);
        initShapes();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderStr);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderStr);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, triangleVB);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }


    private void initShapes() {
        float triangleCoords[] = {
                -0.5f, -0.25f, 0,
                0.5f, -0.25f, 0,
                0.0f, 0.559016994f, 0
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(
                triangleCoords.length * 4
        );
        vbb.order(ByteOrder.nativeOrder());
        triangleVB = vbb.asFloatBuffer();
        triangleVB.put(triangleCoords);
        triangleVB.position(0);
    }


    public int loadShader(int type, String shaderStr) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderStr);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
