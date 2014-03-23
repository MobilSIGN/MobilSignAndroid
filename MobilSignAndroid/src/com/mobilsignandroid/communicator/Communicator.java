package com.mobilsignandroid.communicator;

import com.mobilsignandroid.MainApp;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;


public class Communicator implements Serializable {

    private String serverAddress; // ip adresa servera
    private int serverPort; // port na ktorom server pocuva
    private Socket socket;
    private Sender clientSender;
    private Listener clientListener;
    private MainApp activity;
	private RSAPublicKey communicationKey; // kluc aplikacie, ktorym sa bude sifrovat komunikacia

    /********************** Konstruktory **********************/
    public Communicator(String serverAddress, int serverPort, MainApp activity) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.activity = activity;
    }

    public Communicator(MainApp activity){
        this.activity = activity;
    }


    /********************** Gettery, settery **********************/
    public void setServerAddress(String serverAddress){
        this.serverAddress = serverAddress;
    }

    public String getServerAddress(){
        return this.serverAddress;
    }

    public void setServerPort(int serverPort){
        this.serverPort = serverPort;
    }

    public int getServerPort(){
        return this.serverPort;
    }

	public RSAPublicKey getKey(){
		return this.communicationKey;
	}

	public void setKey(RSAPublicKey key){
		this.communicationKey = key;
	}


    /************************ Metody **********************/

    /**
     * Pripoji sa na server
     */
    public void connectToServer() throws IOException {
        this.socket = new Socket(this.serverAddress,this.serverPort);
        this.clientSender = new Sender(socket);
        this.clientListener = new Listener(socket);
        this.clientListener.start();
        this.clientSender.start();
        receiveMsg(); // spusti sa prijimanie sprav, pokial boli vyslane
    }


    /**
     * Spusti vlakno odosielajuce spravy na server
     * @param str - odosielana sprava
     */
    public void sendMessageToServer(String str) {
		final String str1 = str;
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (clientSender == null) {
					System.out.println("Sprava je null");
				}
				clientSender.putMesssageToQueue(str1);
			}
		}).start();
    }

	/**
	 * Odosle parovaci request na server.
	 */
	public void pairRequest() {
		try {
			BigInteger modulus = this.getKey().getModulus();
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(modulus.toByteArray());
			String sha1 = new BigInteger(1, md.digest()).toString(16);
			sendMessageToServer("PAIR:" + sha1);
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
	}

    /**
     * Spusti vlakno prijimajuce spravy zo servera
     */
    public synchronized void receiveMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(clientListener.hasMessage()){
                        activity.displayMsg(clientListener.getMessage());
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();

    }

}
