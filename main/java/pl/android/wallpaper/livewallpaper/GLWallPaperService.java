package pl.android.wallpaper.livewallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import pl.android.wallpaper.ParticlesRenderer;

/**
 * Created by Maciek on 2017-02-17.
 */

public class GLWallPaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine{
        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererSet;
        private ParticlesRenderer particlesRenderer;
        int WIDTH_SCREEN;
        int HEIGHT_SCREEN;
        float previousX = 0, previousY = 0;
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setTouchEventsEnabled(true);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallPaperService.this);
            final Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            WIDTH_SCREEN = display.getWidth();
            HEIGHT_SCREEN = display.getHeight();




            // Check if the system supports OpenGL ES 2.0.
            ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configurationInfo = activityManager
                    .getDeviceConfigurationInfo();
            final boolean supportsEs2 =
                    configurationInfo.reqGlEsVersion >= 0x20000
            // Check for emulator.
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                            && (Build.FINGERPRINT.startsWith("generic")
                            || Build.FINGERPRINT.startsWith("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")));

            particlesRenderer = new ParticlesRenderer(GLWallPaperService.this);

            if(supportsEs2){
                glSurfaceView.setEGLContextClientVersion(2);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    glSurfaceView.setPreserveEGLContextOnPause(true);
                }
                glSurfaceView.setRenderer(particlesRenderer);
                rendererSet = true;
            }
            else{
                Toast.makeText(GLWallPaperService.this,
                        "This device does not support OpenGL ES 2.0.",
                        Toast.LENGTH_LONG).show();
                return;
            }

        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            if(event!=null){

                // to range (-1; 1)
                final int pointerIndex = event.getActionIndex();
                final float normalizedX = (event.getX(pointerIndex) / WIDTH_SCREEN) * 2 - 1;
                final float normalizedY = -((event.getY(pointerIndex) / HEIGHT_SCREEN) * 2 - 1);


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



            }

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if(rendererSet){
                if(visible){
                    glSurfaceView.onResume();
                }
                else{
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onWallpaperDestroy();
        }

        class WallpaperGLSurfaceView extends GLSurfaceView{
            WallpaperGLSurfaceView(Context context){
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();

            }
            public void onWallpaperDestroy(){
                super.onDetachedFromWindow();
            }
        }

    }


}
