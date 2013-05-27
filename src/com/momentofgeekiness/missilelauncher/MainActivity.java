package com.momentofgeekiness.missilelauncher;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.momentofgeekiness.missilelauncher.io.MLCommand;

public class MainActivity extends Activity {

	// Hardcoded in pymissile
	private int port;	
	private InetAddress addr;

	private ImageButton btnUp;
	private ImageButton btnDown;
	private ImageButton btnLeft;
	private ImageButton btnRight;
	private ImageButton btnStop;
	private Button btnLaunch;
	private long lastLaunch;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Load preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// Set anti-flood variable
		lastLaunch = 0;

		// Init buttons
		btnUp = (ImageButton) findViewById(R.id.buttonUp);
		btnDown = (ImageButton) findViewById(R.id.buttonDown);
		btnLeft = (ImageButton) findViewById(R.id.buttonLeft);
		btnRight = (ImageButton) findViewById(R.id.buttonRight);
		btnStop = (ImageButton) findViewById(R.id.buttomStop);
		btnLaunch = (Button) findViewById(R.id.buttonLaunch);

		// Set listeners
		btnUp.setOnTouchListener(arrowButtonListener);
		btnDown.setOnTouchListener(arrowButtonListener);
		btnLeft.setOnTouchListener(arrowButtonListener);
		btnRight.setOnTouchListener(arrowButtonListener);

		btnStop.setOnClickListener(buttonListener);
		btnLaunch.setOnClickListener(buttonListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		Intent prefsIntent = new Intent(this.getApplicationContext(),SettingsActivity.class);
		MenuItem preferences = menu.findItem(R.id.action_settings);
		preferences.setIntent(prefsIntent);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			this.startActivity(item.getIntent());
			break;
		}
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		setIP(sharedPrefs.getString("ip_address", "127.0.0.1"));
		port = Integer.parseInt(sharedPrefs.getString("port", "20000"));
	}
	
	OnTouchListener arrowButtonListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			// On button release
			if (event.getAction() == MotionEvent.ACTION_UP) {
				handleCommand(MLCommand.STOP);

				// On button press
			} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (v == btnDown) {
					handleCommand(MLCommand.DOWN);
				} else if (v == btnUp) {
					handleCommand(MLCommand.UP);
				} else if (v == btnLeft) {
					handleCommand(MLCommand.LEFT);
				} else if (v == btnRight) {
					handleCommand(MLCommand.RIGHT);
				}
			}
			return true;
		}

		
	};

	OnClickListener buttonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(v == btnStop) {
				handleCommand(MLCommand.STOP);
			} else if (v == btnLaunch) {
				long currentTime = System.currentTimeMillis();
				if((lastLaunch + 4000L) <= currentTime) {
					handleCommand(MLCommand.FIRE);
					lastLaunch = currentTime;
				} else {
					Toast.makeText(getApplicationContext(),"Stop flooding!", Toast.LENGTH_SHORT).show();
				}				
			}	
		}		
	};
	
	private void handleCommand(MLCommand command) {
		try {
			SocketManager sm = new SocketManager(addr, port);
			sm.execute(command.getCommand());
		} catch (SocketException e) {
			Toast.makeText(getApplicationContext(),"Port "+port+" in use on the Android device!", Toast.LENGTH_SHORT).show();
			return;
		}
	}
	
	public void setIP(String ip) {
		try {
			addr = InetAddress.getByName(ip);			
		} catch (UnknownHostException e) {
			Toast.makeText(getApplicationContext(),"Unknown host: "+ip, Toast.LENGTH_SHORT).show();
		} 
	}
	
	private class SocketManager extends AsyncTask<String, Boolean, Boolean> {

		private InetAddress addr;
		private DatagramSocket s;
		private int port;

		public SocketManager(InetAddress addr, int port) throws SocketException {
			this.s = new DatagramSocket();
			this.port = port;
			this.addr = addr;
		}

		@Override
		protected Boolean doInBackground(String... params) {
			byte[] commands = params[0].getBytes();			
			try {
				for(byte command : commands) {
					DatagramPacket packet= new DatagramPacket(new byte[]{command}, 1,addr,port);					
					s.send(packet);
				}
			} catch (Exception e) {
				return Boolean.valueOf(false);
			} finally {
				close();
			}
			return Boolean.valueOf(true);
		}
		
		@Override
		protected void onPostExecute(Boolean bool) {
			if(!bool.booleanValue())
				Toast.makeText(getApplicationContext(),"Error occured when sending package!", Toast.LENGTH_SHORT).show();
		}
		
		public void close() {
			s.close();
		}
	}

}
