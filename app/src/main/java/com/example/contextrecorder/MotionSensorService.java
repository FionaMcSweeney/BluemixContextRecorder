package com.example.contextrecorder;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.IBinder;
import android.util.Log;

@SuppressLint("NewApi")
public class MotionSensorService extends Service implements SensorEventListener{
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private Sensor mPressure;

	private TriggerEventListener mTriggerEventListener;	

	@Override
	public void onCreate(){
	
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		
		mTriggerEventListener = new TriggerEventListener() {
		    @Override
		    public void onTrigger(TriggerEvent event) {
		    	Log.i("Event", event.toString());
		        // Do work
		    }
		};
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//if (event.sensor.getName())
		float millibars_of_pressure = event.values[0];

		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
		

}
