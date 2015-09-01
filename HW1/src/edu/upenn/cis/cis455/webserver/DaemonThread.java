package edu.upenn.cis.cis455.webserver;

import org.apache.log4j.Logger;

import edu.upenn.cis.cis455.webserver.ERISServletHarness.Handler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;

import javax.servlet.http.HttpServlet;

/**
 * Class for Daemon thread.
 * 
 * Daemon pushes Client Socket Object ( requests from the client ) onto the shared queue as long as it is not full.
 */
public class DaemonThread extends Thread {

	// Using log4j framework for logging
	static final Logger logger = Logger.getLogger(DaemonThread.class);		

	protected ArrayList<Socket> sharedQueue = new ArrayList<Socket> ();
	protected WorkerThread workerThread = null;
	protected static ArrayList<Thread> threadPool = new ArrayList<Thread> ();
	protected static int serverPort = 8080;
	protected static ServerSocket serverSocket = null;
	protected static String homeDirectory = "";
	protected static boolean alive = true;

	protected static HashMap<String, String> mapThreadRequestData = new HashMap<String,String> ();
	protected static HashMap<String,HttpServlet> servletPool = new HashMap<String,HttpServlet>();
	protected static ArrayList<ERISSession> sessionList;

	public DaemonThread (int serverPort, String homeDirectory ) {
		// System.out.println("[Output from log4j] After Entering Daemon Thread Constructor");
		DaemonThread.serverPort = serverPort;
		DaemonThread.homeDirectory = homeDirectory;
		// create thread pool with fixed size
		for ( int iThread = 0 ; iThread < 20 ; iThread++ ){
			workerThread = new WorkerThread(this.sharedQueue,DaemonThread.threadPool);
			Thread thread = new Thread (workerThread);
			thread.setName("Thread - " + iThread);
			threadPool.add(thread);
			thread.start();
		}
		// System.out.println("[Output from log4j] After Creating Thread Pool");
		try{
			// System.out.println(" [Output from log4j] Entering the HttpServer Default Constructor");
			// Milestone 2 -- 23rd February 2015
			// parsing web.xml
			Handler handlerObj = ERISServletHarness.parseWebdotxml(HttpServer.webdotxml_path);
			ERISServletContext context = ERISServletHarness.createContext(handlerObj);
			DaemonThread.servletPool = ERISServletHarness.createServlets(handlerObj, context);

			// System.out.println("[Output from log4j] After Creating Servlets");

			// print servletPool hash map
			for (String name: DaemonThread.servletPool.keySet()){
				String key =name.toString();
				String value = DaemonThread.servletPool.get(name).toString();  
				// System.out.println(key + " " + value);  
			} 

			// to create a list of sessions
			DaemonThread.sessionList = new ArrayList<ERISSession>();
			//DaemonThread.servletPool = ERISServletHarness.createServlets(handlerObj, context);
		}
		catch(Exception e){
			// e.printStackTrace();
			System.err.println(" [Output from log4j] Error Occured in Daemon Thread : " + e);
			logger.error(" [Output from log4j] Error Occured in Daemon Thread : " + e);
		}


	}

	public static void addSession (ERISSession sessionObj) {
		DaemonThread.sessionList.add(sessionObj);
	}

	/**
	 * Method which pushes an element passed as argument to shared queue.
	 * @param clientSocket - client request/item to be added
	 * @throws InterruptedException
	 */
	private void addToQueue(Socket clientSocket) {
		try{
			//// System.out.println("[Output from log4j] Adding element to queue");
			//logger.info("[Output from log4j] Adding element to queue");//This would be logged in the log file created and to the console.
			// Synchronizing on the sharedQueue to make sure no more than one thread is accessing the queue same time.
			//Adding element to queue and notifying all waiting consumers
			synchronized (sharedQueue) {
				sharedQueue.add(clientSocket);
				sharedQueue.notify();
			}
		}
		catch(Exception e) {
			logger.error(" [Output from log4j] Error from enqueue function " + e);
			//System.err.println(" [Output from log4j] Error from enqueue function " + e);
			//e.printStackTrace();
		}
	}

	public void run() {
		// write function to add the client request to the queue

		//logger.info("[Output from log4j] Starting the Server Thread");
		//// System.out.println("[Output from log4j] Starting the Server Thread");

		//Thread currentThread = Thread.currentThread(); 
		//logger.info("[Output from log4j] Server Thread "+ currentThread );
		//// System.out.println("[Output from log4j] Server Thread "+ currentThread );

		try{
			DaemonThread.serverSocket = new ServerSocket (DaemonThread.serverPort);
		}
		catch (IOException e){
			logger.error(" [Output from log4j] Cannot Open port "+ DaemonThread.serverPort +" " + e);
			//System.err.println(" [Output from log4j] Cannot Open port "+ DaemonThread.serverPort +" " + e);
			//e.printStackTrace();
		}

		// infinite loop to add any client request coming into the socket
		while (DaemonThread.alive){
			//// System.out.println(" [Output from log4j] Entering infinite loop Daemon Thread ");
			Socket clientSocket = null;
			try{
				clientSocket = DaemonThread.serverSocket.accept();
			}
			catch (Exception e) {
				logger.error(" [Output from log4j] Error accepting client socket "+ e);
				//System.err.println(" [Output from log4j] Error accepting client socket "+ e);
				//e.printStackTrace();
			}
			try {
				//logger.info(" [Output from log4j] Adding client socket to queue " + clientSocket );
				//// System.out.println(" [Output from log4j] Adding client socket to queue " + clientSocket );
				addToQueue (clientSocket);
			}
			catch (Exception e){
				logger.error(" [Output from log4j] Error adding client socket to queue " + e);
				//System.err.println(" [Output from log4j] Error adding client socket to queue " + e);
				//e.printStackTrace();
			}
		}
		// to changing state of threads 
		//// System.out.println(" [Output from log4j] Out of infinite Daemon Thread loop ");
		synchronized (sharedQueue) {
			sharedQueue.notifyAll();
		}
		// join loop for waiting for the runnable threads
		for (Thread thread : threadPool) {
			if (thread.getState()==Thread.State.RUNNABLE) {
				try {
					thread.join();
				} 
				catch (InterruptedException e) {
					logger.error(" [Output from log4j] Error while joining threads " + e);
					//System.err.println(" [Output from log4j] Error while joining threads " + e);
					//e.printStackTrace();
				}
			}
		}
		// destroying servlets 
		for(HttpServlet httpObj : DaemonThread.servletPool.values()) {
			httpObj.destroy();
		}
		DaemonThread.servletPool.clear();

		//// System.out.println(" [Output from log4j] End of run function for Daemon Thread");
	}
}