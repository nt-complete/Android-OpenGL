package com.ntcmplt;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

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

        public MSurfaceView(Context context) {
            super(context);

            setEGLContextClientVersion(2);
            setRenderer(new MRendererLight());
        }
    }
}
