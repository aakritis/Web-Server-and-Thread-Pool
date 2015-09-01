package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Principal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

class ERISServletRequest implements HttpServletRequest {

	private Properties m_params = new Properties();
	private Properties m_props = new Properties();
	private ERISSession m_session;
	private String m_method;
	private Vector<Cookie> m_cookies = new Vector<Cookie>();
	HashMap<String, String> reqHead = new HashMap<String, String>();

	private ERISServletContext m_context;

	String path_info;
	String query_string;
	String servlet_path;
	Socket clientSocket;
	private Locale locale;
	BufferedReader brObj_body ;
	String encode = null;
	Boolean fromUri;

	ERISServletRequest() {
	}

	ERISServletRequest(ERISSession session) {
		m_session = session;
	}

	ERISServletRequest(Socket clientSocket, HashMap<String, String> reqHead, ERISServletContext contextObj, ERISSession sessionObj) {
		this.clientSocket = clientSocket;
		m_session = sessionObj;
		m_context = contextObj;
		this.reqHead = reqHead;
		System.out.println("[Debug]In Request");
		for (String key : this.reqHead.keySet()) {
			if (key.equalsIgnoreCase("cookie")) {
				String[] cookies = this.reqHead.get(key).split(";");
				for (String ck : cookies) {
					String key_cookie = ck.split("=")[0].trim();
					String value_cookie = ck.split("=")[1].trim();
					if (key_cookie.equalsIgnoreCase("JSESSIONID")) {
						// System.out.println("[Debug]In Header Cookie");
						/*for(ERISSession sess : DaemonThread.sessionList){
							if(sess.isValid())
								if(sess.getId().equals(value_cookie))
									m_session = sess;
						}*/
						m_context = (ERISServletContext) getAttribute("Servlet-Context");
						ERISSession sess = (ERISSession)m_context.getAttribute("Session");
						//System.out.println("[Debug] Session +" + sess.getAttribute("id"));
						if(sess.getAttribute("id").equals(value_cookie))
							
							m_session = sess;
					}
					Cookie cookie = new Cookie(key_cookie, value_cookie);
					m_cookies.add(cookie);
				}
			}
		}
	}

