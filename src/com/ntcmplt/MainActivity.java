package com.ntcmplt;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;

public class MainActivity extends Activity
{
    private MSurfaceView mSurfaceView;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mSurfaceView = new MSurfaceView(this);
        setContentView(mSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    class MSurfaceView extends GLSurfaceView {

        MRenderer mRenderer;
        public MSurfaceView(Context context) {
            super(context);

            setEGLContextClientVersion(2);
            mRenderer = new MRenderer();
            setRenderer(mRenderer);
            //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
               mRenderer.touched = true;
                mRenderer.touch = event;
            }

            if(event.getAction() == MotionEvent.ACTION_UP) {
                mRenderer.touched = false;
                mRenderer.touch = null;
            }


            //Log.d("TILLER", "Horizontal Angle out: " + mRenderer.horizontalAngle);
            //requestRender();
            return true;
        }
    }
}
