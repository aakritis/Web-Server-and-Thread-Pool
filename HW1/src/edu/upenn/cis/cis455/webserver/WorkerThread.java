package edu.upenn.cis.cis455.webserver;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.net.Socket;
import java.net.InetAddress;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;

import edu.upenn.cis.cis455.webserver.ERISServletHarness;
import edu.upenn.cis.cis455.webserver.ERISServletHarness.Handler;

/**
 * Class for Worker thread.
 * 
 * Worker Thread reads Socket Objects from the shared queue as long as it is not
 * empty. After Reading the objects, it will process the client requests and
 * send the response.
 */
@SuppressWarnings("unused")
public class WorkerThread extends Thread {

	// Using log4j framework for logging
	static final Logger logger = Logger.getLogger(WorkerThread.class);
	protected ArrayList<Socket> sharedQueue;
	protected ArrayList<Thread> threadPool;
	// protected Socket clientSocket = null;
	protected HashMap<String, String> mapRequestData = null;
	// to check that if head request is created , return only HTTP response
	// headers
	boolean isHead = false;
	boolean isExpect = false;
	boolean isHTTPLatest = false;
	boolean isGet = false;
	boolean isModified = false;
	boolean isUnmodified = false;
	// for Milestone 2 -- 23rd February 2015
	boolean isPost = false;
	boolean isServlet = false;
	String required_url = "";

	String url_pattern_xml;

	HttpServlet servlet;

	DataOutputStream data_out = null;
	// for setting the value for the Request class -- Milestone 2 -- 24th Feb 2015 
	String servlet_path = null;
	String path_info = null;
	String query_string = null;

	BufferedReader brObj_body = null;
	// to store the host name passed in the request from the client
	// to use in ERISServletContext class
	static protected String hostname = null;



	protected enum FileTypes {
		jpg, png, gif, txt, html;
	}

	public WorkerThread(ArrayList<Socket> sharedQueue,
			ArrayList<Thread> threadPool) {
		this.sharedQueue = sharedQueue;
		this.threadPool = threadPool;
	}

	/**
	 * Method to read from the queue.
	 * 
	 * @return - element read from queue
	 * @throws InterruptedException
	 */
	private Socket readFromQueue() {
		try {
			while (sharedQueue.isEmpty()) {
				// // System.out.println(" [Output from log4j] Entering empty dequeue function ");
				// If the queue is empty, we push the current thread to waiting
				// state. Way to avoid polling.
				synchronized (sharedQueue) {
					// logger.info(" [Output from log4j] Queue is currently empty ");
					// // System.out.println(" [Output from log4j] Queue is currently empty ");
					sharedQueue.wait();
					// control appears here after notifyAll()
					if (!DaemonThread.alive) {
						return null;
					}
				}
			}
			// Otherwise consume element and notify waiting producer
			synchronized (sharedQueue) {
				Socket clientSocket = null;
				// // System.out.println(" [Output from log4j] Entering sync dequeue function ");
				clientSocket = (Socket) sharedQueue.remove(0);
				sharedQueue.notify();
				// logger.info(" [Output from log4j] Client socket dequeued for processing "
				// + clientSocket);
				// // System.out.println(" [Output from log4j] Client socket dequeued for processing "
				// + clientSocket);
				return clientSocket;
			}
		} 
		catch (Exception e) {
			logger.error(" [Output from log4j] Error from dequeue function " + e);
			System.err.println(" [Output from log4j] Error from dequeue function "+ e);
			// e.printStackTrace();
			return null;
		}
	}

	public void run() {
		// write code to read from queue and call function to process request
		// change method to call processRequest in synchronized loop
		try {
			while (DaemonThread.alive) {
				try {
					Socket clientSocket = null;
					// // System.out.println(" [Output from log4j] Before entering the dequeue function ");
					clientSocket = readFromQueue();
					if (clientSocket == null && !(DaemonThread.alive)) {
						// // System.out.println(" [Output from log4j] End of run function for Worker Thread");
						return;
					}
					// // System.out.println(" [Output from log4j] Before entering the process Request Function ");
					this.processRequest(clientSocket);

					// closing the socket after processing of request
					try {
						// // System.out.println(" [Output from log4j] Closing client Socket after processing request ");
						clientSocket.close();
						// // System.out.println(" [Output from log4j] After Closing Socket ");
					}
					catch (Exception ex) {
						logger.error(" [Output from log4j] Error closing client Socket " + ex);
						System.err
						.println(" [Output from log4j] Error closing client Socket "
								+ ex);
						ex.printStackTrace();
					}
				} catch (Exception e) {
					logger.error(" [Output from log4j] Error dequeuing client request " + e);
					System.err.println(" [Output from log4j] Error dequeuing client request "+ e);
					// e.printStackTrace();
				}
			}
			// // System.out.println(" [Output from log4j] End of run function for Worker Thread");
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while exiting Run function for Worker Thread " + e);
			System.err.println(" [Output from log4j] Error while exiting Run function for Worker Thread " + e);
			// e.printStackTrace();
		}
	}

