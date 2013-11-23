package com.mobilsignandroid;

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

import com.mobilsignandroid.communicator.Communicator;
import com.mobilsignandroid.zxing.IntentIntegrator;
import com.mobilsignandroid.zxing.IntentResult;

import java.security.MessageDigest;

public class MobilSign extends Activity {

	private Handler handler = new Handler();
	private ListView msgView;
	private ArrayAdapter<String> msgList; // list vsetkych sprav
	private Communicator communicator = new Communicator("192.168.1.2", 2002, this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		msgList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		msgView = (ListView) findViewById(R.id.listView);
		msgView.setAdapter(msgList);

        communicator.receiveMsg(); // spusti vlakno prijimajuce spravy

		Button btnSend = (Button) findViewById(R.id.btn_Send);
		btnSend.setOnClickListener(new View.OnClickListener() { // odosle spravu
			public void onClick(View v) {
				final EditText txtEdit = (EditText) findViewById(R.id.txt_inputText);
                String msgToSend = txtEdit.getText().toString();
                if(msgToSend != null){
                    communicator.sendMessageToServer(msgToSend);
                    msgView.smoothScrollToPosition(msgList.getCount() - 1);
                }
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
     * Zobrazi spravu upravenu na spravny format
     * @param msg - neupravena sprava
     */
    public void displayMsg(String msg) {
        if (msg.length() > 5 && msg.substring(0, 5).equals("SEND:")) { //niekto nieco posiela
            msg = "Message recieved: [" + msg.substring(5) + "]";
        } else if (msg.length() > 5 && msg.substring(0, 5).equals("RESP:")) { //niekto odpoveda na nasu spravu
            if (msg.substring(5).equals("paired")) {
                msg = "Response: [" + msg.substring(5) + "]";
            }
        } else {
            msg = "Unknown message: [" + msg + "]";
        }
        final String finalMsg = msg;
        handler.post(new Runnable() {
            public void run() {
                msgList.add(finalMsg);
                msgView.setAdapter(msgList);
                msgView.smoothScrollToPosition(msgList.getCount() - 1);
            }
        });
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

		if (scanResult != null) { // pokial sa nieco naskenovalo tak sa to spracuje
            String QRcodeString;
            try{
                QRcodeString = scanResult.getContents() == null ? "0" : scanResult.getContents(); // prevediet scan do stringu, pokial scan vrati null string bude 0
                String modulus = QRcodeString;
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(modulus.getBytes("UTF-8"));
                QRcodeString = byteArrayToHexString(digest);
            }catch(Exception e){
                e.printStackTrace();
                return;
            }

			if (QRcodeString.equalsIgnoreCase("0")) { // pokial je string 0, vypise sa chyba
				Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();
			} else { // pokial je string spravny vypise sa na obrazovku
				String QRcodeStringEdit = "PAIR:"+QRcodeString; // prida pred string z QR kodu retazec PAIR aby server vedel, ze sa idem parovat
                displayMsg(QRcodeStringEdit);
                communicator.sendMessageToServer(QRcodeStringEdit);
			}
		} else { // pokial QR code je null, vypise sa chyba
			Toast.makeText(this, "Problem to secan the barcode.",
					Toast.LENGTH_LONG).show();
		}
	}

    private static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

}