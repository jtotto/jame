package com.github.jame;

import com.github.jame.Fragments.PrefsFragment;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

public class JAMEMainActivity extends Activity 
{

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jamemain);
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
    			getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    			
    	}
    }

}