	public void processRequest(Socket clientSocket) {
		// write code to process the incoming requests
		// logger.info(" [Output from log4j] Entering processRequest function");
		// // System.out.println(" [Output from log4j] Entering processRequest function");

		// for Milestone 2 -- 23rd February 2015
		int index_line = 0;
		BufferedReader brObj = null;
		StringBuffer requestData = new StringBuffer("");
		String line = null;
		try {
			// System.out.println("1");
			brObj = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			// System.out.println("2");
			this.data_out = new DataOutputStream(clientSocket.getOutputStream());

			// for Milestone 2 -- 24th feb 2015
			this.brObj_body = brObj;
			// System.out.println("3");
			while ((line = brObj.readLine()).length() != 0) {
				// for Milestone 2 -- 23rd February 2015
				index_line += 1;
				if (line.toLowerCase().startsWith("post") && index_line == 1) {
					isPost = true;
					// break;
				}
				// logger.info(" [Output from log4j] Data Received for processing "+
				// line.toString());
				// // System.out.println(" [Output from log4j] Data Received for processing "+
				// line.toString());
				requestData.append(line + "\t");

			}
			// logger.info(" [Output from log4j] Data from Input Reader captured.");
			// // System.out.println(" [Output from log4j] Data from Input Reader captured.");
		} catch (IOException e) {
			// //e.printStackTrace();
			logger.error(" [Output from log4j] Error reading client request " + e);
			System.err.println(" [Output from log4j] Error reading client request "	+ e);
			// e.printStackTrace();
		}

		// logger.info(" [Output from log4j] Client Socket Data for processing "
		// + requestData);
		// // System.out.println(" [Output from log4j] Client Socket Data for processing "
		// + requestData);

		// for Milestone 2 -- 23rd February 2015
		try {
			// System.out.println("4");
			if (isPost) {
				// create hash map for POST Method
				// boolean isCorrect = this.hashRequestDataForPOST(line,
				// clientSocket);
				// System.out.println("5");
				boolean isCorrect = this.hashRequestData(requestData.toString(),clientSocket);
				// System.out.println(" [Output from log4j] Request Data contains :" + requestData);
				if (isCorrect) {
					// check for url pattern matching
					// System.out.println("6");
					// System.out.println(" [Output from log4j] Entering parsing function");
					boolean is_servlet_process = parse_requested_uri(this.required_url);
					// System.out.println(" [Output from log4j] After parsing function " + this.required_url);
					if(is_servlet_process && this.servlet != null) {
						// invoke servlet
						try {
							//String prefixWithSlash = Pattern.compile("\\*").split(this.url_pattern_xml)[0];
							//String prefix = prefixWithSlash.substring(0, prefixWithSlash.length() - 1);
							//this.servlet_path = prefix;
							//this.path_info = this.required_url.substring(this.servlet_path.length(), this.required_url.length());

							// System.out.println(" [Output from log4j] Values set for Servlet Path " + this.servlet_path + "	Path Info " + this.path_info);
							// System.out.println(" [Output from log4j] Entering Invoking Servlet function ");
							this.invoke_servlets(clientSocket);
						} 
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			} else {
				// to close connection if the data is empty in the clientSocket
				// System.out.println("7");
				if (requestData.equals("")) {
					return;
				}
				// System.out.println("8");
				boolean isCorrect = this.hashRequestData(requestData.toString(),
						clientSocket);
				if (isCorrect) {
					// as per milestone 2 
					// System.out.println(" [Output from log4j] Entering parsing function");
					boolean is_servlet_process = parse_requested_uri(this.required_url);
					// System.out.println(" [Output from log4j] After parsing function " + this.required_url);
					if(is_servlet_process && this.servlet != null) {
						// invoke servlets
						try {
							//String prefixWithSlash = Pattern.compile("\\*").split(this.url_pattern_xml)[0];
							//String prefix = prefixWithSlash.substring(0, prefixWithSlash.length() - 1);
							//this.servlet_path = prefix;
							//this.path_info = this.required_url.substring(this.servlet_path.length(), this.required_url.length());

							// System.out.println(" [Output from log4j] Entering Invoking Servlet function ");
							this.invoke_servlets(clientSocket);
						} 
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else {
						this.createRespone(clientSocket);
					}
				}
			}
			return;
		}
		catch (Exception e) {
			System.err.println(" [Output from log4j] Error while checking Milestone 2 coding :" + e);
		}
	}

	public void invoke_servlets(Socket clientSocket) throws IOException {
		try{
			System.out.println("[debug] in invoke_servlet");
			ERISSession sessionObj = null;
			// ERISServletRequest req = new ERISServletRequest ();
			ERISServletContext ctObj = (ERISServletContext)servlet.getServletContext();
			// System.out.println("+ Session obj val + " + sessionObj);
			ERISServletRequest req = new ERISServletRequest (clientSocket, this.mapRequestData, ctObj , sessionObj);
			//ERISServletContext context = (ERISServletContext)servlet.getServletContext();
			//ERISServletRequest req = new ERISServletRequest();
			//ERISServletRequest req = new ERISServletRequest (clientSocket, this.mapRequestData , sessionObj);
			//ERISServletResponse responseObj = new ERISServletResponse (requestObj);
			// System.out.println (" [Output from log4j] Invoking Servlets");
			for (String key : this.mapRequestData.keySet()) {
				//// System.out.println("After 1");
				System.out.println("[debug] key +" + key);
				if (key.equalsIgnoreCase("cookie")) {
					System.out.println("[debug]Entering Cookie loop");
					//// System.out.println("After 2");
					String[] cookies = this.mapRequestData.get(key).split("; ");
					for (String ck : cookies) {
						String key_cookie = ck.split("=")[0].trim();
						String value_cookie = ck.split("=")[1].trim();
						System.out.println("Key" + key_cookie + "	Value" + value_cookie);
						if (key_cookie.equalsIgnoreCase("JSESSIONID")) {
							/*for(ERISSession sess : DaemonThread.sessionList){
							if(sess.isValid())
								if(sess.getId().equals(value_cookie))
									sessionObj = sess;
						}*/
							ERISServletContext con = (ERISServletContext) req.getAttribute("Servlet-Context");
							ERISSession sess = (ERISSession)con.getAttribute("Session");
							if(sess.getAttribute("id").equals(value_cookie))
								sessionObj = sess;
							System.out.println("[Debug] sessionObj +" + sessionObj);
							// System.out.println("After 3");
						}
					}
				}
			}

			

			// System.out.println("After 4");
			// create HttpServletRequest
			ERISServletContext contextObj = (ERISServletContext)servlet.getServletContext();
			// System.out.println("+ Session obj val + " + sessionObj);
			ERISServletRequest requestObj = new ERISServletRequest (clientSocket, this.mapRequestData, contextObj , sessionObj);
			requestObj.setMethod(this.mapRequestData.get("Method"));
			// requestObj.setProtocol(this.mapRequestData.get("Version"));
			requestObj.setServletPath(this.servlet_path);
			requestObj.setPathInfo(this.path_info);
			requestObj.setQueryString(this.query_string);

			// set parameters for query string
			if(this.query_string != null && !this.query_string.isEmpty()){
				String[] queries = this.query_string.split("\\&");
				for(String query : queries){
					String k = query.split("=")[0];
					String v = query.split("=")[1];
					requestObj.setParameter(k, v);
					// System.out.println("Key : " + k + "	Value : " + v);
				}
			}

			// create HttpServletResponse
			// System.out.println("Last 1" + requestObj + " " + this.data_out);
			ERISServletResponse responseObj = new ERISServletResponse(requestObj, this.data_out);
			// System.out.println("Last 2 " + responseObj);
			// System.out.println("After 6");
			String serverName = this.servlet.getServletContext().getServerInfo();
			responseObj.setHeader("Server", serverName);

			try{
				// start service 
				// System.out.println (" [Output from log4j] Invoking Servlets Service");
				this.servlet.service(requestObj, responseObj);
				// System.out.println (" [Output from log4j] After Servlets Service");

				if(!responseObj.isCommitted()) {
					// System.out.println (" [Output from log4j] Inside Not Committed Service");
					try{
						responseObj.flushBuffer();
					}
					catch(Exception e){
						System.err.println (" [Output from log4j] Error flush buffer " + e);
					}
					// System.out.println (" [Output from log4j] After flush buffer");
				}

			}
			catch (ServletException e) {
				e.printStackTrace();
				logger.error (" [Output from log4j] Error Servlets Service " + e);
				System.err.println (" [Output from log4j] Error Servlets Service " + e);
				//sendResponse(500, false);
			}
			finally {
				this.data_out.flush();
			}
			return;
		}
		catch(Exception e){

		}
	}

	public void createRespone(Socket clientSocket) {
		// fetch values from HashMap
		String requestMethod = null;
		String requestPath = null;
		String requestVersion = null;
		try {
			for (@SuppressWarnings("rawtypes")
			Map.Entry map : this.mapRequestData.entrySet()) {
				if ((String) map.getKey() == "Method") {
					requestMethod = map.getValue().toString();
					// logger.info(" [Output from log4j] Data captured for Response Header : Method : "
					// + requestMethod );
					// // System.out.println(" [Output from log4j] Data captured for Response Header : Method : "
					// + requestMethod );
				} else if ((String) map.getKey() == "Path") {
					requestPath = map.getValue().toString();
					// logger.info(" [Output from log4j] Data captured for Response Header : Path : "
					// + requestPath );
					// // System.out.println(" [Output from log4j] Data captured for Response Header : Path : "
					// + requestPath );
				} else if ((String) map.getKey() == "Version") {
					requestVersion = map.getValue().toString();
					// logger.info(" [Output from log4j] Data captured for Response Header : Version : "
					// + requestVersion );
					// // System.out.println(" [Output from log4j] Data captured for Response Header : Version : "
					// + requestVersion );
				}
			}

			// creating Hash Map to store values for Thread and corresponding
			// Path
			Thread currentThread = Thread.currentThread();
			DaemonThread.mapThreadRequestData.put(currentThread.getName(), requestPath);

			if (requestMethod.toLowerCase().equals("get")
					|| requestMethod.toLowerCase().equals("head")) {
				if (requestMethod.toLowerCase().equals("head")) {
					isHead = true;
				} 
				else {
					isGet = true;
				}
				String hostName = null;
				// for compliance with HTTP 1.1 requests
				if (requestVersion.equalsIgnoreCase("HTTP/1.1")) {
					isHTTPLatest = true;
					if ((this.mapRequestData.containsKey("Expect"))) {
						isExpect = true;
					}
					if (!this.mapRequestData.containsKey("Host")) {
						// System.err.println(" [Output from log4j] Error while loading the requested file : Bad Request");
						int errorStatusCode = 400;
						this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
					} 
					else if (this.mapRequestData.containsKey("Host")) {
						if ((hostName = this.mapRequestData.get("Host"))
								.length() == 0) {
							int errorStatusCode = 400;
							this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
						} 
						else {
							// to set the static variable for accessing in
							// ERISServletContext
							WorkerThread.hostname = this.mapRequestData.get("Host");
							// // System.out.println(" [Output from log4j] Entering loop after host check");
							// check scenarios for if-modified-since
							if (isGet) {
								if (this.mapRequestData.containsKey("If-Modified-Since") && this.mapRequestData.containsKey("If-Unmodified-Since")) {
									// System.err.println(" [Output from log4j] Error while loading the requested file : BAD Request");
									int errorStatusCode = 400;
									this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
								}
								else if (this.mapRequestData.containsKey("If-Modified-Since")) {
									// String modifiedDate =
									// this.mapRequestData.get("If-Modified-Since");
									isModified = true;
									this.fetchRequestForm(requestPath,requestVersion, clientSocket);
								} 
								else if (this.mapRequestData.containsKey("If-Unmodified-Since")) {
									// String unmodifiedDate =
									// this.mapRequestData.get("If-Unmodified-Since");
									isUnmodified = true;
									this.fetchRequestForm(requestPath,requestVersion, clientSocket);
								} 
								else {
									this.fetchRequestForm(requestPath,requestVersion, clientSocket);
								}
							} 
							else {
								if (this.mapRequestData.containsKey("If-Unmodified-Since")) {
									isUnmodified = true;
									this.fetchRequestForm(requestPath,requestVersion, clientSocket);
								} 
								else {
									this.fetchRequestForm(requestPath,requestVersion, clientSocket);
								}
							}
						}
					}
				} else {
					this.fetchRequestForm(requestPath, requestVersion,clientSocket);
				}
			} else {
				// handle error case for Not Implemented
				// System.err.println(" [Output from log4j] Error while loading the requested file : FILE NOT FOUND");
				int errorStatusCode = 501;
				this.createErrorResponse(errorStatusCode, requestVersion,clientSocket);
			}
		} 
		catch (Exception e) {
			logger.error(" [Output from log4j] Error While capturing data for Response Header " + e );
			System.err.println(" [Output from log4j] Error While capturing data for Response Header "+ e);
			// e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public void fetchRequestForm(String requestPath, String requestVersion,Socket clientSocket) {
		String absoluteFilePath = null;
		requestPath = requestPath.replace("%20", " ");
		int errorStatusCode;
		try {
			if (requestPath.length() == 0) {
				// call function to recursively call and get contents of root
				// directory.
				int defaultDirType = 1;
				absoluteFilePath = DaemonThread.homeDirectory + "/";

				if (isModified) {
					String modifiedDate = this.mapRequestData.get("If-Modified-Since");
					String compareModifiedDate = this.updateDateFormat(modifiedDate);
					// get last modified date for a directory
					File fdir = new File(absoluteFilePath);
					SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
					String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

					// write code to compare dates and proceed
					try {
						Date dHeadDate = sdf.parse(compareModifiedDate);
						Date dFileDate = sdf.parse(fileModifiedDate);
						if (dHeadDate.compareTo(dFileDate) <= 0) {
							this.getDirectoryContents(
									URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,defaultDirType);
						} else {
							// System.err.println(" [Output from log4j] Error with If Modified Since");
							errorStatusCode = 304;
							this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
						}
					} 
					catch (Exception ex) {
						logger.error(" [Output from log4j] Error while comparing dates " + ex );
						System.err.println(" [Output from log4j] Error while comparing dates "+ ex);
						ex.printStackTrace();
					}
				} 
				else if (isUnmodified) {
					String unmodifiedDate = this.mapRequestData.get("If-Unmodified-Since");
					String compareUnmodifiedDate = this.updateDateFormat(unmodifiedDate);
					// get last modified date for a directory
					File fdir = new File(absoluteFilePath);
					SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
					String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

					// write code to compare dates and proceed
					try {
						Date dHeadDate = sdf.parse(compareUnmodifiedDate);
						Date dFileDate = sdf.parse(fileModifiedDate);
						if (dHeadDate.compareTo(dFileDate) >= 0) {
							this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,defaultDirType);
						} 
						else {
							// System.err.println(" [Output from log4j] Error with If UnModified Since");
							errorStatusCode = 412;
							this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);

						}
					} 
					catch (Exception ex) {
						logger.error(" [Output from log4j] Error while comparing dates " + ex );
						System.err.println(" [Output from log4j] Error while comparing dates "+ ex);
						ex.printStackTrace();
					}

				} 
				else {
					this.getDirectoryContents(
							URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket, defaultDirType);
				}
				// check where to call this function
				// this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),
				// requestVersion, clientSocket, defaultDirType);
			} 
			else if (requestPath.length() == 1) {
				if (requestPath.equals("/")) {
					// call function to recursively call and get contents of
					// root directory.
					int defaultDirType = 1;
					absoluteFilePath = DaemonThread.homeDirectory + requestPath;

					if (isModified) {
						String modifiedDate = this.mapRequestData.get("If-Modified-Since");
						String compareModifiedDate = this.updateDateFormat(modifiedDate);
						// get last modified date for a directory
						File fdir = new File(absoluteFilePath);
						SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
						String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

						// write code to compare dates and proceed
						try {
							Date dHeadDate = sdf.parse(compareModifiedDate);
							Date dFileDate = sdf.parse(fileModifiedDate);
							if (dHeadDate.compareTo(dFileDate) <= 0) {
								this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,defaultDirType);
							} 
							else {
								// System.err.println(" [Output from log4j] Error with If Modified Since");
								errorStatusCode = 304;
								this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
							}
						} 
						catch (Exception ex) {
							logger.error(" [Output from log4j] Error while comparing dates " + ex );
							System.err.println(" [Output from log4j] Error while comparing dates " + ex);
							// ex.printStackTrace();
						}

					} 
					else if (isUnmodified) {
						String unmodifiedDate = this.mapRequestData.get("If-Unmodified-Since");
						String compareUnmodifiedDate = this.updateDateFormat(unmodifiedDate);
						// get last modified date for a directory
						File fdir = new File(absoluteFilePath);
						SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
						String fileModifiedDate = sdf.format( fdir.lastModified()).toString() + "GMT";

						// write code to compare dates and proceed
						// write code to compare dates and proceed
						try {
							Date dHeadDate = sdf.parse(compareUnmodifiedDate);
							Date dFileDate = sdf.parse(fileModifiedDate);
							if (dHeadDate.compareTo(dFileDate) >= 0) {
								this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,	defaultDirType);
							} else {
								// System.err.println(" [Output from log4j] Error with If UnModified Since");
								errorStatusCode = 412;
								this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);

							}
						} 
						catch (Exception ex) {
							// ex.printStackTrace();
							logger.error(" [Output from log4j] Error while comparing dates " + ex );
							System.err.println(" [Output from log4j] Error while comparing dates " + ex);
						}
					} else {
						this.getDirectoryContents(
								URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket, defaultDirType);
					}

					// this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),
					// requestVersion, clientSocket, defaultDirType);
				}
			} else {
				boolean isSpecialCase = false;
				if (requestPath.equalsIgnoreCase("/shutdown")) {
					// function to call graceful shutdown to the system!!
					isSpecialCase = true;
					this.createShutdownResponse(absoluteFilePath,requestVersion, clientSocket);
					this.implementShutDown();
				} 
				else if (requestPath.equalsIgnoreCase("/control")) {
					// function to display the control panel for the server..
					isSpecialCase = true;
					this.fetchControlPanel(requestVersion, clientSocket);
				}
				if (!isSpecialCase) {
					absoluteFilePath = DaemonThread.homeDirectory + requestPath;
					boolean isExist = true;
					if (!(new File(absoluteFilePath).exists())) {
						// call function to generate File Not Found Error
						// System.err.println(" [Output from log4j] Error while loading the requested file : FILE NOT FOUND");
						errorStatusCode = 404;
						this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
						isExist = false;
					}
					if (isExist) {
						if (new File(absoluteFilePath).isDirectory()) {
							// call function to get contents of the current
							// directory
							int defaultDirType = 0;

							if (isModified) {
								String modifiedDate = this.mapRequestData.get("If-Modified-Since");
								String compareModifiedDate = this.updateDateFormat(modifiedDate);
								// get last modified date for a directory
								File fdir = new File(absoluteFilePath);
								SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
								String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

								// write code to compare dates and proceed
								try {
									Date dHeadDate = sdf.parse(compareModifiedDate);
									Date dFileDate = sdf.parse(fileModifiedDate);
									if (dHeadDate.compareTo(dFileDate) <= 0) {
										this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,	defaultDirType);
									} 
									else {
										// System.err.println(" [Output from log4j] Error with If Modified Since");
										errorStatusCode = 304;
										this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);

									}
								} 
								catch (Exception ex) {
									logger.error(" [Output from log4j] Error while comparing dates " + ex );
									System.err.println(" [Output from log4j] Error while comparing dates " + ex);
									// ex.printStackTrace();
								}
							} 
							else if (isUnmodified) {
								String unmodifiedDate = this.mapRequestData.get("If-Unmodified-Since");
								String compareUnmodifiedDate = this.updateDateFormat(unmodifiedDate);
								// get last modified date for a directory
								File fdir = new File(absoluteFilePath);
								SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
								String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

								// write code to compare dates and proceed
								// write code to compare dates and proceed
								try {
									Date dHeadDate = sdf.parse(compareUnmodifiedDate);
									Date dFileDate = sdf.parse(fileModifiedDate);
									if (dHeadDate.compareTo(dFileDate) >= 0) {
										this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,defaultDirType);
									} 
									else {
										// System.err.println(" [Output from log4j] Error with If UnModified Since");
										errorStatusCode = 412;
										this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
									}
								} 
								catch (Exception ex) {
									logger.error(" [Output from log4j] Error while comparing dates " + ex );
									System.err.println(" [Output from log4j] Error while comparing dates " + ex );
									// ex.printStackTrace();
								}
							} 
							else {
								this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket,	defaultDirType);
							}

							// this.getDirectoryContents(URLDecoder.decode(absoluteFilePath),
							// requestVersion, clientSocket, defaultDirType);
						} else if (new File(absoluteFilePath).isFile()) {
							// call function to get contents of file

							if (isModified) {
								String modifiedDate = this.mapRequestData.get("If-Modified-Since");
								String compareModifiedDate = this.updateDateFormat(modifiedDate);
								// get last modified date for a directory
								File fdir = new File(absoluteFilePath);
								SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
								String fileModifiedDate = sdf.format(fdir.lastModified()).toString() + "GMT";

								// write code to compare dates and proceed
								try {
									Date dHeadDate = sdf.parse(compareModifiedDate);
									Date dFileDate = sdf.parse(fileModifiedDate);
									if (dHeadDate.compareTo(dFileDate) <= 0) {
										this.createFileResponse(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket);
									} else {
										// System.err.println(" [Output from log4j] Error with If Modified Since");
										errorStatusCode = 304;
										this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
									}
								} 
								catch (Exception ex) {
									logger.error(" [Output from log4j] Error while comparing dates " + ex );
									System.err.println(" [Output from log4j] Error while comparing dates " + ex);
									// ex.printStackTrace();
								}
							} else if (isUnmodified) {
								String unmodifiedDate = this.mapRequestData.get("If-Unmodified-Since");
								String compareUnmodifiedDate = this.updateDateFormat(unmodifiedDate);
								// get last modified date for a directory
								File fdir = new File(absoluteFilePath);
								SimpleDateFormat sdf = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
								String fileModifiedDate = sdf.format(fdir.lastModified()).toString()+ "GMT";

								// write code to compare dates and proceed
								// write code to compare dates and proceed
								try {
									Date dHeadDate = sdf.parse(compareUnmodifiedDate);
									Date dFileDate = sdf.parse(fileModifiedDate);
									if (dHeadDate.compareTo(dFileDate) >= 0) {
										this.createFileResponse(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket);
									} else {
										// System.err.println(" [Output from log4j] Error with If UnModified Since");
										errorStatusCode = 412;
										this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);

									}
								} catch (Exception ex) {
									logger.error(" [Output from log4j] Error while comparing dates "+ ex );
									System.err.println(" [Output from log4j] Error while comparing dates "+ ex);
									// ex.printStackTrace();
								}
							} else {
								this.createFileResponse(URLDecoder.decode(absoluteFilePath),requestVersion, clientSocket);
							}
							// this.createFileResponse(URLDecoder.decode(absoluteFilePath),
							// requestVersion, clientSocket);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while checking type of request path " + e);
			System.err.println(" [Output from log4j] Error while checking type of request path " + e);
			// e.printStackTrace();
		}
	}

	public void implementShutDown() {
		DaemonThread.alive = false;
		try {
			DaemonThread.serverSocket.close();
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while closing Server Socket "+ e);
			System.err.println(" [Output from log4j] Error while closing Server Socket "+ e);
			// e.printStackTrace();
		}
	}

	public void fetchControlPanel(String requestVersion, Socket clientSocket) {
		try {
			// creating header for control panel
			String htmlResponse = "<!DOCTYPE html><html><head><title>My Control Panel</title></head><body>";
			htmlResponse += "<font color = '#660033'><h1 align='middle'>ERIS 1.0</h1><hr></font>";
			htmlResponse += "<p><font color = '#0033CC'>";
			htmlResponse += "<h4 align='middle' padding = '0' margin = '0'>Server Owner : Aakriti Singla<br/>";
			htmlResponse += "SEAS Login Account : aakritis@seas.upenn.edu</h4><hr>";

			// starting display for thread details
			htmlResponse += "<br/>";
			htmlResponse += "<table style = 'width:100%; height:100%'> <tr>";
			htmlResponse += "<td align = 'center'>Thread-Name</td> <td align = 'center'>Thread-Current-State</td><td align = 'center'>Thread-Request-Url</td></tr>";

			// accessing Thread Pool Data through Iterator Class
			@SuppressWarnings("rawtypes")
			Iterator iThread = DaemonThread.threadPool.iterator();
			while (iThread.hasNext()) {
				htmlResponse += "<tr>";
				Thread thread = (Thread) iThread.next();
				String threadName = thread.getName();
				State threadState = thread.getState();
				int state = 0;
				String threadURL = "No Request";
				if (threadState.toString().toLowerCase().equals("runnable")) {
					state = 1;
					threadURL = DaemonThread.mapThreadRequestData.get(threadName);
				} else {
					state = 0;
				}
				if (state == 1) {
					htmlResponse += "<td align = 'center'><font color = '#009933'>"
							+ threadName
							+ "</font></td><td align = 'center'><font color = '#009933'>"
							+ threadState
							+ "</font></td><td align = 'center'><font color = '#009933'>"
							+ threadURL + "</font></td></tr>";
				} else {
					htmlResponse += "<td align = 'center'><font color = '#FF5050'>"
							+ threadName
							+ "</font></td><td align = 'center'><font color = '#FF5050'>"
							+ threadState
							+ "</font></td><td align = 'center'><font color = '#FF5050'>"
							+ threadURL + "</font></td></tr>";
				}
			}
			htmlResponse += "</p></table><br/><br/>";
			String localAddress = InetAddress.getLocalHost().getHostAddress();
			String displayURL = "http://" + localAddress + ":"
					+ DaemonThread.serverPort + "/shutdown";
			htmlResponse += "<center><h3><a href = ' " + displayURL
					+ "'> SHUT DOWN </a></h3></center><br/><br/>";
			String log_url = "http://" + localAddress + ":"
					+ DaemonThread.serverPort + "/workspace/HW1/www/htmlLayout.html";
			htmlResponse += "<center><h3><a href = ' " + log_url
					+ "'> LOG </a></h3></center><br/><br/>";

			htmlResponse += "</font></body></html>";
			byte[] defaultHTMLBytes = new byte[(int) htmlResponse.length()];
			defaultHTMLBytes = htmlResponse.getBytes();

			// update contentLength
			int contentLength = htmlResponse.length();
			String contentType = "text/html";
			// Date currentDate = new Date();
			// String date = currentDate.toString();
			String date = this.getCurrentDate();

			String responseHeader = "";
			if (isExpect) {
				responseHeader += requestVersion + "100 Continue\r\n";
			}
			responseHeader += requestVersion + " 200 OK\r\n" + "Date: " + date
					+ "\r\n" + "Content-Type: " + contentType + "\r\n"
					+ "Content-Length: " + contentLength + "\r\n"
					+ "Connection: close\r\n" + "\n";
			// // System.out.println(" [Output from log4j] Response Header : " +
			// responseHeader);

			DataOutputStream dout = new DataOutputStream(
					clientSocket.getOutputStream());
			dout.writeUTF(responseHeader);
			if (!isHead) {
				dout.write(defaultHTMLBytes);
			}
			dout.flush();
			dout.close();
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while displaying contents of Control Page " + e );
			System.err.println(" [Output from log4j] Error while displaying contents of Control Page " + e);
			// e.printStackTrace();
		}
	}

	public String getCurrentDate() {
		try {
			SimpleDateFormat formatDate = new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss");
			formatDate.setTimeZone(TimeZone.getTimeZone("GMT"));
			String currentDate = formatDate.format(new Date()).toString()+ " GMT";
			return currentDate;
		} 
		catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	public String updateDateFormat(String requestDate) {
		String finalDate = null;
		try {
			if (requestDate.contains(",") && requestDate.contains("-")) {
				// Type 2 Date Format
				String[] spaceSplit = requestDate.split(" ");
				if (spaceSplit.length == 4) {
					String[] dateSplit = spaceSplit[1].split("-");
					if (dateSplit.length == 3) {
						String year = null;
						if (Integer.parseInt(dateSplit[2]) <= 65) {
							year = "20" + dateSplit[2];
						} else {
							year = "19" + dateSplit[2];
						}
						finalDate = spaceSplit[0].substring(0, 3) + ", "
								+ dateSplit[0] + " " + dateSplit[1] + " "
								+ year + " " + spaceSplit[2] + " GMT";
					}
				}
			} else if (requestDate.contains(",")) {
				// Type 1 Date Format
				finalDate = requestDate;
			} else {
				// Type 3 Date Format
				String[] dateParts = requestDate.split(" ");
				if (dateParts.length == 5) {
					finalDate = dateParts[0] + ", " + dateParts[2] + " "
							+ dateParts[1] + " " + dateParts[4] + " "
							+ dateParts[3] + " GMT";
				}
			}
			// logger.info(" [Output from log4j] Date Format for If-Modified-Since / If-Unmodified-since "
			// + finalDate );
			// // System.out.println(" [Output from log4j] Date Format for If-Modified-Since / If-Unmodified-since "
			// + finalDate );
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error : Date Format for If-Modified-Since / If-Unmodified-since "+ finalDate );
			System.err.println(" [Output from log4j] Error : Date Format for If-Modified-Since / If-Unmodified-since " + finalDate);
			// e.printStackTrace();
		}
		return finalDate;
	}

	// check for forbidden access error
	public void getDirectoryContents(String absoluteFilePath,
			String requestVersion, Socket clientSocket, int defaultDirType) {
		try {
			File directoryPath = new File(absoluteFilePath);
			String directoryActualPath = directoryPath.getCanonicalPath();
			if (!(directoryActualPath.startsWith(DaemonThread.homeDirectory))) {
				int errorStatusCode = 403;
				this.createErrorResponse(errorStatusCode, requestVersion,clientSocket);
			} else {
				String dirPath = directoryPath.getCanonicalPath();
				String[] dirName = dirPath.split("/");
				File[] directoryContent = directoryPath.listFiles();
				// creating response page for listing directory contents
				String htmlResponse = "<!DOCTYPE html><html><head><title>My Directory Listing</title></head><body><font color = '#0033CC'>";
				boolean isBackButton = true;
				if (defaultDirType == 1) {
					htmlResponse = htmlResponse
							+ "<h2 align='middle'>Directory Listing For : ROOT"
							+ "</h2>" + "<hr>";
					isBackButton = false;
				} else {
					htmlResponse = htmlResponse
							+ "<h2 align ='middle'>Directory Listing For : "
							+ dirName[dirName.length - 1] + "</h2>" + "<hr>";
				}
				htmlResponse = htmlResponse + "<br/><p><h4>";
				if (isBackButton) {
					String previousDirPath = dirPath.replace(dirName[(dirName.length - 1)], "");
					previousDirPath = previousDirPath.replace(DaemonThread.homeDirectory, "");
					String localAddress = InetAddress.getLocalHost().getHostAddress();
					String displayURL = "http://" + localAddress + ":"
							+ DaemonThread.serverPort + previousDirPath;
					htmlResponse += "<a href = ' " + displayURL
							+ "'>Back</a><br/><br/>";
				}

				for (File file : directoryContent) {
					String filePath = file.getCanonicalPath();
					String[] name = filePath.split("/");
					if (file.isDirectory()) {
						htmlResponse = htmlResponse
								+ "<font color = '#009933'>Sub-Directory : ";
						// create reference link

						String subFilePath = filePath.replace(DaemonThread.homeDirectory, "");
						// subFilePath = URLEncoder.encode(subFilePath,
						// "UTF-8");
						// subFilePath = URLEncoder.encode(subFilePath);
						String localAddress = InetAddress.getLocalHost().getHostAddress();
						String displayURL = "http://" + localAddress + ":"
								+ DaemonThread.serverPort + subFilePath;
						htmlResponse = htmlResponse + "<a href = '"
								+ displayURL + "'>";
						htmlResponse = htmlResponse + name[name.length - 1]
								+ "</a></font><br/>";
					} else {
						htmlResponse = htmlResponse
								+ "<font color = '#FF5050'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;File : ";
						htmlResponse = htmlResponse + name[name.length - 1]
								+ "</font><br/>";
					}
				}
				htmlResponse = htmlResponse + "</h4></p></font></body></html>";

				byte[] defaultHTMLBytes = new byte[(int) htmlResponse.length()];
				defaultHTMLBytes = htmlResponse.getBytes();

				// update contentLength
				int contentLength = htmlResponse.length();
				String contentType = "text/html";
				// Date currentDate = new Date();
				// String date = currentDate.toString();
				String date = this.getCurrentDate();

				String responseHeader = "";
				if (isExpect) {
					responseHeader += requestVersion + "100 Continue\r\n";
				}
				responseHeader += requestVersion + " 200 OK\r\n" + "Date: "
						+ date + "\r\n" + "Content-Type: " + contentType
						+ "\r\n" + "Content-Length: " + contentLength + "\r\n"
						+ "Connection: close\r\n" + "\n";
				// // System.out.println(" [Output from log4j] Response Header : "
				// + responseHeader);

				DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());
				dout.writeUTF(responseHeader);
				if (!isHead) {
					dout.write(defaultHTMLBytes);
				}
				dout.flush();
				dout.close();
			}
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while displaying contents of requested directory "	+ e );
			System.err.println(" [Output from log4j] Error while displaying contents of requested directory "+ e);
			// e.printStackTrace();
		}
	}

	public void createErrorResponse(int errorStatusCode, String requestVersion,
			Socket clientSocket) {
		try {
			String contentType = "text/html";
			// Date currentDate = new Date();
			// String date = currentDate.toString();
			String date = this.getCurrentDate();
			String errorStatusMsg = null;
			int contentLength = 0;

			switch (errorStatusCode) {
			case 404:
				errorStatusMsg = "Not Found";
				break;
			case 403:
				errorStatusMsg = "Forbidden";
				break;
			case 501:
				errorStatusMsg = "Not Implemented";
				break;
			case 505:
				errorStatusMsg = "HTTP Version Not Supported";
				break;
			case 400:
				errorStatusMsg = "Bad Request";
				break;
			case 412:
				errorStatusMsg = "Precondition Failed";
				break;
			case 304:
				errorStatusMsg = "Not Modified";
				break;
			}

			// create HTML response
			String htmlResponse = "<!DOCTYPE html><html><head><title>Error Page</title></head><body><font color = '#0033CC'><h1>HTTP Error "
					+ errorStatusCode
					+ " </h1><br/><h2>"
					+ errorStatusCode
					+ " " + errorStatusMsg + "</h2></font></body></html>";
			byte[] defaultHTMLBytes = new byte[(int) htmlResponse.length()];
			defaultHTMLBytes = htmlResponse.getBytes();
			// // System.out.println(" [Output from log4j] Response HTML : " +
			// htmlResponse );
			contentLength = htmlResponse.length();
			String responseHeader = "";
			if (isExpect) {
				responseHeader += requestVersion + "100 Continue\r\n";
			}
			responseHeader += requestVersion + " " + errorStatusCode + " "
					+ errorStatusMsg + "\r\n" + "Date: " + date + "\r\n"
					+ "Content-Type: " + contentType + "\r\n"
					+ "Content-Length: " + contentLength + "\r\n"
					+ "Connection: close\r\n" + "\n";
			// // System.out.println(" [Output from log4j] Response Header : " +
			// responseHeader);

			DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());
			dout.writeUTF(responseHeader);
			if (!isHead) {
				dout.write(defaultHTMLBytes);
			}
			dout.flush();
			dout.close();
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while creating Error Response "
					+ e );
			System.err
			.println(" [Output from log4j] Error while creating Error Response "
					+ e);
			// e.printStackTrace();
		}
	}

	public void createShutdownResponse(String absoluteFilePath,
			String requestVersion, Socket clientSocket) {
		try {
			String contentType = "text/html";
			String date = this.getCurrentDate();
			String errorStatusMsg = null;
			int contentLength = 0;
			String htmlResponse = "<!DOCTYPE html><html><head><title>Shutdown Page</title></head><body><font color = '#0033CC'><h1></h1><br/><h1>"
					+ "Shutting Down The Server...."
					+ "</h1></font></body></html>";
			byte[] defaultHTMLBytes = new byte[(int) htmlResponse.length()];
			defaultHTMLBytes = htmlResponse.getBytes();
			// // System.out.println(" [Output from log4j] Response HTML : " +
			// htmlResponse );
			contentLength = htmlResponse.length();
			String responseHeader = "";
			if (isExpect) {
				responseHeader += requestVersion + "100 Continue\r\n";
			}
			responseHeader += requestVersion + " " + "200 OK" + "\r\n"
					+ "Date: " + date + "\r\n" + "Content-Type: " + contentType
					+ "\r\n" + "Content-Length: " + contentLength + "\r\n"
					+ "Connection: close\r\n" + "\n";
			// // System.out.println(" [Output from log4j] Response Header : " +
			// responseHeader);

			DataOutputStream dout = new DataOutputStream(
					clientSocket.getOutputStream());
			dout.writeUTF(responseHeader);
			if (!isHead) {
				dout.write(defaultHTMLBytes);
			}
			dout.flush();
			dout.close();

		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while creating file response " + e);
			System.err.println(" [Output from log4j] Error while creating file response "	+ e);
			// e.printStackTrace();
		}
	}

	// check code for forbidden access error
	public void createFileResponse(String absoluteFilePath,
			String requestVersion, Socket clientSocket) {
		try {
			File fileRead = new File(absoluteFilePath);
			// // System.out.println(" [Output from log4j] Absolute File Path : " +
			// absoluteFilePath);
			// if-else loop for forbidden access error
			String fileActualPath = fileRead.getCanonicalPath();
			if (!(fileActualPath.startsWith(DaemonThread.homeDirectory))) {
				int errorStatusCode = 403;
				this.createErrorResponse(errorStatusCode, requestVersion,clientSocket);
			} else {
				if (fileRead.canRead()) {
					// reading the requested resource
					@SuppressWarnings("resource")
					FileInputStream fileStream = new FileInputStream(absoluteFilePath);
					// byte[] fileBytes = new byte[fileStream.available()];
					byte[] fileBytes = new byte[(int) fileRead.length()];
					// // System.out.println(" [Output from log4j] FileStream Length : "
					// + fileBytes.length);
					// // System.out.println(" [Output from log4j] File Length : "
					// + fileRead.length());
					fileStream.read(fileBytes);

					// String fileContent = new String (fileBytes);
					// // System.out.println(" [Output from log4j] File Content : "
					// + fileBytes);

					// creating response header
					String[] filePath = absoluteFilePath.split("\\.");
					// // System.out.println(" [Output from log4j] FilePath Length : "
					// + filePath.length);
					/*
					 * for (String check : filePath){
					 * // System.out.println(" [Output from log4j] Array Element : "
					 * + check); }
					 */
					String fileExtension = filePath[filePath.length - 1];

					FileTypes fileType = FileTypes.valueOf(fileExtension);
					String contentType = null;

					switch (fileType) {
					case jpg:
						contentType = "image/jpeg";
						break;
					case png:
						contentType = "image/png";
						break;
					case gif:
						contentType = "image/gif";
						break;
					case txt:
						contentType = "text/plain";
						break;
					case html:
						contentType = "text/html";
						break;
					default:
						contentType = "invalidtype";
						break;
					}

					int contentLength = fileBytes.length;
					// Date currentDate = new Date();
					// String date = currentDate.toString();
					String date = this.getCurrentDate();

					String responseHeader = "";
					if (isExpect) {
						responseHeader += requestVersion + "100 Continue\r\n";
					}
					responseHeader += requestVersion + " 200 OK\r\n" + "Date: "
							+ date + "\r\n" + "Content-Type: " + contentType
							+ "\r\n" + "Content-Length: " + contentLength
							+ "\r\n" + "Connection: close\r\n" + "\n";
					// // System.out.println(" [Output from log4j] Response Header : "
					// + responseHeader);
					// returning data to Socket
					DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream());
					dout.writeUTF(responseHeader);
					if (!isHead) {
						dout.write(fileBytes);
					}
					dout.flush();
					dout.close();
				} else {
					// call function to create Permission denied error
					int errorStatusCode = 403;
					try {
						this.createErrorResponse(errorStatusCode,requestVersion, clientSocket);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while creating file response " + e);
			System.err.println(" [Output from log4j] Error while creating file response "+ e);
			// e.printStackTrace();
		}
	}

	/*
	 * public boolean hashRequestDataForPOST(String first_line , Socket
	 * clientSocket){ mapRequestData = new HashMap<String,String> ();
	 * 
	 * boolean isCorrect = true; BufferedReader brObj = null; try{ { // checking
	 * for multiple spaces in a get request
	 * // System.out.println(" [Output for log4j] First Line : " + first_line);
	 * String[] subLine = first_line.split(" +"); // case for proper request GET
	 * path HTTP/*.* if (subLine.length == 3) { if
	 * (subLine[2].toLowerCase().equals("http/1.0") ||
	 * subLine[2].toLowerCase().equals("http/1.1")){
	 * this.mapRequestData.put("Method", subLine[0]); String requestPath =
	 * subLine[1]; if(subLine[2].toLowerCase().equals("http/1.1")) { if
	 * (requestPath.startsWith("http")) { URL urlObj = new URL (requestPath);
	 * this.mapRequestData.put("Path", urlObj.getPath());
	 * // System.out.println(urlObj.getPath().toString()); } else {
	 * this.mapRequestData.put("Path", subLine[1]); } } else {
	 * this.mapRequestData.put("Path", subLine[1]); }
	 * this.mapRequestData.put("Version", subLine[2]); isCorrect = true; } //
	 * case where GET path HTTP/*.* but not proper HTTP/*.* else { // call
	 * function to display error page for HTTP incorrect int errorStatusCode =
	 * 505; String requestVersion = subLine[2];
	 * this.createErrorResponse(errorStatusCode, requestVersion, clientSocket);
	 * isCorrect = false; } } // case where improper request of length 1 or 3
	 * else if (subLine.length == 1 || subLine.length > 3) { // call function to
	 * display error page for Bad Request int errorStatusCode = 400; String
	 * requestVersion = "HTTP/1.1"; this.createErrorResponse(errorStatusCode,
	 * requestVersion, clientSocket); isCorrect = false; } else if
	 * (subLine.length == 2){ // catch 2 special cases if
	 * (subLine[1].equalsIgnoreCase("/shutdown") ||
	 * subLine[1].equalsIgnoreCase("/control")){
	 * this.mapRequestData.put("Method", subLine[0]);
	 * this.mapRequestData.put("Path", subLine[1]);
	 * this.mapRequestData.put("Version", "HTTP/1.1"); isCorrect = true; } else{
	 * // case where request like GET HTTP/*.* if
	 * (subLine[1].toLowerCase().equals("http/1.0") ||
	 * subLine[1].toLowerCase().equals("http/1.1")){
	 * this.mapRequestData.put("Method", subLine[0]);
	 * this.mapRequestData.put("Path", "/"); this.mapRequestData.put("Version",
	 * subLine[1]); isCorrect = true; } else { int errorStatusCode = 0; String
	 * requestVersion = null; if (subLine[1].contains("HTTP/")){ errorStatusCode
	 * = 505; requestVersion = subLine[1];
	 * this.createErrorResponse(errorStatusCode, requestVersion, clientSocket);
	 * } else{ errorStatusCode = 400; requestVersion = "HTTP/1.1";
	 * this.createErrorResponse(errorStatusCode, requestVersion, clientSocket);
	 * } isCorrect = false; } } } } brObj = new BufferedReader (new
	 * InputStreamReader (clientSocket.getInputStream()));
	 * 
	 * String thisLine = brObj.readLine(); // all the lines are stored in a
	 * string
	 * // System.out.println(" [Output for log4j] First Line Buffer Reader : " +
	 * thisLine); while((thisLine != null && !thisLine.equals(""))){ //
	 * logger.info(" [Output from log4j Hash Map split for processed data " +
	 * retVal); //
	 * // System.out.println(" [Output from log4j Hash Map split for processed data "
	 * + retVal); // iCount++; { String[] subLine = thisLine.split(": "); //
	 * check process request data function if(subLine.length == 2){
	 * this.mapRequestData.put(subLine[0], subLine[1]); } else{ //logger.info(
	 * " [Output from log4j Error in parsing of RequestData. Length of split " +
	 * subLine.length); //System.err.println(
	 * " [Output from log4j Error in parsing of RequestData. Length of split " +
	 * subLine.length); } } thisLine = brObj.readLine(); } //logger.info(
	 * " [Output from log4j] Hash Map created for storing processed data " +
	 * mapRequestData); //// System.out.println(
	 * " [Output from log4j] Hash Map created for storing processed data " +
	 * mapRequestData);
	 * 
	 * // ### Dealing with the POST Method
	 * // System.out.println(" [Output from log4j] Out of while loop from line " +
	 * thisLine); StringBuffer postBody = new StringBuffer(); String str =
	 * this.mapRequestData.get("Content-Length");
	 * // System.out.println(" [Output from log4j] Out of while loop from line " +
	 * str); int length = Integer.parseInt(str.split(":")[1].trim()); int
	 * readChar = 0; while (true) { postBody.append((char)brObj.read());
	 * readChar ++; if(readChar >= length)break; }
	 * // System.out.println(" [Output from log4j]	" + postBody); } catch
	 * (Exception e){ logger.error(
	 * " [Output from log4j] Error while creating Map for storing processed data (POST Method)"
	 * + e); System.err.println(
	 * " [Output from log4j] Error while creating Map for storing processed data (POST Method) "
	 * + e); //e.printStackTrace(); isCorrect = false; } return isCorrect; }
	 */

	public boolean hashRequestData(String requestData, Socket clientSocket) {
		mapRequestData = new HashMap<String, String>();
		String[] requestLines = requestData.split("\t");

		// System.out.println(" [Output from log4j] Output from original hash map " + requestData);
		boolean isCorrect = true;
		int iCount = 0;
		try {
			for (String retVal : requestLines) {
				// logger.info(" [Output from log4j Hash Map split for processed data "
				// + retVal);
				// // System.out.println(" [Output from log4j Hash Map split for processed data "
				// + retVal);
				iCount++;
				if (iCount == 1) {
					// checking for multiple spaces in a get request
					String[] subLine = retVal.split(" +");

					// case for proper request GET path HTTP/*.*
					if (subLine.length == 3) {
						if (subLine[2].toLowerCase().equals("http/1.0")
								|| subLine[2].toLowerCase().equals("http/1.1")) {
							this.mapRequestData.put("Method", subLine[0]);
							String requestPath = subLine[1];
							if (subLine[2].toLowerCase().equals("http/1.1")) {
								if (requestPath.startsWith("http")) {
									URL urlObj = new URL(requestPath);
									this.mapRequestData.put("Path", urlObj.getPath());

									// for milestone 2 -- storing query string from the path
									String[] query_string_split = requestPath.split("\\?");
									if(query_string_split.length == 2) {
										this.query_string = query_string_split[1];
									}
									// for url pattern matching -- for Milestone 2 
									required_url = query_string_split[0];
									System.err.println(" [Output from log4j] Data from URL object for parsing " + this.required_url);
									// System.out.println(urlObj.getPath().toString());
								} else {
									this.mapRequestData.put("Path", subLine[1]);
									// for milestone 2 -- storing query string from the path
									String[] query_string_split = subLine[1].split("\\?");
									if(query_string_split.length == 2) {
										this.query_string = query_string_split[1];
									}
									// for url pattern matching -- for Milestone 2 
									required_url = query_string_split[0];
								}
							} else {
								this.mapRequestData.put("Path", subLine[1]);
								String[] query_string_split = subLine[1].split("\\?");
								if(query_string_split.length == 2) {
									this.query_string = query_string_split[1];
								}
								// for url pattern matching -- for Milestone 2 
								required_url = query_string_split[0];
							}
							this.mapRequestData.put("Version", subLine[2]);
							isCorrect = true;
						}
						// case where GET path HTTP/*.* but not proper HTTP/*.*
						else {
							// call function to display error page for HTTP
							// incorrect
							int errorStatusCode = 505;
							String requestVersion = subLine[2];
							this.createErrorResponse(errorStatusCode,
									requestVersion, clientSocket);
							isCorrect = false;
						}
					}
					// case where improper request of length 1 or 3
					else if (subLine.length == 1 || subLine.length > 3) {
						// call function to display error page for Bad Request
						int errorStatusCode = 400;
						String requestVersion = "HTTP/1.1";
						this.createErrorResponse(errorStatusCode,
								requestVersion, clientSocket);
						isCorrect = false;
					} else if (subLine.length == 2) {
						// catch 2 special cases
						if (subLine[1].equalsIgnoreCase("/shutdown")
								|| subLine[1].equalsIgnoreCase("/control")) {
							this.mapRequestData.put("Method", subLine[0]);
							this.mapRequestData.put("Path", subLine[1]);

							// for milestone 2 -- storing query string from the path
							String[] query_string_split = subLine[1].split("\\?");
							if(query_string_split.length == 2) {
								this.query_string = query_string_split[1];
							}
							// for url pattern matching -- for Milestone 2 
							required_url = query_string_split[0];

							this.mapRequestData.put("Version", "HTTP/1.1");
							isCorrect = true;
						} else {
							// case where request like GET HTTP/*.*
							if (subLine[1].toLowerCase().equals("http/1.0")
									|| subLine[1].toLowerCase().equals(
											"http/1.1")) {
								this.mapRequestData.put("Method", subLine[0]);
								this.mapRequestData.put("Path", "/");
								required_url = "/";
								this.mapRequestData.put("Version", subLine[1]);
								isCorrect = true;
							} else {
								int errorStatusCode = 0;
								String requestVersion = null;
								if (subLine[1].contains("HTTP/")) {
									errorStatusCode = 505;
									requestVersion = subLine[1];
									this.createErrorResponse(errorStatusCode,
											requestVersion, clientSocket);
								} else {
									errorStatusCode = 400;
									requestVersion = "HTTP/1.1";
									this.createErrorResponse(errorStatusCode,
											requestVersion, clientSocket);
								}
								isCorrect = false;
							}
						}
					}
				} else {
					String[] subLine = retVal.split(": ");
					System.out.println("[debug] subLine +" + subLine[0] + subLine[1]);
					// check process request data function
					if (subLine.length == 2) {
						this.mapRequestData.put(subLine[0], subLine[1]);
					} else {
						// logger.info(" [Output from log4j Error in parsing of RequestData. Length of split "
						// + subLine.length);
						// System.err.println(" [Output from log4j Error in parsing of RequestData. Length of split "
						// + subLine.length);
					}
				}
			}

			// ### Dealing with the POST Method
			//System.out.println("[DEBUG] Reached till POST loop");
			if (isPost) {
				System.out.println("[DEBUG] Inside POST loop");
				BufferedReader brObj = null;
				System.out.println("[DEBUG] Before Buffered Reader Obj");
				brObj = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				System.out.println("[DEBUG] After Buffered Reader Obj");
				StringBuffer postBody = new StringBuffer();
				/*
				 * String str = this.mapRequestData.get("Content-Length");
				 * //System
				 * .out.println(" [Output from log4j] Out of while loop from line "
				 * + str); int length =
				 * Integer.parseInt(str.split(":")[1].trim()); int readChar = 0;
				 * while (true) { postBody.append((char)brObj.read()); readChar
				 * ++; if(readChar >= length)break; }
				 * // System.out.println(" [Output from log4j]	" + postBody);
				 */
				String line = null;
				System.out.println("[DEBUG] Before While + " + brObj == null);
				/**
				System.out.println("[DEBUG] Before While +" + brObj.readLine());
				while ((line = brObj.readLine()).length() != 0) {
					System.out.println("[DEBUG] Line + " + line);
					postBody.append(line);
				}
				**/
				String str = this.mapRequestData.get("Content-Length");
				int length = Integer.parseInt(str);
				System.out.println("[DEBUG] Content-Length + " + length);
				int readChar = 0;
				while (true) {
					System.out.println("[DEBUG] Before Read + " + postBody);
					postBody.append(brObj.read());
					System.out.println("[DEBUG] After Read + " + postBody);
					readChar ++;
					if(readChar >= length)break;
				}
				System.out.println("[DEBUG] After While + " + postBody);
				// // System.out.println(" [Output from log4j]	" + postBody);
			}

			// logger.info(" [Output from log4j] Hash Map created for storing processed data "
			// + mapRequestData);
			// // System.out.println(" [Output from log4j] Hash Map created for storing processed data "
			// + mapRequestData);
		} catch (Exception e) {
			logger.error(" [Output from log4j] Error while creating Map for storing processed data " + e);
			System.err
			.println(" [Output from log4j] Error while creating Map for storing processed data "+ e);
			// e.printStackTrace();
			isCorrect = false;
		}
		return isCorrect;
	}

	public boolean parse_requested_uri (String required_uri) {
		// System.out.println(" [Output from log4j] Printing pattern for required_uri  " + required_uri);
		HashMap <String, String> url_map = new HashMap<String,String>();
		url_map = ERISServletHarness.Handler.m_urlPattern;
		if (! required_uri.startsWith("/")) 
			required_uri = "/" + required_uri;
		for (String url_pattern : (url_map.keySet())) {
			if (! url_pattern.startsWith("/"))
				url_pattern = "/" + url_pattern;

			if(required_uri.equals(url_pattern)){
				// System.out.println(" [Output from log4j] Entering loop to set isServlet = True ......1");
				this.servlet_path = required_uri;
				this.path_info = "";
				String servlet_name = url_map.get(url_pattern);
				// System.out.println(" [Output from log4j] Value of Servlet ......1      " + servlet_name);
				servlet = DaemonThread.servletPool.get(servlet_name);
				// System.out.println(" [Output from log4j] Value of Servlet ......1      " + servlet);
				this.isServlet = true;
				break;
			}
			else if (url_pattern.contains("*")) {

				String url_start_with = url_pattern;
				String url_end_with = url_pattern;

				// case 1 -- starts with "*"
				if(url_start_with.startsWith("/*")) {
					//String servlet_path_with_slash = url_start_with.split("/*")[0];
					//// System.out.println(" [Output from log4j] Value of servlet_with_slash " + servlet_path_with_slash + "	" + servlet_path_with_slash.length());
					//String prefix_servlet_path = servlet_path_with_slash.substring(0, servlet_path_with_slash.length() - 1);
					//// System.out.println(" [Output from log4j] Value of servlet_with_slash " + prefix_servlet_path + "	" + prefix_servlet_path.length());
					//this.servlet_path = prefix_servlet_path;
					//this.path_info = required_url.substring(this.servlet_path.length(), required_url.length());
					url_start_with = url_start_with.replace("/*", "");
					if (required_url.endsWith(url_start_with)) {
						this.servlet_path = "";
						this.path_info = this.required_url;
						// System.out.println(" [Output from log4j] Servlet Path :" + this.servlet_path + "	Path_info :" + this.path_info);
						//this.url_pattern_xml = url_pattern;
						// System.out.println(" [Output from log4j] Entering loop to set isServlet = True ......2");
						String servlet_name = url_map.get(url_pattern);
						// System.out.println(" [Output from log4j] Value of Servlet Name ......2 " + servlet_name);
						servlet = DaemonThread.servletPool.get(servlet_name);
						// System.out.println(" [Output from log4j] Value of Servlet ......2 " + servlet);
						this.isServlet = true;
						break;
					}
				}

				if(url_end_with.endsWith("*")) {
					//// System.out.println(" [Output from log4j] Entering loop to set isServlet = True ......3");
					//String servlet_path_with_slash = url_end_with.split("/*")[0];
					//// System.out.println(" [Output from log4j] Value of servlet_with_slash " + servlet_path_with_slash + "	" + servlet_path_with_slash.length());
					//String prefix_servlet_path = servlet_path_with_slash.substring(0, servlet_path_with_slash.length() - 1);
					//// System.out.println(" [Output from log4j] Value of servlet_with_slash " + servlet_path_with_slash + "	" + servlet_path_with_slash.length());
					//this.servlet_path = prefix_servlet_path;
					//this.path_info = required_url.substring(this.servlet_path.length(), required_url.length());
					url_end_with = url_end_with.substring(0, url_end_with.length()-1);
					if(required_url.startsWith(url_end_with)) {
						this.servlet_path = url_end_with.substring(0,(url_end_with.length()-1));
						if(required_url.length() >= this.servlet_path.length())
							this.path_info = required_url.substring(this.servlet_path.length(),this.required_url.length());
						else 
							this.path_info = "";
						//this.url_pattern_xml = url_pattern;
						String servlet_name = url_map.get(url_pattern);
						servlet = DaemonThread.servletPool.get(servlet_name);
						// System.out.println(" [Output from log4j] Value of Servlet ......3 " + servlet);
						this.isServlet = true;
						break;
					}
				}
			}
		}
		return this.isServlet;
	}
}
