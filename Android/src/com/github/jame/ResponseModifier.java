package com.github.jame;

public enum ResponseModifier 
{
	LINEAR 
	{
		@Override
		public float getTransformedValue(float data) { return data; }
	}
	,QUADRATIC
	{
		@Override
		public float getTransformedValue(float data) { return data * data; }
	}
	,ROOT
	{
		@Override
		public float getTransformedValue(float data) { return (float) Math.sqrt(data); }
	};
	
	public static ResponseModifier getResponseModifier(String name)
	{
		if(name.equals("quadratic")) { return ResponseModifier.QUADRATIC; }
		else if(name.equals("root")) { return ResponseModifier.ROOT; }
		else { return ResponseModifier.LINEAR; }
	}
	
	public abstract float getTransformedValue(float data);
}
