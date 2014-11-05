package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.StringTokenizer;

/**
 * Created by Michal on 23.3.2014.
 */
public class Login extends Activity {
	private AlertDialog.Builder messageBox;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		messageBox = new AlertDialog.Builder(this);

		// po kliku na tlacidlo prihlasit v prihlasovacom layoute sa pouzivatel pokusi prihlasit
		Button btnLogin = (Button) findViewById(R.id.login);
		btnLogin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// PIN zo suboru
				String pinSalt = getPin(getString(R.string.account_file));
				StringTokenizer tokensOrigin = new StringTokenizer(pinSalt, "|"); // rozbije string na pin a salt
				String pinOrigin = tokensOrigin.nextToken();// ulozi aktualny pin
				String saltOrigin = tokensOrigin.nextToken();// ulozi aktualny salt

				// ziska sa zadany PIN
				final EditText etPin = (EditText) findViewById(R.id.pin);
				String pinGiven = etPin.getText().toString();
				String[] hashArray = hashPassword(pinGiven, saltOrigin); // zahesuje zadany pin s danym saltom
				pinGiven = hashArray[0];// ulozi pin prijaty od uzivatela

				// PIN sa porovna z PINom ulozenym v subore s autentifikacnymi udajmi
				if(!pinGiven.equals(pinOrigin)){
					messageBox("PIN kód nie je správny. Zadajte prosím správny PIN kód.", "Chyba", "OK"); // pokial je PIN nespravny vypise sa chyba
				} else{
					startActivity(new Intent(Login.this, Connect.class)); // pokial je PIN spravny uzivatel je prihlaseny do aplikacie
				}
			}
		});
	}

	/**
	 * Pokusi sa ziskat PIN kod zo suboru a ten vrati
	 * @param fileName - nazov suboru s PIN kodom
	 * @return PIN kod
	 */
	public String getPin(String fileName) {
		String pinSalt = "";
		try{
			String pin = "";
			FileInputStream fis = openFileInput(fileName);
			byte[] pinByte = new byte[fis.available()];
			fis.read(pinByte);
			fis.close();
			for(int i=0;i<pinByte.length;i++)
			{
				pin += (char)pinByte[i];
			}
			StringTokenizer tokens = new StringTokenizer(pin, "|"); // rozbije string na pin a salt
			pin = tokens.nextToken();// ulozi aktualny pin
			String salt = tokens.nextToken();// ulozi aktualny salt

			tokens = new StringTokenizer(pin, ":"); // rozbije string na pin a salt
			tokens.nextToken();
			pin = tokens.nextToken();// ulozi aktualny pin
			tokens = new StringTokenizer(salt, ":"); // rozbije string na pin a salt
			tokens.nextToken();
			salt = tokens.nextToken();// ulozi aktualny salt

			pinSalt = pin + "|" + salt; // hash aj salt sa spoja a ulozia do premennej
		}catch (Exception e){
			messageBox("Nepodarilo sa ziskat PIN kod zo suboru. " + e.getMessage(), "Chyba", "OK");
			e.printStackTrace();
		}
		return pinSalt;
	}

	/**
	 * Zahesuje heslo z parametra a vrati jeho hash
	 * @param password - heslo
	 * @param salt - sol, pridavane h hashu (kvoli bezpecnosti)
	 * @return hesovane heslo
	 */
	public String[] hashPassword(String password, String salt){
		String[] hashArray = new String[2];
		try{
			// pokial je sol prazdna vygeneruje ju
			if(salt.equals("")){
				SecureRandom random = new SecureRandom();
				BigInteger bigInteger = new BigInteger(100, random);
				salt = bigInteger.toString(32);
			}
			// pripoji salt k heslu a vysledok zahesuje
			String saltedPassword = password + salt;

			// zahesuje salt s heslom
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(saltedPassword.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			hashArray[0] = hexString.toString();
			hashArray[1] = salt;
		}catch(Exception e){
			messageBox("Error code: 2 " + e.getMessage(), "Chyba", "OK");
		}
		return hashArray;
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
