package edu.upenn.cis.cis455.webserver;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;


class ERISServletConfig implements ServletConfig {
	private String name;
	private ERISServletContext context;
	private HashMap<String,String> initParams;
	static final Logger logger = Logger.getLogger(WorkerThread.class);
	
	public ERISServletConfig(String name, ERISServletContext context) {
		this.name = name;
		this.context = context;
		initParams = new HashMap<String,String>();
	}

	void setInitParam(String name, String value) {
		initParams.put(name, value);
	}
	
	public String getInitParameter(String param_key) {
		return initParams.get(param_key);
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}
	
	public ServletContext getServletContext() {
		return context;
	}
	
	public String getServletName() {
		return name;
		}
}
