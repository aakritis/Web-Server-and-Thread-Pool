package edu.upenn.cis.cis455.webserver;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SuppressWarnings("unused")
public class ERISServletContext implements ServletContext {
	
	private String context_name;
	private HashMap<String,Object> attributes;
	private HashMap<String,String> initParams;
	
	public ERISServletContext() {
		this.context_name = "ERIS Harness";
		attributes = new HashMap<String,Object>();
		initParams = new HashMap<String,String>();
	}
	
	public ERISServletContext(String context_name) {
		this.context_name = context_name;
		attributes = new HashMap<String,Object>();
		initParams = new HashMap<String,String>();
	} 
	
	public Object getAttribute(String attr_key) {
		return attributes.get(attr_key);
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getAttributeNames() {
		Set<String> keys = attributes.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}
	
	public ServletContext getContext(String name) {
		return this;
	}
	
	public String getInitParameter(String name) {
		return initParams.get(name);
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}
	
	public int getMajorVersion() {
		return 2;
	}
	
	public String getMimeType(String file) {
		return null;
	}
	
	public int getMinorVersion() {
		return 4;
	}
	
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}
	
	public String getRealPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		String real_path = "";
		String hostname = "localhost";
		int port_number = HttpServer.serverPort;
		real_path += "http://" + hostname + ":" + port_number + "/"; 
		String webdotxml_path = HttpServer.webdotxml_path;
		String[] xml_path_split = webdotxml_path.split("/");
		for (int index = 0 ; index < (xml_path_split.length - 2) ; index++ ){
			real_path += xml_path_split[index] + "/"; 
		}
		real_path += path;
		return real_path;
	}
	
	public RequestDispatcher getRequestDispatcher(String name) {
		return null;
	}
	
	public java.net.URL getResource(String path) throws MalformedURLException {
		return null;
	}
	
	public java.io.InputStream getResourceAsStream(String path) {
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public java.util.Set getResourcePaths(String path) {
		return null;
	}
	
	public String getServerInfo() {
		return this.context_name;
	}
	
	public Servlet getServlet(String name) throws ServletException {
		return null;
	}
	
	public String getServletContextName() {
		return this.context_name;
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getServletNames() {
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getServlets() {
		return null;
	}
	
	public void log(Exception exception, String msg) {
		log(msg, (Throwable) exception);
	}
	
	public void log(String msg) {
		System.err.println(msg);
	}
	
	public void log(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace(System.err);
	}
	
	public void removeAttribute(String name) {
		attributes.remove(name);
	}
	
	public void setAttribute(String name, Object object) {
		attributes.put(name, object);
	}
	
	void setInitParam(String name, String value) {
		initParams.put(name, value);
	}
}