package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public class MobilSign extends Activity {
    static final String ACCOUNT_FILE = "acount_info";
    // TODO upravit atributy a vytvarat ich az tam kde treba
	private Handler handler = new Handler();
	private ListView msgView;
	private ArrayAdapter<String> msgList; // list vsetkych sprav
    private Communicator communicator;
	//private Communicator communicator = new Communicator("192.168.1.2", 2002, this);

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        final AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        try{
            if(fileExistance(ACCOUNT_FILE)) {
                login();
            } else{
                register();
            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * Pokial je aplikacia spustena na zariadeni prvy krat, vypita si od pouzivatela PIN, ktorym sa bude do aplikacie prihlasovat.
     * Ten sa ulozi do interneho uloziska telefonu
     */
    private void register(){
        setContentView(R.layout.register);
        final AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        // po kliku na tlacidlo prihlasit v registracnom layoute sa zaregistruje novy pin
        Button btnRegister = (Button) findViewById(R.id.register);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // ziska zadane piny z oboch poli
                final EditText etPin1 = (EditText) findViewById(R.id.pin1);
                String pin1 = etPin1.getText().toString();
                final EditText etPin2 = (EditText) findViewById(R.id.pin2);
                String pin2 = etPin2.getText().toString();

                if(!pin1.equals(pin2)){
                    dlgAlert.setMessage("Zadané PIN kódy nie sú rovnaké.Zadajte prosím rovnaké PIN kódy.");
                    dlgAlert.setTitle("Chyba");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.create().show();
                } else{
                    try{
                        // TODO heslo treba hashovat a kontrolovat ci ma aspon 4 znaky
                        FileOutputStream fos = openFileOutput(ACCOUNT_FILE, Context.MODE_PRIVATE);
                        fos.write(pin1.getBytes());
                        fos.close();
                        dlgAlert.setMessage("Váš PIN kód bol uložený. Môžete sa prihlásiť.");
                        dlgAlert.setTitle("Správa");
                        dlgAlert.setPositiveButton("OK", null);
                        dlgAlert.create().show();
                        login();
                    } catch (Exception e){
                        dlgAlert.setMessage("code: 1");
                        dlgAlert.setTitle("Chyba");
                        dlgAlert.setPositiveButton("OK", null);
                        dlgAlert.create().show();
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private void login(){
        // TODO comunicatior vytvara az ked je overene heslo
        setContentView(R.layout.login);
        final AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        msgList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        communicator = new Communicator("192.168.1.2", 2002, this);

        // po kliku na tlacidlo prihlasit v registracnom layoute sa zaregistruje novy pin
        Button btnLogin = (Button) findViewById(R.id.login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText etPin = (EditText) findViewById(R.id.pin);
                String pinGiven = etPin.getText().toString();
                if(!pinGiven.equals(getPin(ACCOUNT_FILE))){
                    dlgAlert.setMessage("PIN kód nie je správny. Zadajte prosím správny PIN kód.");
                    dlgAlert.setTitle("Chyba");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.create().show();
                } else{
                    setContentView(R.layout.main);

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

            }
        });
    }

    private String getPin(String fileName) {
        String pin = "";
        try{
            FileInputStream fis = openFileInput(fileName);
            byte[] pinByte = new byte[fis.available()];
            fis.read(pinByte);
            fis.close();
            for(int i=0;i<pinByte.length;i++)
            {
                pin += (char)pinByte[i];
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return pin;
    }

    private boolean fileExistance(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    /*
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
*/
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