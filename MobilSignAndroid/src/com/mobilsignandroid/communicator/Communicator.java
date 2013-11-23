package com.mobilsignandroid.communicator;

import com.mobilsignandroid.MobilSign;

import java.io.IOException;
import java.net.Socket;


public class Communicator {

    private String serverAddress; // ip adresa servera
    private int serverPort; // port na ktorom server pocuva
    private Socket socket;
    private Sender clientSender;
    private Listener clientListener;
    private MobilSign activity;

    public Communicator(String serverAddress, int serverPort, MobilSign activity) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.activity = activity;
        this.connectToServer();
    }

    /**
     * Pripoji sa na server
     */
    public void connectToServer() {
        try {
            this.socket = new Socket(this.serverAddress,this.serverPort);
            this.clientSender = new Sender(socket);
            this.clientListener = new Listener(socket);
            this.clientListener.start();
            this.clientSender.start();
        } catch (IOException ioe) {
            System.err.println("Can not establish connection to " + serverAddress + ":" + serverPort + "\n" + ioe.getMessage());
            ioe.printStackTrace(System.out);
            System.exit(-1);
        }
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
                clientSender.sendMessage(str1);
            }
        }).start();
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
