package com.alexkang.loopboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.alexkang.loopboard.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class GraphSurfaceView extends View implements SensorEventListener {
	
	protected byte[] mSampleData;
	protected int mSampleSize;
	protected float mSampleLength;
	
	protected Bitmap mBitmap;
	protected Paint mPaint;

    private SharedPreferences prefs;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private boolean initialized = false;
    private float x, y, z, xLast, yLast, zLast;
    private float[] gravity = {0,0,0};
    private final float FILTER_GRAVITY = 0.8f;
    private final float FILTER_NOISE = 0.5f;
    private int SENSOR_TIME = 500;
    private int BACKGROUND_COLOR = Color.BLACK;


	public GraphSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	public GraphSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public GraphSurfaceView(Context context) {
		super(context);
		init(context);
	}
	
	protected void init(Context ctx) {
		mPaint = new Paint();
		mBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_launcher);

        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        sensorManager = (SensorManager) ctx.getSystemService(ctx.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_NORMAL);
	}

	public void setData(byte[] data, int sampleSize, float sampleLength) {
		this.mSampleData = data;
		this.mSampleSize = sampleSize;
		this.mSampleLength = sampleLength;
		invalidate();


        mPaint.setColor(this.getRGBfromXYZ());
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
        this.getPreferences();
		canvas.drawColor(BACKGROUND_COLOR);
		
		if (mSampleData != null) {
			int numPoints = canvas.getWidth();
			int step = mSampleSize / numPoints;
			int halfHeight = canvas.getHeight() / 2;
	
			float[] points = new float[numPoints * 4];		// a line = 4 points: x0,y0,x1,y1
			float oldX = 0.0f;
			float oldY = halfHeight;
			
			for (int i = 0; i < numPoints; i++) {
				byte valHi = mSampleData[i*step];
                byte valLow = mSampleData[i*step+1];
                short val = (short)(valHi + Byte.MAX_VALUE * valLow);

				float y = (((float)val / Short.MAX_VALUE) * halfHeight) + halfHeight;
				points[i*4+0] = oldX;
				points[i*4+1] = oldY;
				
				points[i*4+2] = i+1;
				points[i*4+3] = y;

                oldX = i;
                oldY = y;
			}
			canvas.drawLines(points, mPaint);

		}

	}

    private void getPreferences() {
        BACKGROUND_COLOR = Color.parseColor(prefs.getString("prefColor","BLACK"));
        SENSOR_TIME = Integer.parseInt(prefs.getString("prefRefreshTime","500"));
    }

    private int getRGBfromXYZ() {
        int R = Math.round(255 * this.x);
        int G = Math.round(255 * this.y);
        int B = Math.round(255 * this.z);

        R = (R << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        G = (G << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
        B = B & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | R | G | B; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;

        if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > SENSOR_TIME) {
                lastUpdate = curTime;

                gravity[0] = FILTER_GRAVITY * gravity[0] + (1 - FILTER_GRAVITY) * event.values[0];
                gravity[1] = FILTER_GRAVITY * gravity[1] + (1 - FILTER_GRAVITY) * event.values[1];
                gravity[2] = FILTER_GRAVITY * gravity[2] + (1 - FILTER_GRAVITY) * event.values[2];

                x = event.values[0] - gravity[0];
                y = event.values[1] - gravity[1];
                z = event.values[2] - gravity[2];

                if (!initialized) {
                    xLast = x;
                    yLast = y;
                    zLast = z;
                    initialized = true;
                } else {
                    float deltaX = Math.abs(xLast - x);
                    float deltaY = Math.abs(yLast - y);
                    float deltaZ = Math.abs(zLast - z);

                    if (deltaX < FILTER_NOISE)
                        x = xLast;
                    if (deltaY < FILTER_NOISE)
                        y = yLast;
                    if (deltaZ < FILTER_NOISE)
                        z = zLast;

                    xLast = x;
                    yLast = y;
                    zLast = z;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
