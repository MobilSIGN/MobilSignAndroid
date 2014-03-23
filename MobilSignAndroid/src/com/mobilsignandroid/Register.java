package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.StringTokenizer;

/**
 * Created by Michal on 23.3.2014.
 */
public class Register extends Activity{
	private AlertDialog.Builder messageBox;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);

		messageBox = new AlertDialog.Builder(this);

		// po kliku na tlacidlo prihlasit v registracnom layoute sa zaregistruje novy pin
		Button btnRegister = (Button) findViewById(R.id.register);
		btnRegister.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// ziska zadane piny z oboch poli
				final EditText etPin1 = (EditText) findViewById(R.id.pin1);
				String pin1 = etPin1.getText().toString();
				final EditText etPin2 = (EditText) findViewById(R.id.pin2);
				String pin2 = etPin2.getText().toString();

				// porovna PIN kody
				if(!pin1.equals(pin2)){
					// pokial sa nerovnaju vypise sa chyba
					messageBox("Zadané PIN kódy nie sú rovnaké. Zadajte prosím rovnaké PIN kódy.", "Chyba", "OK");
				} else if(pin1.length() < 4){
					// PIN musi mat aspon 4 znaky
					messageBox("PIN kód musí mať aspoň 4 znaky.", "Chyba", "OK");
				} else{
					// pokial sa rovnaju pokysi sa zapisat heslo do suboru
					try{
						String hashPass = hashPassword(pin1, ""); // heslo sa zahesuje a vrati sa spet hash a salt
						StringTokenizer tokens = new StringTokenizer(hashPass, "|"); // rozbije string na heslo a salt
						String pass = tokens.nextToken();// ulozi heslo
						String salt = tokens.nextToken();// ulozi salt
						String toFile = "pass:"+pass+"|salt:"+salt; // hash aj salt sa spoja a ulozia do premennej

						// hash aj salt sa ulozia do suboru
						FileOutputStream fos = openFileOutput(getString(R.string.account_file), Context.MODE_PRIVATE);
						fos.write(toFile.getBytes());
						fos.close();
						messageBox("Váš PIN kód bol uložený. Môžete sa prihlásiť.", "Správa", "OK");
						startActivity(new Intent("Login"));
					} catch (Exception e){
						messageBox("Error code: 1 " + e.getMessage(), "Chyba", "OK");
						e.printStackTrace();
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
	 * Zahesuje heslo z parametra a vrati jeho hash
	 * @param password - heslo
	 * @param salt - sol, pridavane h hashu (kvoli bezpecnosti)
	 * @return hesovane heslo
	 */
	public String hashPassword(String password, String salt){
		String encryptedString = "";
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
			encryptedString = hexString.toString();
			encryptedString += "|"+salt;
		}catch(Exception e){
			messageBox("Error code: 2 " + e.getMessage(), "Chyba", "OK");
		}
		return encryptedString;
	}
}
