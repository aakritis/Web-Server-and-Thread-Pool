package edu.upenn.cis.cis455.webserver;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.junit.Test;

import junit.framework.Assert;


@SuppressWarnings("unused")
public class ERISServerJUnitTest extends TestCase {
	String query;
	@Test
	public void testRequest() throws IOException{
		String whatFile ="workspace/HW1/www/calculate?num1=1&num2=2";
		String [] divideReq = null;
		// Servlet type request

		divideReq = whatFile.split("\\?");
		if(divideReq.length == 2) {
			whatFile = divideReq[0];
			query = divideReq[1];
		}					
		WorkerThread worker = new WorkerThread(null,null);
		HttpServlet serv = new CalculatorServlet();
		HashMap<String, String> mapRequestData = new HashMap<>();
		//Host: www.host1.com:80
		mapRequestData.put("Host", "localhost:8080");
		//// System.out.println("Servlet: " + serv);
		if(serv != null) {
			ERISSession session = null;
			//ERISServletContext context = null;
			ERISServletRequest req = new ERISServletRequest();

			//Associate user request to session
			for(String key: mapRequestData.keySet()) {
				//Get cookie from hashmap that stored request
				if(key.equalsIgnoreCase("Cookie")) {
					String headerValue = req.getHeader(key);
					String[] cookieParts = headerValue.split("; ");
					for(String cp : cookieParts) {
						String name = cp.split("=")[0].trim();
						String value = cp.split("=")[1].trim();
						// If it's a session in cookoe
						if(name.equalsIgnoreCase("JSESSIONID")) {
							//m_session = Dispatcher.getSession(value);
							ERISServletContext con = (ERISServletContext) req.getAttribute("Servlet-Context");
							ERISSession sess = (ERISSession) con.getAttribute("Session");
							if(sess.getAttribute("id").equals(value))
								session = sess;
						}
					}
				}
			}
			req.setMethod("GET");
			req.setQueryString(query);
			req.setServletPath(whatFile);
			req.setPathInfo("");
			//Set params for query string
			if(query != null && !query.isEmpty())
			{
				String[] qArray = query.split("\\&");
				for(String q : qArray){
					String part1 = q.split("=")[0];
					String part2 = q.split("=")[1];
					req.setParameter(part1, part2);
				}
			}
			//Response 
			HttpServlet servlet = serv;
			ERISServletResponse response = new ERISServletResponse(req, null);
			response.setHeader("Server", "Test Server");
			try {
				Assert.assertNotNull(req);
				Assert.assertNotNull(response);
				servlet.service(req, response);//Service method invocation
				// System.out.println("Service ended");

			} catch (ServletException e) {
				System.err.println("Error Occured " + e);	
			}
		}
	}
}