package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michal on 23.3.2014.
 */
public class Connect extends Activity{
	private AlertDialog.Builder messageBox;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);

		messageBox = new AlertDialog.Builder(this);

		// pokusi sa pripojit na server
		Button btnConnect = (Button) findViewById(R.id.connect);
		btnConnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final EditText etIpAddress = (EditText) findViewById(R.id.ip_address);
				final EditText etPort = (EditText) findViewById(R.id.port);
				String ipAddress = etIpAddress.getText().toString();
				int port = Integer.parseInt(etPort.getText().toString());
				if(ipAddress != null){
					if(ip(ipAddress)){
						Intent intent = new Intent(Connect.this, MainApp.class);
						intent.putExtra("port", port+"");
						intent.putExtra("ipAddress", ipAddress);
						startActivity(intent);
					} else {
						messageBox("Zadaná IP adresa má zlý formát.", "Error", "OK");
					}
				}
			}
		});
	}

	/**
	 * Vypise messagebox so spravou
	 * @param content - obsah spravy
	 * @param title - nadpis spravy
	 * @param positiveButton - napis na tlacidle
	 */
	public void messageBox(String content, String title, String positiveButton){
		messageBox.setMessage(content);
		messageBox.setTitle(title);
		messageBox.setPositiveButton(positiveButton, null);
		messageBox.create().show();
	}

	/**
	 * Overi zadana IP adresa ma spravny format
	 * @param text
	 * @return
	 */
	public boolean ip(String text) {
		Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
		Matcher m = p.matcher(text);
		return m.find();
	}


}
