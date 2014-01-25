package com.github.jame;

public class TiltSensor
{
	public interface TiltListenerActivity
	{
		public void registerTiltListener(TiltDataListener listener);
		public void unregisterTiltListener();
	}
	
	public interface TiltDataListener
	{
		public void onTiltChanged(float data);
	}
}
