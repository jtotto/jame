package com.github.jame;

import com.github.jame.TiltSensor;

import java.net.Socket;

import java.io.IOException;
import java.io.OutputStream;

public class MidiDataTransmitter extends Thread implements TiltSensor.TiltDataListener
{
	private int midiMin, midiMax, port;
	private float tiltMin, tiltMax, stepSize;
	private String ip;
	private Boolean invert;
	private ResponseModifier modifier;
	private TiltSensor.TiltListenerActivity mTiltActivity;
	
	private Socket socket;
	private OutputStream outputStream;
	
	private int previousValue;
	
	public MidiDataTransmitter(
			int midiMin
			,int midiMax
			,float tiltMin
			,float tiltMax
			,String ip
			,int port
			,Boolean invert
			,ResponseModifier modifier
			,TiltSensor.TiltListenerActivity tiltActivity)
	{
		this.midiMin = midiMin;
		this.midiMax = midiMax;
		this.stepSize = modifier.getTransformedValue(tiltMax - tiltMin) / (midiMax - midiMin); // Zero is implicitly subtracted from the numerator.
		this.tiltMin = tiltMin;
		this.tiltMax = tiltMax;
		this.ip = ip;
		this.port = port;
		this.invert = invert;
		this.modifier = modifier;
		this.mTiltActivity = tiltActivity;
		this.previousValue = 0;
	}
	
	public void run()
	{
		try
		{
			System.err.println(ip);
			System.err.println(port);
			System.err.println(mTiltActivity);
			socket = new Socket(ip, port);
			outputStream = socket.getOutputStream();
			mTiltActivity.registerTiltListener(this);
		}
		catch(Exception e)
		{
			System.err.println(e);
			throw new RuntimeException();
		}
	}
	
	@Override
	public void onTiltChanged(float tilt)
	{
		if(tilt < tiltMin) { tilt = tiltMin; }
		else if(tilt > tiltMax) { tilt = tiltMax; }
		
		tilt -= tiltMin; // Make the lowest possible value be 0.
		tilt = modifier.getTransformedValue(tilt); // Transform according to desired response.
		
		int midi = ((int) ((tilt / stepSize) + 0.5)) + midiMin; // The cast acts as the floor function.
		
		if(invert) { midi = midiMax - midi; }
		
		if(midi != previousValue)
		{
			try
			{
				outputStream.write(midi);
				previousValue = midi;
			}
			catch(IOException e)
			{
				mTiltActivity.unregisterTiltListener(); // Should probably notify the user.
			}
		}
	}
	
	public void close() throws IOException
	{
		if(mTiltActivity != null)
			mTiltActivity.unregisterTiltListener();
		if(outputStream != null)
			outputStream.close();
		if(socket != null)
			socket.close();
	}
}
