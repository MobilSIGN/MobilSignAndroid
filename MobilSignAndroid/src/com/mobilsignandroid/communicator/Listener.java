package com.mobilsignandroid.communicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Listener extends Thread {

    private BufferedReader mIn; //citac
    private Queue<String> mReceivedMessages;

    public Listener(Socket socket) {
        try {
            mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mReceivedMessages = new LinkedBlockingQueue<>();
        } catch (IOException ex) {
            System.err.println("Vyskytla sa chyba pri vytvarani clientListenera");
        }
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                String message = mIn.readLine();
                if (message == null || message.equals("")) {
                    // todo odstranit zo zoznamu klientov
                    System.out.println("Breakujem");
                    break;
                }
                if (message != null) {
                    mReceivedMessages.add(message);
                }
            }
        } catch (IOException ioex) {
            // Problem reading from socket (communication is broken)
            System.err.println("Chyba pri komunikacii s klientom");
            ioex.printStackTrace();
        }
    }

    public String getMessage() {
        return mReceivedMessages.poll();
    }

    public boolean hasMessage() {
        return (mReceivedMessages.peek() == null) ? false : true;
    }

}
