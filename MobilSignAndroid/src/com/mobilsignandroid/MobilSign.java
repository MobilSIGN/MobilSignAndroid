package com.mobilsignandroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MobilSign extends Activity {

	private Handler handler = new Handler();
	private ListView msgView;
	private ArrayAdapter<String> msgList; // list vsetkych sprav
	private Socket socket;
	private String address = "192.168.1.6";
	private int port = 2002;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			socket = new Socket(address, port); // vytvori pripojenie na server
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		msgList = new ArrayAdapter<String>(this, 	android.R.layout.simple_list_item_1);
		msgView = (ListView) findViewById(R.id.listView);
		msgView.setAdapter(msgList);

		receiveMsg(); // spusti vlakno prijimajuce spravy

		Button btnSend = (Button) findViewById(R.id.btn_Send);
		btnSend.setOnClickListener(new View.OnClickListener() { // odosle spravu
			public void onClick(View v) {
				final EditText txtEdit = (EditText) findViewById(R.id.txt_inputText);
				// msgList.add(txtEdit.getText().toString());
				sendMessageToServer(txtEdit.getText().toString());
				msgView.smoothScrollToPosition(msgList.getCount() - 1);
			}
		});
		
		Button btnQR = (Button) findViewById(R.id.btnQR);
		btnQR.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	IntentIntegrator integrator = new IntentIntegrator(MobilSign.this);
                integrator.initiateScan();  
            }
        });
	}

	/**
	 * Spusti vlakno odosielajuce spravy na server
	 */
	public void sendMessageToServer(String str) {
		final String str1 = str;

		new Thread(new Runnable() {
			public void run() {
				PrintWriter out;
				try {
					out = new PrintWriter(socket.getOutputStream());
					out.println(str1);
					out.flush();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Spusti vlakno prijimajuce spravy zo servera
	 */
	public void receiveMsg() {

		new Thread(new Runnable() {
			public void run() {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
				} catch (IOException e) {
					e.printStackTrace();
				}

				while (true) {
					String msg = null;
					try {
						msg = in.readLine();
						// msgList.add(msg);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (msg == null) {
						break;
					} else {
						displayMsg(msg);
					}
				}
			}
		}).start();

	}

	/**
	 * Zobrazi spravu upravenu na spravny format
	 */
	public void displayMsg(String msg) {
		if (msg.length() > 5 && msg.substring(0, 5).equals("SEND:")) {
			msg = "Message recieved: [" + msg.substring(5) + "]";
		} else if (msg.length() > 5 && msg.substring(0, 5).equals("RESP:")) {
			if (msg.substring(5).equals("paired")) {
				msg = "Response: [" + msg.substring(5) + "]";
			}
		} else {
			msg = "Unknown message: [" + msg + "]";
		}
		final String mssg = msg;

		handler.post(new Runnable() {
			public void run() {
				msgList.add(mssg);
				msgView.setAdapter(msgList);
				msgView.smoothScrollToPosition(msgList.getCount() - 1);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
				
		if (scanResult != null) { // pokial sa nieco naskenovalo tak sa to spracuje
			String QRcodeString = scanResult.getContents() == null ? "0" : scanResult.getContents(); // prevediet scan do stringu, pokial scan vrati null string bude 0
						
			if (QRcodeString.equalsIgnoreCase("0")) { // pokial je string 0, vypise sa chyba
				Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();
			} else { // pokial je string spravny vypise sa na obrazovku
				String QRcodeStringEdit = "PAIR:"+QRcodeString; // prida pred string z QR kodu retazec PAIR aby server vedel, ze sa idem parovat
				sendMessageToServer(QRcodeStringEdit);
			}
		} else { // pokial QR code je null, vypise sa chyba
			Toast.makeText(this, "Problem to secan the barcode.",
					Toast.LENGTH_LONG).show();
		}
	}

}