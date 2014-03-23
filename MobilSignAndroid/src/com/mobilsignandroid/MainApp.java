package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.mobilsignandroid.communicator.Communicator;
import com.mobilsignandroid.communicator.Crypto;
import com.mobilsignandroid.keystore.KeyStore;
import com.mobilsignandroid.zxing.IntentIntegrator;
import com.mobilsignandroid.zxing.IntentResult;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * Created by Michal on 23.3.2014.
 */
public class MainApp extends Activity {
	// trieda vykonavajuca kryptograficke operacie
	private Crypto crypto;

	// prvky potrebne pre list sprav
	private Handler handler;
	private ListView messageView;
	private ArrayAdapter<String> messageList;

	// trieda vykonavajuca komunikaciu so serverom
	private Communicator communicator;

	// kluce
	private PrivateKey privateKey;
	private RSAPublicKey publicKey;
	private RSAPublicKey QRPublicKey;

	private AlertDialog.Builder messageBox;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		handler = new Handler();
		messageBox = new AlertDialog.Builder(this);
		messageList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		messageView = (ListView) findViewById(R.id.msgQueue);
		messageView.setAdapter(messageList);

		String ipAddress = getIntent().getStringExtra("ipAddress");
		int port = Integer.parseInt(getIntent().getStringExtra("port"));
		communicator = new Communicator(ipAddress, port, this);

		try{
			communicator.connectToServer();
		} catch (IOException e){
			messageBox("Nepodarilo sa pripojit na server.", "Error", "OK");
			startActivity(new Intent(this, Connect.class));
		}
		communicator.receiveMsg(); // spusti vlakno prijimajuce spravy

		try{ // posklada privatny kluc, ak ho najde ulozeny
			privateKey = getPrivateKey(getString(R.string.key_file));
		} catch(FileNotFoundException e){
			messageBox("Chyba pri získavan kľúča z úložiska: " + e.getMessage(), "Error", "OK");
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e){
			messageBox("Keyfactory nedokázal nájsť daný algoritmus: " + e.getMessage(), "Error", "OK");
			e.printStackTrace();
		} catch (InvalidKeySpecException e){
			messageBox("Keyfactory nedokázal poskladať kľúč: " + e.getMessage(), "Error", "OK");
			e.printStackTrace();
		}

        /* udalost pre button poslanie spravy */
		Button btnSend = (Button) findViewById(R.id.btn_Send);
		btnSend.setOnClickListener(new View.OnClickListener() { // odosle spravu
			public void onClick(View v) {
				final EditText txtEdit = (EditText) findViewById(R.id.txt_inputText);
				String msgToSend = txtEdit.getText().toString();
				msgToSend = "SEND:" + crypto.encrypt(msgToSend);
				if(msgToSend != null){
					communicator.sendMessageToServer(msgToSend);
					messageView.smoothScrollToPosition(messageList.getCount() - 1);
				}
			}
		});