	public String getAuthType() {
		// TODO Auto-generated method stub
		return BASIC_AUTH;
	}

	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		int size = m_cookies.size();
		if (size == 0)
			return null;
		return m_cookies.toArray(new Cookie[size]);
	}

	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey(arg0))
			return -1;
		String date_pattern = "EEE, dd MMM yyyy HH:mm:ss zzz";
		SimpleDateFormat parserObj = new SimpleDateFormat(date_pattern);
		try {
			String date_val = getHeader(arg0);
			Date date = (Date) parserObj.parse(date_val);
			return date.getTime();
		} catch (Exception e) {
			throw new IllegalArgumentException();
		}
	}

	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey(arg0))
			return null;
		return this.reqHead.get(arg0);
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getHeaders(String arg0) {
		// TODO Auto-generated method stub
		if (this.reqHead.isEmpty() || this.reqHead == null)
			return null;
		if (this.reqHead.containsKey(arg0)) {
			String[] key_values = this.reqHead.get(arg0).split(", ");
			Vector<String> values = new Vector<String>();
			for (int index = 0; index < key_values.length; index++) {
				values.addElement(key_values[index]);
			}
			return values.elements();
		} else {
			Enumeration<String> empty_enum = Collections.emptyEnumeration();
			return empty_enum;
		}
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getHeaderNames() {
		// TODO Auto-generated method stub
		if (this.reqHead.isEmpty()) {
			Enumeration<String> empty_enum = Collections.emptyEnumeration();
			return empty_enum;
		}
		if (this.reqHead == null)
			return null;
		return Collections.enumeration(this.reqHead.keySet());
	}

	public int getIntHeader(String arg0) {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey(arg0)) {
			return -1;
		}
		try {
			int parsed_int = Integer.parseInt(this.reqHead.get(arg0));
			return parsed_int;
		} catch (NumberFormatException e) {
			throw new NumberFormatException();
		}
	}

	public String getMethod() {
		return m_method;
	}

	public void setPathInfo(String path) {
		path_info = path;
	}

	public String getPathInfo() {
		// TODO Auto-generated method stub
		if (path_info == "")
			return null;
		return path_info;
	}

	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null; // not required to implement
	}

	public String getContextPath() {
		// TODO Auto-generated method stub
		return ""; // as per the requirements only 1 application per servlet
		// container
	}

	public void setQueryString(String query_string) {
		// TODO Auto-generated method stub
		this.query_string = query_string;
	}

	public String getQueryString() {
		// TODO Auto-generated method stub
		return this.query_string;
	}

	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null; // not implemented for HW2
	}

	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return m_session.getId();
	}

	public String getRequestURI() {
		// TODO Auto-generated method stub
		return this.servlet_path + this.path_info;
	}

	// Yet to implement
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServletPath() {
		// TODO Auto-generated method stub
		return this.servlet_path;
	}
	public void setServletPath(String servlet_path) {
		this.servlet_path = servlet_path;
	}

	/*
	// Yet to implement
	@SuppressWarnings("unused")
	public HttpSession getSession(boolean arg0) {
		// System.out.println("################in #######");
		// NOW counts for ONE ACCESS to sessions
		if (arg0) {
			if (! hasSession()) {
				// System.out.println("@@@@@@No session");
				m_session = new ERISSession(m_context);
				DaemonThread.sessionList.add(m_session);
				this.reqHead.put("cookie", "JSESSIONID=" + m_session.getId());
			}
		} 
		else {
			if (! hasSession()) {
				m_session = null;
				DaemonThread.sessionList.remove(m_session);
			}
			else{
				// m_session.invalidate();
				// m_session = null;
				// update the last access time of session
				long today = System.currentTimeMillis();
				long due = m_session.getLastAccessedTime() + m_session.getMaxInactiveInterval()*1000;
				if(true ){
					// If session too old, get a NEW one
					m_session.invalidate();
					// m_session = new FakeSession((FakeContext)m_context);
					// Dispatcher.addSession(m_session);
					// reqHead.put("cookie", "JSESSIONID=" + m_session.getId());
					return null;
					// If session too old, get a NEW one
				} 
				else {
					m_session.setLastAccessedTime(today);
				}
			}
		}
		return m_session;
	}
	 */

	public HttpSession getSession(boolean arg0) {
		try {
			// // System.out.println(" [Output from log4j] In ERISServletRequest, Before getCokie Function");
			getCookies();
			// // System.out.println(" [Output from log4j] In ERISServletRequest, After getCokie Function");
			if (arg0) {
				//Check if session already exists
				if (hasSession()) {
					fromUri = false;//Used in checking source from cookie or uri
					// // System.out.println(" [Output from log4j] In ERISServletRequest, Before putValue Function");
					m_session.putValue("isNew", false);
					// // System.out.println(" [Output from log4j] In ERISServletRequest, After putValue Function");
					return m_session;
				}
				else {
					if (!hasSession()) {
						fromUri = true;
						// System.out.println(" [Output from log4j] In ERISServletRequest, Before Session Obj");
						m_session = new ERISSession(this.m_context);
						// System.out.println(" [Output from log4j] In ERISServletRequest, After Session Obj");
						m_session.putValue("isNew", true);
						// System.out.println(" [Output from log4j] In ERISServletRequest, After put value");
						m_session.setAttribute("id",UUID.randomUUID().toString());
						// System.out.println(" [Output from log4j] In ERISServletRequest, After set attribute value");
					}
				}
				// System.out.println(" [Output from log4j] In ERISServletRequest, Before req head put function");
				this.reqHead.put("JSESSIONID", m_session.getId());
				// System.out.println(" [Output from log4j] In ERISServletRequest, After req head put function");
				return m_session;
			}
			else {
				if(!hasSession())
					return null;
			}
			return m_session;
		}
		catch(Exception ex) {
			System.err.println(" [Output from log4j] In ERISServletRequest, Exception in getSession() " + ex);
			return null;
		}
	}

	public HttpSession getSession() {
		// System.out.println(" [Output from log4j] In ERISServletRequest, Enter Get Session Function");
		return getSession(true);
	}

	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return m_session.isValid();
	}

	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		if (this.reqHead.containsKey("Cookie"))
			if (this.reqHead.get("Cookie").startsWith("Session"))
				return true;
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		if (this.reqHead.containsKey("session"))
			return true;
		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false; // not to be implemented
	}

	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return m_props.get(arg0);
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return m_props.keys();
	}

	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		if (this.reqHead.containsKey("Character-Encoding"))
			return this.reqHead.get("Character-Encoding");
		return "ISO-8859-1";
	}

	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		this.encode = arg0;
	}

	public int getContentLength() {
		// TODO Auto-generated method stub
		if (this.reqHead.containsKey("Content-Length"))
			return Integer.parseInt(reqHead.get("Content-Length"));
		return -1;
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		if (this.reqHead.containsKey("Content-Type"))
			return reqHead.get("Content-Type");
		return null;
	}

	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getParameter(String arg0) {
		return m_params.getProperty(arg0);
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getParameterNames() {
		return m_params.keys();
	}

	public String[] getParameterValues(String arg0) {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey(arg0))
			return null;
		String param_value = this.reqHead.get(arg0);
		String[] vals = param_value.split(",");
		return vals;
	}

	@SuppressWarnings("rawtypes")
	public Map getParameterMap() {
		// TODO Auto-generated method stub
		if (m_params == null || m_params.isEmpty())
			return null;
		return m_params;
	}

	public String getProtocol() {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey("Version"))
			return null;
		return this.reqHead.get("Version");
	}

	public String getScheme() {
		// TODO Auto-generated method stub
		return "HTTP";
	}

	public String getServerName() {
		// TODO Auto-generated method stub
		if (!this.reqHead.containsKey("Host"))
			return null;
		String host_value = (reqHead.get("Host"));
		String host_name = host_value.substring(0, host_value.indexOf(":"));
		return host_name;
	}

	public int getServerPort() {
		// TODO Auto-generated method stub
		return HttpServer.serverPort;
	}

	public void setReader(BufferedReader brObj) throws IOException {
		// TODO Auto-generated method stub
		this.brObj_body = brObj;
	}

	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return this.brObj_body;
	}

	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		if (clientSocket.isConnected())
			return clientSocket.getRemoteSocketAddress().toString();
		return null;
	}

	public String getRemoteHost() {
		// TODO Auto-generated method stub
		if (clientSocket.isConnected())
			return this.reqHead.get("User-Agent");
		return null;
	}

	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		m_props.remove(arg0);
	}

	public Locale getLocale() {
		// TODO Auto-generated method stub
		if (locale != null)
			return locale;
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null; // not implemented
	}

	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getRemotePort() {
		// TODO Auto-generated method stub
		if (clientSocket != null)
			return clientSocket.getPort();
		return -1;
	}

	public String getLocalName() {
		// TODO Auto-generated method stub
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return null;
		}
	}

	public String getLocalAddr() {
		// TODO Auto-generated method stub
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return null;
		}
	}

	public int getLocalPort() {
		// TODO Auto-generated method stub
		return HttpServer.serverPort;
	}

	void setMethod(String method) {
		m_method = method;
	}

	void setParameter(String key, String value) {
		m_params.setProperty(key, value);
	}

	void clearParameters() {
		m_params.clear();
	}

	boolean hasSession() {
		return ((m_session != null) && m_session.isValid());
	}
}
