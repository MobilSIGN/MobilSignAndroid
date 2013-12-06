package com.mobilsignandroid.communicator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender extends Thread {

	private LinkedBlockingQueue mMessageQueue;
	private PrintWriter mOut;

	public Sender(Socket socket){
		try{
			mOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			mMessageQueue = new LinkedBlockingQueue<>();
		}
		catch(IOException ex){
			System.err.println("Vyskytla sa chyba pri vytvarani clientSendera");
		}
	}

	public synchronized void sendMessage(String aMessage)
	{
		try{
			mMessageQueue.put(aMessage);
		}
		catch(InterruptedException ex){
			System.err.println("Vyskytla sa chyba pri vkladani spravy do frontu sprav na odoslanie");
		}
		notify();
	}

	private synchronized String getNextMessageFromQueue() throws InterruptedException
	{
		while (mMessageQueue.isEmpty()){
			wait();
		}
		String message = (String) mMessageQueue.poll();
		return message;
	}

	private void sendMessageToClient(String aMessage)
	{
		mOut.println(aMessage);
		mOut.flush();
	}

	@Override
	public void run()
	{
		try {
			while (!isInterrupted()) {
				String message = getNextMessageFromQueue();
				if (message != null) {
					sendMessageToClient(message);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