		/* udalost pre button na odfotenie QR kodu */
		Button btnQR = (Button) findViewById(R.id.btnQR);
		btnQR.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				IntentIntegrator integrator = new IntentIntegrator(MainApp.this);
				integrator.initiateScan();
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (scanResult != null) { // pokial sa nieco naskenovalo tak sa to spracuje
			BigInteger modulus;
			try{
				if(scanResult.getContents() == null){
					messageBox("Chyba", "Nepodarilo sa zosnímať QR kód. Skúste to prosím znovu.", "OK");
					return;
				}
				String QrCode = scanResult.getContents(); // prevediet scan do stringu, pokial scan vrati null string bude 0
				byte[] modulusByteArray = Base64.decode(QrCode, Base64.DEFAULT);
				modulus = new BigInteger(modulusByteArray);
				QRPublicKey = getKeyFromModulus(modulus);
				communicator.setKey(QRPublicKey); // z modulusu z QR kodu zisti kluc, ktory bude sifrovat komunikaciu a ten si nastavi do atributu
				communicator.pairRequest();
				crypto = new Crypto(QRPublicKey);

				KeyPairGenerator generatorRSA = KeyPairGenerator.getInstance("RSA"); // vytvori instanciu generatora RSA klucov
				generatorRSA.initialize(2048, new SecureRandom()); // inicializuje generator 2048 bitovych RSA klucov
				KeyPair keyRSA = generatorRSA.generateKeyPair(); // vygeneruje klucovi par
				privateKey = keyRSA.getPrivate(); // kluc desktopovej aplikacie je sukromny kluc z klucoveho paru
				publicKey = (RSAPublicKey) keyRSA.getPublic(); // vrati verejny kluc type RSAPublicKey, lebo z neho mozem dostat modulus
				communicator.sendMessageToServer("MPUB:" +  crypto.encrypt(publicKey.getModulus()+""));
			}catch(Exception e){
				e.printStackTrace();
				return;
			}
		} else { // pokial QR code je null, vypise sa chyba
			messageBox("Nastala chyba, prosím zoskenujte QR kód znova.", "Error", "OK");
		}
	}

	/**
	 * Zobrazi spravu upravenu na spravny format
	 * @param msg - neupravena sprava
	 */
	public void displayMsg(String msg) {
		if (msg.length() > 5 && msg.substring(0, 5).equals("SEND:")) { //niekto nieco posiela
			String decrypted = crypto.decrypt(msg.substring(5));
			msg = "Message recieved: [" + decrypted + "]";
		} else if (msg.length() > 5 && msg.substring(0, 5).equals("RESP:")) { //niekto odpoveda na nasu spravu
			if (msg.substring(5).equals("paired")) {
				msg = "Response: [" + msg.substring(5) + "]";
			}
		} else if (msg.length() > 5 && msg.substring(0, 5).equals("MPUB:")) { //niekto odpoveda na nasu spravu
			crypto.setKey(privateKey);
			String decrypt1 = crypto.decrypt(msg.substring(5));
			crypto.setKey(QRPublicKey);
			String decrypt2 = crypto.decrypt(decrypt1);
			if(decrypt2.equals(publicKey.getModulus()+"")){
				crypto.setKey(privateKey);
				if(!savePrivateKey(privateKey, getString(R.string.key_file))){
					messageBox("Neporadilo sa uložiť kľúč", "Chyba", "OK");
				}
			} else{
				Log.e("CHYBA", "CHYBA displayMsg"); // TODO ukoncit spojenie a poslat spravu, ze sa detekoval utok
			}
			return;
		} else {
			msg = "Unknown message: [" + msg + "]";
		}
		final String finalMsg = msg;
		handler.post(new Runnable() {
			public void run() {
				messageList.add(finalMsg);
				messageView.setAdapter(messageList);
				messageView.smoothScrollToPosition(messageList.getCount() - 1);
			}
		});
	}

	public PrivateKey getPrivateKey(String fileName) throws FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException{
		startActivity(new Intent("android.credentials.UNLOCK"));
		KeyStore ks = KeyStore.getInstance();
		if(ks.contains(fileName)){
			byte[] keyBytes = ks.get(fileName);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory rsaFact = KeyFactory.getInstance("RSA");
			return rsaFact.generatePrivate(spec);
		} else{
			throw new FileNotFoundException();
		}
	}

	public boolean savePrivateKey(PrivateKey key, String fileName){
		startActivity(new Intent("android.credentials.UNLOCK"));
		KeyStore ks = KeyStore.getInstance();
		return ks.put(fileName, key.getEncoded());
	}

	/**
	 * Vyrobi z modulusu kluc a vrati ho
	 * @param pModulus - modulus, z ktoreho sa vygeneruje kluc
	 * @return RSAPublicKey
	 */
	public RSAPublicKey getKeyFromModulus(BigInteger pModulus){
		RSAPublicKey key = null;
		try{
			//BigInteger modulus = new BigInteger(pModulus + "", 16); // vyrobi BigInteger z exponenta 65537
			RSAPublicKeySpec spec = new RSAPublicKeySpec(pModulus, new BigInteger("65537"));
			KeyFactory factory = KeyFactory.getInstance("RSA");
			key = (RSAPublicKey)factory.generatePublic(spec);
		} catch(Exception e){
			//messageBox("Error code: 3 " + e.getMessage(), "Chyba", "OK");
			e.printStackTrace();
		}
		return key;
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
}
