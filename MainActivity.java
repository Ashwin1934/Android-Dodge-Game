package com.example.ashud.dodgegame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    GameSurface gameSurface;
    SensorManager sensorManager;
    TextView xValue,yValue;
    double xAcceleration;
    Bitmap player;
    Drawable russel, tackler;
    boolean stop = false;
    int score = 0, val = 0;
    int start = -900;
    int rand;
    int time;
    SoundPool soundPool;
    int soundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);

        setContentView(gameSurface);

        xValue = (TextView)findViewById(R.id.xValue);
        yValue = (TextView)findViewById(R.id.yValue);

        russel = getResources().getDrawable(R.drawable.russel);
        tackler = getResources().getDrawable(R.drawable.tackler2);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long l) {
                time=(int)l/1000;
            }

            @Override
            public void onFinish() {
                time=0;
                stop = true;
            }
        }.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            xAcceleration = -1.5 * sensorEvent.values[0];
            Log.d("XACCELERATION", String.valueOf(xAcceleration));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class GameSurface extends SurfaceView implements Runnable {

        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap myImage, myImage2, myImage3, saveImage1, saveImage3;
        Paint paintProperty;

        int screenWidth;
        int screenHeight;

        int value = 5, speedTackler = 5;

        public GameSurface(Context context) {
            super(context);

            holder=getHolder();

            myImage = BitmapFactory.decodeResource(getResources(),R.drawable.russel);
            myImage2 = BitmapFactory.decodeResource(getResources(),R.drawable.tackler2);
            myImage3 = BitmapFactory.decodeResource(getResources(), R.drawable.russelhit);

            saveImage1 = myImage;
            saveImage3 = myImage3;


            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth=sizeOfScreen.x;
            screenHeight=sizeOfScreen.y;

            paintProperty= new Paint();
            paintProperty.setTextSize(70);

            soundPool=new SoundPool(1,AudioManager.STREAM_MUSIC,0);
            soundID=soundPool.load(MainActivity.this,R.raw.soundeffect,100);
        }

        @Override
        public void run() {

            rand = (int) (Math.random() * 590 + 1);

            gameSurface.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(speedTackler == 5)
                        speedTackler =20;
                    else
                        speedTackler=5;
                }
            });

            while (running){
                if (!holder.getSurface().isValid())
                    continue;

                Canvas canvas= holder.lockCanvas();
                canvas.drawRGB(0,255,0);
                canvas.drawText("Score: " + score,50,100,paintProperty);
                canvas.drawText("Time Left: "+time,50,230,paintProperty);

                value += 2.1 * xAcceleration;

                if(stop) {
                    canvas.drawText("GAME OVER", 80, 350, paintProperty);
                    holder.unlockCanvasAndPost(canvas);
                }

                else {

                    if (120 + value >= 0 && 120 + value <= screenWidth - 75)
                        canvas.drawBitmap(myImage, 120 + value, 700, null);
                    else if (value < -20) {
                        value = -18;
                        canvas.drawBitmap(myImage, 120 + value, 700, null);
                    } else if (value > screenWidth - 300) {
                        value = screenWidth - 302;
                        canvas.drawBitmap(myImage, value + 120, 700, null);
                    }

                    myImage = saveImage1;


                    Rect russelRect = new Rect(value + 120, 700, myImage.getWidth() + value + 120, 700 + myImage.getHeight());
                    Rect tacklerRect;

                    if (start <= screenHeight - 10) {
                        tacklerRect = new Rect(rand, start, rand + myImage2.getWidth(), start + myImage2.getHeight());

                        canvas.drawBitmap(myImage2, rand, start, null);
                        start += 10 * speedTackler / 5;
                    } else {
                        tacklerRect = new Rect(rand, start, rand + myImage2.getWidth(), start + myImage2.getHeight());

                        rand = (int) (Math.random() * 590 + 1); //new value
                        start = -900;
                    }

                    if (russelRect.intersect(tacklerRect)) {
                        //SoundPool sound = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
                        //int soundId = sound.load(MainActivity.this,R.raw.soundeffect,1);

                        myImage = saveImage3;
                        soundPool.play(soundID,0,1,0,0,.4f);

                    } else if (!russelRect.intersect(tacklerRect) && start == 700 + myImage.getHeight() - myImage.getHeight() % 10) {
                        score++;
                    }

                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        public void resume(){
            running=true;
            gameThread=new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}






