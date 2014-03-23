package com.mobilsignandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;

public class MobilSign extends Activity {
	private AlertDialog.Builder messageBox;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		this.messageBox = new AlertDialog.Builder(this);

		try{
			// overi ci je aplikacia spustena prvy krat, pokial ano tak neexistuje subor s uzivatelskymi nastaveniami
			if(fileExistance(getString(R.string.account_file))) {
				startActivity(new Intent(this,Login.class));
			} else { // pokial je aplikacia spustena prvy krat tak sa presmeruje na registracny formular
				startActivity(new Intent(this,Register.class));
			}
		} catch (Exception e){
			messageBox("Error code: 0 " + e.getMessage(), "Chyba", "OK");
			e.printStackTrace();
		}
	}

	/**
	 * Overi ci existuje subor v internom ulozisku
	 * @param fname - nazov suboru
	 * @return true/false
	 */
	public boolean fileExistance(String fname){
		File file = getBaseContext().getFileStreamPath(fname);
		return file.exists();
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