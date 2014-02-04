/* JAME Android MIDI Expression project.
Copyright (C) 2014  Joshua Otto

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */

package com.github.jame;

import com.github.jame.TiltSensor;
import com.github.jame.Fragments.CalibrationFragment;
import com.github.jame.Fragments.MidiControlFragment;
import com.github.jame.Fragments.PrefsFragment;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class JAMEMainActivity extends Activity implements 
					MidiControlFragment.OnCalibrationInitiatedListener, 
					CalibrationFragment.OnCalibrationCompletedListener, 
					SensorEventListener, 
					TiltSensor.TiltListenerActivity 
{	
	private SensorManager mSensorManager;
	private Sensor gravitySensor;
	private TiltSensor.TiltDataListener mTiltListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jamemain);

        if (findViewById(R.id.control_container) != null) 
        {
            if (savedInstanceState != null) 
            {
                return;
            }
            
            getFragmentManager().beginTransaction().add(R.id.control_container, new MidiControlFragment(), "control").commit();
        }
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mTiltListener = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.jamemain, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    		case R.id.action_settings:
    			FragmentTransaction transaction = getFragmentManager().beginTransaction();

    			transaction.replace(R.id.control_container, new PrefsFragment());
    			transaction.addToBackStack(null);
    			transaction.commit();
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }
	
	@Override
	public void getCalibrationBound(int flag, String prompt)
	{
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		
		CalibrationFragment calibrationFragment = new CalibrationFragment();
		Bundle args = new Bundle();
		args.putString("prompt", prompt);
		args.putInt("flag", flag);
		calibrationFragment.setArguments(args);
		
		transaction.add(0, calibrationFragment, Integer.toString(flag));
		transaction.commit();
	}
	
	@Override
	public void receiveCalibrationBound(int flag, float data)
	{
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		
		CalibrationFragment calibrationFragment = (CalibrationFragment) fragmentManager.findFragmentByTag(Integer.toString(flag));
		transaction.remove(calibrationFragment);
		transaction.commit();
		
		((MidiControlFragment) fragmentManager.findFragmentByTag("control")).recordCalibration(flag, data);
	}
	
	@Override
	public void registerTiltListener(TiltSensor.TiltDataListener listener)
	{
		mTiltListener = listener;
		mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
	}
	
	@Override
	public void unregisterTiltListener()
	{
		mSensorManager.unregisterListener(this);
		mTiltListener = null;
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(mTiltListener != null)
		{	
			mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		System.err.println("Accuracy changed..."); // Do I care?
	}
	
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		mTiltListener.onTiltChanged((float)Math.atan(event.values[1]/event.values[2]));
	}
}
