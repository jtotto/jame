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
