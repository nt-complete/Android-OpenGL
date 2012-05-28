package com.ntcmplt;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MainActivity extends Activity
{
    private MSurfaceView mSurfaceView;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //mSurfaceView = new MSurfaceView(this);
        setContentView(R.layout.main);
        mSurfaceView = (MSurfaceView) findViewById(R.id.surface_view);
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

    public static class MSurfaceView extends GLSurfaceView {

        MRenderer mRenderer;
        public MSurfaceView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);

            setEGLContextClientVersion(2);
            mRenderer = new MRenderer(context);
            setRenderer(mRenderer);
            //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
               mRenderer.touched = true;
                mRenderer.touch = event;
                mRenderer.updated = false;
                /*if(event.getX() > mRenderer.width / 2) {
                    //mRenderer.intervalNum += 1.0;
                    Log.d("Tiller", "IntervalNum: " + mRenderer.intervalNum);
                    //mRenderer.updateShapes();
                } else {
                    if(mRenderer.intervalNum > 2) {
                        //mRenderer.intervalNum -= 1.0;
                        Log.d("Tiller", "IntervalNum: " + mRenderer.intervalNum);
                        mRenderer.updateShapes();
                    }
                }*/
            }

            if(event.getAction() == MotionEvent.ACTION_UP) {
                mRenderer.touched = false;
                //mRenderer.touch = event;
                mRenderer.updated = true;
            }


            //Log.d("TILLER", "Horizontal Angle out: " + mRenderer.horizontalAngle);
            //requestRender();
            return true;
        }

    }
}
