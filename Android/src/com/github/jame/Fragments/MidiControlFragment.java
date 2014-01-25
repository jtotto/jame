package com.github.jame.Fragments;

import android.app.Activity;
import android.app.Fragment;

import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.jame.MidiDataTransmitter;
import com.github.jame.R;
import com.github.jame.ResponseModifier;
import com.github.jame.TiltSensor;

import java.io.IOException;

public class MidiControlFragment extends Fragment
{
	private NumberPicker midiMax, midiMin;
	// Also store the tilt calibration information here, because it is managed from this fragment.
	private float minTilt, maxTilt;
	private OnCalibrationInitiatedListener mCallback;
	private TiltSensor.TiltListenerActivity mTiltActivity; // Needed for the transmitter thread.  The same object as mCallback, but conceptually different.
	private Context applicationContext; // Needed for Toasts.  The same object as mCallback, but conceptually different.
	private MidiDataTransmitter transmitter;
	
	
	// These flags are passed around as part of the handshake, but only meaningful within this fragment.
	private static final int CAL_MIN_FLAG = 0;
	private static final int CAL_MAX_FLAG = 1;
	
	public interface OnCalibrationInitiatedListener
	{
		public void getCalibrationBound(int flag, String prompt);
	}
	
	public void recordCalibration(int flag, float result)
	{
		// I tried to implement this in a reasonably general way to allow for easier future improvement - ie. this can be expanded to accommodate other calibrated parameters.
		switch(flag)
		{
			case CAL_MIN_FLAG:
				minTilt = result;
				mCallback.getCalibrationBound(CAL_MAX_FLAG, getString(R.string.calibration_max_prompt));
				break;
			case CAL_MAX_FLAG:
				maxTilt = result;
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
        // Inflate the layout for this fragment
        View inflatedLayout = inflater.inflate(R.layout.fragment_midicontrol, container, false);
        
        midiMax = (NumberPicker)inflatedLayout.findViewById(R.id.select_midi_max);
        midiMin = (NumberPicker)inflatedLayout.findViewById(R.id.select_midi_min);
        configurePickerForMidi(midiMax);
        configurePickerForMidi(midiMin);
        
        ((Button)inflatedLayout.findViewById(R.id.calibrate_button)).setOnClickListener(new View.OnClickListener() 
        {	
			@Override
			public void onClick(View v) 
			{
				mCallback.getCalibrationBound(CAL_MIN_FLAG, getString(R.string.calibration_min_prompt));
			}
		});
        
        final ToggleButton inversion = (ToggleButton) inflatedLayout.findViewById(R.id.toggle_inversion_button);
        ((ToggleButton) inflatedLayout.findViewById(R.id.send_midi_button)).setOnCheckedChangeListener
        (new CompoundButton.OnCheckedChangeListener() 
        {
        	@Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
            {
                if (isChecked) 
                {
                    // First, validate the session settings.
                	if(!(midiMin.getValue() < midiMax.getValue()))
                	{
                		toast("Minimum controller value must be lower than maximum! (have you tried the inversion button?)");
                		return;
                	}
                	else if(!(minTilt < maxTilt))
                	{
                		toast("Invalid calibration: minimum tilt must be greater than maximum tilt! (have you tried the inversion button?)");
                		return;
                	}
                	
                	// Next, read the stored preferences.
                	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
                	String ip = preferences.getString("pref_server_ip", "");
                	int port = Integer.parseInt(preferences.getString("pref_server_port", "1337"));
                	ResponseModifier modifier = ResponseModifier.getResponseModifier(preferences.getString("pref_tilt_response", "linear"));
                	
                	// Begin transmitting data.
                	Thread.UncaughtExceptionHandler transmissionHandler = new Thread.UncaughtExceptionHandler() 
                	{
                	    public void uncaughtException(Thread thread, Throwable exception) 
                	    {
                	        toast("Connection Error: " + exception.toString());
                	    }
                	};
                	
                	transmitter = new MidiDataTransmitter(
                			midiMin.getValue()
                			,midiMax.getValue()
                			,minTilt
                			,maxTilt
                			,ip
                			,port
                			,inversion.isChecked()
                			,modifier
                			,mTiltActivity
                	);
                	// transmitter.setUncaughtExceptionHandler(transmissionHandler);
                	transmitter.start(); // Aaaaaaand go!
                } 
                else 
                {
                    if(transmitter != null)
                    {
                    	try
                    	{
                    		transmitter.close();
                    	}
                    	catch(IOException e)
                    	{
                    		toast("Unable to close transmitter.");
                    	}
                    }
                }
            }
        });
        
        return inflatedLayout;
    }
	
	@Override
    public void onAttach(Activity activity) 
	{
        super.onAttach(activity);
        
        try 
        {
            mCallback = (OnCalibrationInitiatedListener) activity;
            mTiltActivity = (TiltSensor.TiltListenerActivity) activity;
        } 
        catch (ClassCastException e) 
        {
            throw new ClassCastException(activity.toString() + " must implement OnCalibrationInitiatedListener and TiltSensor.TiltListenerActivity.");
        }
        
        applicationContext = activity.getApplicationContext();
    }
	
	private void configurePickerForMidi(NumberPicker picker)
	{
		picker.setMaxValue(127);
		picker.setMinValue(0);
	}
	
	private void toast(CharSequence message)
	{
		Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show();
	}
}