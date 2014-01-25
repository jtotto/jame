package com.github.jame.Fragments;

import com.github.jame.TiltSensor;
import com.github.jame.R;

import android.app.Activity;
import android.app.DialogFragment;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

public class CalibrationFragment extends DialogFragment implements TiltSensor.TiltDataListener
{
	private String mPrompt;
	private int mFlag;
	private OnCalibrationCompletedListener mCallback;
	private TiltSensor.TiltListenerActivity mTiltActivity; // In practice, refers to the same activity as above.  However, they're conceptually unrelated and are stored separately for clarity.
	private float[] calibrationData;
	private int calibrationIndex;
	private final int SAMPLE_SIZE = 50;
	
	public interface OnCalibrationCompletedListener
	{
		public void receiveCalibrationBound(int flag, float bound);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mPrompt = getArguments().getString("prompt");
		mFlag = getArguments().getInt("flag");
		calibrationIndex = 0;
		calibrationData = new float[SAMPLE_SIZE];
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View inflatedLayout = inflater.inflate(R.layout.fragment_calibrationmanager, container, false);
		((TextView) inflatedLayout.findViewById(R.id.calibrate_prompt)).setText(mPrompt);
		final TiltSensor.TiltDataListener listener = this; // Ewwwwwwww.
		((Button) inflatedLayout.findViewById(R.id.calibrate_start)).setOnClickListener(new View.OnClickListener() 
		{	
			@Override
			public void onClick(View v) 
			{
				mTiltActivity.registerTiltListener(listener);
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
            mCallback = (OnCalibrationCompletedListener) activity;
            mTiltActivity = (TiltSensor.TiltListenerActivity) activity;
        } 
        catch (ClassCastException e) 
        {
            throw new ClassCastException(activity.toString() + " must implement OnCalibrationCompletedListener and TiltSensor.TiltListenerActivity.");
        }
    }
	
	@Override 
	public void onTiltChanged(float data)
	{
		if(calibrationIndex < SAMPLE_SIZE)
		{
			calibrationData[calibrationIndex] = data;
			calibrationIndex++;
		}
		else
		{
			mTiltActivity.unregisterTiltListener();
			mCallback.receiveCalibrationBound(mFlag, mean(calibrationData));
		}
	}
	
	private static float mean(float [] data)
	{
		float result = 0;
		for(int i = 0; i < data.length; i++)
		{
			result += data[i];
		}
		return result / data.length;
	}
}
