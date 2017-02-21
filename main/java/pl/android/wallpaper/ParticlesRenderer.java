package pl.android.wallpaper;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.util.Log;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pl.android.wallpaper.objects.ParticleShooter;
import pl.android.wallpaper.objects.ParticleSystem;
import pl.android.wallpaper.objects.Skybox;
import pl.android.wallpaper.programs.ParticleShaderProgram;
import pl.android.wallpaper.programs.SkyboxShaderProgram;
import pl.android.wallpaper.util.Geometry;
import pl.android.wallpaper.util.Geometry.*;
import pl.android.wallpaper.util.LoggerConfig;
import pl.android.wallpaper.util.MatrixHelper;
import pl.android.wallpaper.util.TextureHelper;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Maciek on 2017-02-12.
 */

public class ParticlesRenderer implements Renderer {

    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];

    private long frameStartTimeMs;
    private static final String TAG = "ParticlesRenderer";
    private long startTimeMs;
    private int frameCount;

    private ParticleShaderProgram particleProgram;
    private ParticleSystem particleSystem;

  //  private ParticleShooter redParticleShooter;
   // private ParticleShooter greenParticleShooter;
   // private ParticleShooter blueParticleShooter;
    private ParticleShooter[] particleShooters;
    int createdParticlesShooters = 3;
    int counterShooters = 3;
    final int MAX_COUNT = 10;

    private ParticleShooter blueFirework;
    private ParticleShooter greenFirework;
    private ParticleShooter redFirework;

    private SkyboxShaderProgram skyboxProgram;
    private Skybox skybox;
    private int skyboxTexture;

    private long globalStartTime;

    private int particleTexture;
    private final Random random = new Random();

    private long previousFireworkTime;
    private int nextFireworkInSec;
    private float xRotation, yRotation;


    public ParticlesRenderer(Context context){
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

       // glEnable(GL_BLEND);
      //  glBlendFunc(GL_ONE, GL_ONE);

        final float angleVarianceInDegrees = 20f;
        final float speedVariance = 1f;

        particleProgram = new ParticleShaderProgram(context);
        particleSystem = new ParticleSystem(5000);
        globalStartTime = System.nanoTime();
        previousFireworkTime = globalStartTime;
        nextFireworkInSec = random.nextInt(3);

        skyboxProgram = new SkyboxShaderProgram(context);
        skybox = new Skybox();
        skyboxTexture = TextureHelper.loadCubeMap(context,
                new int[] { R.drawable.night_left, R.drawable.night_right,
                        R.drawable.night_bottom, R.drawable.night_top,
                        R.drawable.night_front, R.drawable.night_back});

        final Vector particleDirection = new Vector(0f, 0.5f, 0f);

        particleShooters = new ParticleShooter[10];
        particleShooters[0] = new ParticleShooter(
                new Point(-0.8f, 0f, -4.0f),
                particleDirection,
                Color.rgb(255, 50, 5), angleVarianceInDegrees, speedVariance);
        particleShooters[1] = new ParticleShooter(
                new Point(0f, 0f, -5f),
                particleDirection,
                Color.rgb(25, 255, 25), angleVarianceInDegrees, speedVariance);
        particleShooters[2] = new ParticleShooter(
                new Point(0.8f, 0f, -4.0f),
                particleDirection,
                Color.rgb(5, 50, 255), angleVarianceInDegrees, speedVariance);

        blueFirework = new ParticleShooter(new Point(-1f, 2.5f, 0f), particleDirection, Color.rgb(10, 10, 255), 360f, 1f);
        greenFirework = new ParticleShooter(new Point(0f, 3f, 0f), particleDirection, Color.rgb(10, 255, 10), 360f, 1f);
        redFirework = new ParticleShooter(new Point(1f, 2.5f, 0f), particleDirection, Color.rgb(255, 10, 10), 360f, 1f);

        particleTexture = TextureHelper.loadTexture(context, R.drawable.particle_texture);


    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        glViewport(0, 0, width, height);


        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);

       // setIdentityM(viewMatrix, 0);
       /* if(width < height) {
            translateM(viewMatrix, 0, 0f, -1.5f, -5f);
        }
        else{
            translateM(viewMatrix, 0, 0f, -0.75f, -2f);
        }*/

      //  translateM(viewMatrix, 0, 0f, -1.5f, 0f);

      //  multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0,
       //         viewMatrix, 0);
       // invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        limitFrameRate(24);
        logFrameRate();
        glClear(GL_COLOR_BUFFER_BIT);


        drawSkybox();
        drawParticles();

    }

    public void handleTouchPress(float normalizedX, float normalizedY){
        if(LoggerConfig.ON){
            Log.v("RENDERER", "Touch Press");
        }
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

        final Point point = Geometry.getWorldPointFromNormalized(invertedViewProjectionMatrix, normalizedX, normalizedY, 0.5f);

        randColorFirework(point, currentTime);

    }
    public void handleTouchDrag(float deltaX, float deltaY){
        if(LoggerConfig.ON){
            Log.v("RENDERER", "Touch Drag");
        }


    }


    private void drawParticles(){
        float time = (System.nanoTime() - previousFireworkTime) / 1000000000f;
        if(time >= nextFireworkInSec) {
            randFirework();
            previousFireworkTime = System.nanoTime();
            nextFireworkInSec = random.nextInt(3);
        }

        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

        for(int i=0; i<createdParticlesShooters; ++i){
            if(particleShooters[i].isON())
                particleShooters[i].addParticles(particleSystem, currentTime, 5);
        }

        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        translateM(viewMatrix, 0, 0f, -1.5f, 0f);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0,
                viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);

        particleProgram.useProgram();
        particleProgram.setUniforms(viewProjectionMatrix, currentTime, particleTexture);
        particleSystem.bindData(particleProgram);
        particleSystem.draw();

        glDisable(GL_BLEND);
    }
    private void drawSkybox() {
        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        skyboxProgram.useProgram();
        skyboxProgram.setUniforms(viewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        skybox.draw();
    }


    private void randFirework(){
        final float x = random.nextFloat() - 0.5f;
        final float y = random.nextFloat() - 0.5f;

        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

        Point point = Geometry.getWorldPointFromNormalized(invertedViewProjectionMatrix, x, y, 0.5f);

        randColorFirework(point, currentTime);


    }
    private void randColorFirework(Point point, float currentTime){
        int colorId = random.nextInt(3);
        final int count = 500;
        if(colorId == 0){
            blueFirework.changePosition(point);
            blueFirework.addParticles(particleSystem, currentTime, count);
        }
        else if(colorId == 1){
            greenFirework.changePosition(point);
            greenFirework.addParticles(particleSystem, currentTime, count);
        }
        else{
            redFirework.changePosition(point);
            redFirework.addParticles(particleSystem, currentTime, count);
        }
    }
    private int randColor(){
        int colorId = random.nextInt(3);

        if(colorId==0)
            return Color.rgb(255, 50, 5);
        else if(colorId==1)
            return Color.rgb(25, 255, 25);
        else
            return Color.rgb(5, 50, 255);


    }

    private void limitFrameRate(int framesPerSecond){
        long elapsedFrameTimeMs = SystemClock.elapsedRealtime() - frameStartTimeMs;
        long expectedFrameTimeMs = 1000 / framesPerSecond;
        long timeToSleepMs = expectedFrameTimeMs - elapsedFrameTimeMs;

        if(timeToSleepMs > 0){
            SystemClock.sleep(timeToSleepMs);
        }
        frameStartTimeMs = SystemClock.elapsedRealtime();
    }
    private void logFrameRate() {
        if(LoggerConfig.ON){
            long elapsedRealTimeMs = SystemClock.elapsedRealtime();
            double elapsedSeconds = (elapsedRealTimeMs - startTimeMs) / 1000.0;

            if(elapsedSeconds > 1.0){
                Log.v(TAG, frameCount / elapsedSeconds + "fps");
                startTimeMs = SystemClock.elapsedRealtime();
                frameCount = 0;
            }
            frameCount++;
        }
    }
    public void handleMoveCamera(float deltaX, float deltaY){
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;

        if (yRotation < -20) {
            yRotation = -20;
        } else if (yRotation > 20) {
            yRotation = 20;
        }
    }
    public void checkifShooterisTouched(float normalizedX, float normalizedY){
        final Point point = Geometry.getWorldPointFromNormalized(invertedViewProjectionMatrix, normalizedX, normalizedY, 0.5f);

        System.out.println(point.x + " " + point.y +" "+point.z);

        Ray ray = Geometry.convertNormalized2DPointToRay(normalizedX, normalizedY, invertedViewProjectionMatrix);
        boolean anyTouched = false;
        for(int i=0; i<createdParticlesShooters; ++i){
            Sphere sphere = new Sphere(new Point(particleShooters[i].getX(), particleShooters[i].getY(), particleShooters[i].getZ()), 0.2f);
            if(Geometry.intersects(sphere, ray)){
                anyTouched = true;
                if(particleShooters[i].isON()){
                    particleShooters[i].switchOff();
                }
                else{
                    particleShooters[i].switchOn();
                }
            }
        }
        if(!anyTouched){
            if(++createdParticlesShooters > MAX_COUNT) createdParticlesShooters=MAX_COUNT;
            particleShooters[counterShooters] = new ParticleShooter(
                    point,
                    new Vector(0f, 0.5f, 0f),
                    randColor(), 20f, 1f);

            if(++counterShooters == MAX_COUNT) counterShooters = 0;
        }

    }

}
