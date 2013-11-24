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
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.util.StringTokenizer;

public class MobilSign extends Activity {
    static final String ACCOUNT_FILE = "acount_info"; // nazov suboru s pouzivatelskymi informaciami

    // prvky potrebne pre list sprav
	private Handler handler;
	private ListView messageView;
	private ArrayAdapter<String> messageList;

    // vykonava celu komunikaciu so severom (klientom)
    private Communicator communicator;
	// kluc aplikacie, ktorym sa bude sifrovat komunikacia
	private PublicKey communicationKey;
    // vypisuje hlasky
    private AlertDialog.Builder messageBox;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // inicializacia atributov potrebnych v celej triede
        this.handler = new Handler();
        try{
            // overi ci je aplikacia spustena prvy krat, pokial ano tak neexistuje subor s uzivatelskymi nastaveniami
            if(fileExistance(ACCOUNT_FILE)) {
                login(); // aplikacia nie je spustena prvy krat, prejde sa na prihlasovaci formular
            } else{
                register(); // aplikacia je spustena prvy krat, prejde sa na registracny formular
            }
        } catch (Exception e){
            messageBox("Error code: 0", "Chyba", "OK");
            e.printStackTrace();
        }
    }

    /**
     * Aplikacia si vypita od pouzivatela PIN, ktorym sa bude do aplikacie prihlasovat.
     * Ten sa ulozi do interneho uloziska telefonu
     */
    private void register(){
        setContentView(R.layout.register);

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
                        FileOutputStream fos = openFileOutput(ACCOUNT_FILE, Context.MODE_PRIVATE);
                        fos.write(toFile.getBytes());
                        fos.close();
                        messageBox("Váš PIN kód bol uložený. Môžete sa prihlásiť.", "Správa", "OK");
                        login();
                    } catch (Exception e){
                        messageBox("Error code: 1 " + e.getMessage(), "Chyba", "OK");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Zobrazi formular na prihlasenia a po zadani spravneho PINu prihlasi pouzivatela do aplikacie.
     * V pripade nespravneho PINu sa vypise hlaska a uzivatel sa neprihlasi
     */
    private void login(){
        setContentView(R.layout.login);
        messageList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        // po kliku na tlacidlo prihlasit v prihlasovacom layoute sa pouzivatel pokusi prihlasit
        Button btnLogin = (Button) findViewById(R.id.login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // PIN zo suboru
                String pinSalt = getPin(ACCOUNT_FILE);
                StringTokenizer tokensOrigin = new StringTokenizer(pinSalt, "|"); // rozbije string na pin a salt
                String pinOrigin = tokensOrigin.nextToken();// ulozi aktualny pin
                String saltOrigin = tokensOrigin.nextToken();// ulozi aktualny salt

                // ziska sa zadany PIN
                final EditText etPin = (EditText) findViewById(R.id.pin);
                String pinGiven = etPin.getText().toString();
                String pinSaltGiven = hashPassword(pinGiven, saltOrigin); // zahesuje zadany pin s danym saltom
                StringTokenizer tokensGiven = new StringTokenizer(pinSaltGiven, "|"); // rozbije string na pin a salt
                pinGiven = tokensGiven.nextToken();// ulozi pin prijaty od uzivatela

                // PIN sa porovna z PINom ulozenym v subore s autentifikacnymi udajmi
                if(!pinGiven.equals(pinOrigin)){
                    messageBox("PIN kód nie je správny. Zadajte prosím správny PIN kód.", "Chyba", "OK"); // pokial je PIN nespravny vypise sa chyba
                } else{
                    connect(); // pokial je PIN spravny uzivatel je prihlaseny do aplikacie
                }
            }
        });
    }

    /**
     * Zobrazi formular na zadanie IP adresy a portu a pokusi sa pripojit na server.
     * V pripade neuspesneho pripojenia sa vypise hlaska
     */
    private void connect(){
        setContentView(R.layout.connect);
        communicator = new Communicator(this);

        // pokusi sa pripojit na server
        Button btnConnect = (Button) findViewById(R.id.connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText etIpAddress = (EditText) findViewById(R.id.ip_address);
                final EditText etPort = (EditText) findViewById(R.id.port);
                String ipAddress = etIpAddress.getText().toString();
                int port = Integer.parseInt(etPort.getText().toString());
                if(ipAddress != null){
                    communicator.setServerAddress(ipAddress);
                    communicator.setServerPort(port);
                    try{
                        communicator.connectToServer();
                        mainApp();
                    } catch(Exception e){
                        messageBox("Nepodarilo sa pripojit na server.", "Chyba", "OK");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Zobrazi layout s hlavnym oknom aplikacie
     */
    private void mainApp(){
        setContentView(R.layout.main);

        /**** nastavenie parametrov ****/
        messageView = (ListView) findViewById(R.id.msgQueue);
        messageView.setAdapter(messageList);
        communicator.receiveMsg(); // spusti vlakno prijimajuce spravy

        /**** udalosti pre buttny ****/
        Button btnSend = (Button) findViewById(R.id.btn_Send);
        btnSend.setOnClickListener(new View.OnClickListener() { // odosle spravu
            public void onClick(View v) {
                final EditText txtEdit = (EditText) findViewById(R.id.txt_inputText);
                String msgToSend = txtEdit.getText().toString();
                if(msgToSend != null){
                    communicator.sendMessageToServer(msgToSend);
                    messageView.smoothScrollToPosition(messageList.getCount() - 1);
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
                messageList.add(finalMsg);
                messageView.setAdapter(messageList);
                messageView.smoothScrollToPosition(messageList.getCount() - 1);
            }
        });
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		String modulus = null;
		if (scanResult != null) { // pokial sa nieco naskenovalo tak sa to spracuje
            String QRcodeString;
            try{
                QRcodeString = scanResult.getContents() == null ? "0" : scanResult.getContents(); // prevediet scan do stringu, pokial scan vrati null string bude 0
				modulus = QRcodeString;
				// QR kode sa zahesuje a prevedie na HEX aby sa mohol odoslat na server na sparovanie
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(QRcodeString.getBytes("UTF-8"));
                QRcodeString = byteArrayToHexString(digest);
            }catch(Exception e){
                e.printStackTrace();
                return;
            }

			if (QRcodeString.equalsIgnoreCase("0") || modulus == null) { // pokial je string 0, vypise sa chyba
				messageBox("Nepodarilo sa zosnimat QR kod.", "Chyba", "OK");
				Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();
			} else { // pokial je string spravny vypise sa na obrazovku
				String QRcodeStringEdit = "PAIR:"+QRcodeString; // prida pred string z QR kodu retazec PAIR aby server vedel, ze sa idem parovat
                communicator.sendMessageToServer(QRcodeStringEdit); // posle ziadost o sparovanie na server
				communicationKey = getKeyFromModulus(modulus); // z modulusu z QR kodu zisti kluc, ktory bude sifrovat komunikaciu a ten si nastavi do atributu
			}
		} else { // pokial QR code je null, vypise sa chyba
			Toast.makeText(this, "Problem to secan the barcode.",
					Toast.LENGTH_LONG).show();
		}
	}

    /**
     * Prevedie pole bytov do HEX stringu
     * @param b - pole bytov
     * @return HEX string
     */
    private static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    /**
     * Pokusi sa ziskat PIN kod zo suboru a ten vrati
     * @param fileName - nazov suboru s PIN kodom
     * @return PIN kod
     */
    private String getPin(String fileName) {
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
     * Overi ci existuje subor v internom ulozisku
     * @param fname - nazov suboru
     * @return true/false
     */
    private boolean fileExistance(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    /**
     * Vypise messagebox so spravou
     * @param content - obsah spravy
     * @param title - nadpis spravy
     * @param positiveButton - napis na tlacidle
     */
    private void messageBox(String content, String title, String positiveButton){
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
    private String hashPassword(String password, String salt){
        String encryptedString = "";
        try{
            // pokial je sol prazdna vygeneruje ju
            if(salt == ""){
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

	/**
	 * Vyrobi z modulusu kluc a vrati ho
	 * @param pModulus
	 * @return
	 */
    private PublicKey getKeyFromModulus(String pModulus){
		PublicKey key = null;
		try{
			BigInteger modulus = new BigInteger(pModulus, 16);
			BigInteger exponent = new BigInteger("65537", 16); // exponent je vzdy 65537
			RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, exponent);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			key = factory.generatePublic(publicSpec);
		} catch(Exception e){
			messageBox("Error code: 3 " + e.getMessage(), "Chyba", "OK");
			e.printStackTrace();
		}
		return key;
    }

}