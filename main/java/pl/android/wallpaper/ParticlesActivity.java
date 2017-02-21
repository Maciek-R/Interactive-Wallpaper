package pl.android.wallpaper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class ParticlesActivity extends Activity {

    private GLSurfaceView glSurfaceView;
    private Boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        glSurfaceView = new GLSurfaceView(this);

        final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        final ParticlesRenderer particlesRenderer = new ParticlesRenderer(this);

        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("emulator")
                || Build.MODEL.contains("Android SDK built for x86")));

        if(supportsEs2){
            glSurfaceView.setEGLContextClientVersion(2);

            glSurfaceView.setRenderer(particlesRenderer);
            rendererSet = true;
        }
        else{
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.", Toast.LENGTH_LONG).show();
            return;
        }


        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            float previousX, previousY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event!=null){

                    final int pointerIndex = event.getActionIndex();
                    final float normalizedX = (event.getX(pointerIndex) / (float) view.getWidth()) * 2 - 1;         // to range (-1; 1)
                    final float normalizedY = -((event.getY(pointerIndex) / (float) view.getHeight()) * 2 - 1);

                        switch(event.getActionMasked()){
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_POINTER_DOWN:


                                previousX = event.getX(pointerIndex);
                                previousY = event.getY(pointerIndex);

                                glSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                        particlesRenderer.handleTouchPress(normalizedX, normalizedY);
                                        particlesRenderer.checkifShooterisTouched(normalizedX, normalizedY);

                                    }
                                });

                                break;
                            case MotionEvent.ACTION_MOVE:
                                final float deltaX = event.getX(pointerIndex) - previousX;
                                final float deltaY = event.getY(pointerIndex) - previousY;

                                    previousY = event.getY(pointerIndex);
                                    previousX = event.getX(pointerIndex);

                                if(event.getPointerCount()==1) {
                                    glSurfaceView.queueEvent(new Runnable() {
                                        @Override
                                        public void run() {
                                            particlesRenderer.handleTouchDrag(normalizedX, normalizedY);
                                            particlesRenderer.handleMoveCamera(deltaX, deltaY);
                                        }
                                    });
                                }
                                break;
                        }


                    return true;
                }
                else{
                    return false;
                }
            }
        });

        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(rendererSet){
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(rendererSet){
            glSurfaceView.onResume();
        }
    }
}
