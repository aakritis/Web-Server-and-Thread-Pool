package edu.upenn.cis.cis455.webserver;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import edu.upenn.cis.cis455.webserver.ERISServletHarness;
import edu.upenn.cis.cis455.webserver.ERISServletHarness.Handler;

/**
 * HTTPServer Class
 * 
 * About the problem we are solving:
 * We try to create a ThreadPooled Server in Java to handle HTTP Request/Response problems. 
 * We create a Daemon thread and a Worker thread and use a common queue(ArrayList here) 
 * where the Daemon Thread pushes and Worker Thread consumes from.
 * The delay added in the Worker thread ensures wait and notifyAll gets called.
 * 
 */
@SuppressWarnings("unused")
public class HttpServer {

	// Using log4j framework for logging
	static final Logger logger = Logger.getLogger(HttpServer.class);
	static int serverPort = 0;
	// Milestone 2 -- 23rd February 2015
	static String webdotxml_path = null;

	public static void main (String args[]){
		try{		
			String homeDirectory = "";
			boolean isValid = true;

			if(args.length == 0 ) {
				isValid = false;
				// System.out.println("Name : Aakriti Singla");
				// System.out.println("Seas Login Account : aakritis@seas.upenn.edu");
				return;
			}
			else if (args.length == 1) {
				isValid = false;
				return;
			}
			else if (args.length >= 2 ) {
				isValid = true;
				serverPort = Integer.parseInt(args[0]);
				String homeDir = args[1].toString();
				// Milestone 2 -- 23rd February 2015 
				// adding 3rd command line argument (path to web.xml) 
				HttpServer.webdotxml_path = args[2].toString();
				if (homeDir.substring(homeDir.length() -1).equals("/")) {
					homeDirectory = homeDir.substring(0, homeDir.length() - 1);
				}
				else {
					homeDirectory = homeDir;
				}
			}

			// if the request is a valid request n
			if (isValid) {
				// HashMap<String,HttpServlet> servlets = ERISServletHarness.createServlets(handlerObj, context);

				DaemonThread daemonThread = new DaemonThread (serverPort, homeDirectory);
				// System.out.println(" [Output from log4j] After Starting the DaemonTHread ");
				Thread thread = new Thread (daemonThread,"Daemon Thread");
				thread.start();
			}
		}
		catch (Exception e){
			//logger.error(" [Output from log4j] Error in main "+ e);
			//System.err.println(" [Output from log4j] Error in main "+ e);
			//e.printStackTrace();
		}
	}
}