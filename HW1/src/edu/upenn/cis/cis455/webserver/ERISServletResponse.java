package edu.upenn.cis.cis455.webserver;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class ERISServletResponse implements HttpServletResponse {

	Vector<Cookie> response_cookie = null;
	HashMap<String, StringBuffer> response_header = null;
	StringWriter buffer_writer = null;
	DataOutputStream dout;
	PrintWriter printWriterObj = null;
	int buffer_size = 0;
	Locale locale;	
	String content_type = null;
	int content_length = 0;
	String char_encode = null;

	String http_version;
	boolean isCommit = false;
	long time;

	String status_msg ;
	private ByteArrayOutputStream out;
	int status_code ;

	ERISServletRequest requestObj;
	private ERISSession session;

	public ERISServletResponse(ERISServletRequest requestObj, DataOutputStream dout) {
		try{
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor Before request Obj");
			this.requestObj = requestObj;
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After request Obj");
			response_cookie = new Vector<Cookie>();
			response_header = new HashMap<String, StringBuffer>();
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor Before Locale");
			locale = new Locale("en");
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After Locale");
			buffer_size = 0;
			this.status_code = 200;
			this.status_msg = "OK";
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor Before dout");
			this.dout = dout;
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After dout");
			this.http_version = this.requestObj.getProtocol();
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After protocol");
			this.session = (ERISSession) this.requestObj.getSession();
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After session");
			this.out = new ByteArrayOutputStream();
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After out");
			this.printWriterObj = new PrintWriter(out);
			// // System.out.println(" [Output from log4j] In ERISServletResponse Constructor After printwriter");
			addDateHeader("Date", (new Date()).getTime());
			//addHeader("Server", "ERIS Server");
		}
		catch (Exception e) {
			System.err.println(" [Output from log4j] In ERISServletResponse Constructor " + e);
		}
	}
	public void addCookie(Cookie arg0) {		
		// TODO Auto-generated method stub
		// System.out.println("[DEBUG] Adding in Cookie +" + arg0);
		this.response_cookie.add(arg0);
	}

	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return this.response_header.containsKey(arg0);
	}

	// Yet to implement
	public String encodeURL(String arg0) {
		return arg0;
	}

	public String encodeRedirectURL(String arg0) {
		return encodeURL(arg0);
	}

	public String encodeUrl(String arg0) {
		return arg0; // Deprecated
	}

	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return arg0; // Deprecated
	}

	public void sendError(int arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		if(this.isCommitted())
			throw new IllegalStateException();
		this.setStatus(arg0);
		this.status_msg = arg1;
	}

	public void sendError(int arg0) throws IOException {
		// TODO Auto-generated method stub
		if (isCommitted())
			throw new IllegalStateException();
		this.setStatus(arg0);
	}

	public void sendRedirect(String arg0) throws IOException {
		// System.out.println("[DEBUG] redirect to " + arg0 + " requested");
		// System.out.println("[DEBUG] stack trace: ");
		Exception e = new Exception();
		StackTraceElement[] frames = e.getStackTrace();
		for (int i = 0; i < frames.length; i++) {
			System.out.print("[DEBUG]   ");
			// System.out.println(frames[i].toString());
		}
	}

	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		SimpleDateFormat parser = new SimpleDateFormat("EEE, dd MM HH:mm:ss zzz yyyy");
		if(containsHeader(arg0))
			this.response_header.remove(arg0);
		this.response_header.put(arg0, new StringBuffer(parser.format(arg1)));
	}

	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		SimpleDateFormat date_parser = new SimpleDateFormat("EEE, dd MM HH:mm:ss zzz yyyy");
		StringBuffer hash_value = new StringBuffer();
		if (containsHeader(arg0)) {
			hash_value = this.response_header.get(arg0);
			hash_value.append(", ");
		}
		this.response_header.put(arg0, hash_value.append(date_parser.format(arg1)));
	}

	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		if(containsHeader(arg0))
			this.response_header.remove(arg0);
		this.response_header.put(arg0, new StringBuffer(arg1));

	}

	public void addHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		StringBuffer hash_value = new StringBuffer();
		if(this.response_header.containsKey(arg0)) {
			hash_value = this.response_header.get(arg0);
			hash_value.append(", ");
		}
		this.response_header.put(arg0, hash_value.append(arg1));

	}

	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		StringBuffer hash_value = new StringBuffer();
		this.response_header.put(arg0,hash_value.append(arg1));
	}

	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		StringBuffer hash_value = new StringBuffer();
		if(this.response_header.containsKey(arg0)) {
			hash_value = this.response_header.get(arg0);
			hash_value.append(", ");
		}
		this.response_header.put(arg0, hash_value.append(arg1));
	}

	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		this.status_code = arg0;
		switch (this.status_code) {
		case 200:
			this.status_msg = "OK";
			break;
		case 100:
			this.status_msg = "Continue";
			break;
		case 404:
			this.status_msg = "Not Found";
			break;
		case 403:
			this.status_msg = "Forbidden";
			break;
		case 501:
			this.status_msg = "Not Implemented";
			break;
		case 505:
			this.status_msg = "HTTP Version Not Supported";
			break;
		case 400:
			this.status_msg = "Bad Request";
			break;
		case 412:
			this.status_msg = "Precondition Failed";
			break;
		case 304:
			this.status_msg = "Not Modified";
			break;
		default:
			this.status_msg = "";
			break;
		}
	}

	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		// not to be implemented
	}

	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return "ISO-8859-1";
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		if(this.content_type != null) 
			return this.content_type;
		return "text/html";
	}

	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public PrintWriter getWriter() throws IOException {
		//this.buffer_writer = new StringWriter(this.buffer_size);
		//this.printWriterObj = new PrintWriter(this.buffer_writer,false);
		//// System.out.println(" [Output from log4j] Inside Response " + this.buffer_writer);
		//return new PrintWriter(System.out, true);
		return printWriterObj;
	}

	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		if(!isCommitted()) {
			this.char_encode = arg0;
			this.setHeader("Character-Encoding", this.char_encode);
		}
	}

	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		if(!this.isCommitted()) {
			this.content_length = arg0;
			this.setIntHeader("Content-Length", this.content_length);
		}
	}

	public void setContentType(String arg0) {
		// TODO Auto-generated method stub
		if(!this.isCommitted()) {
			this.content_type = arg0;
			this.setHeader("Content-Type", this.content_type);
		}
	}

	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		if(!isCommitted()) {
			this.buffer_size = arg0;
		}
		throw new IllegalStateException();
	}

	public int getBufferSize() {
		// TODO Auto-generated method stub
		return this.buffer_size;
	}

	// Yet to implement
	public void flushBuffer() throws IOException
	{
		try {
			// TODO Auto-generated method stub
			String header="";
			if(isCommitted())
				return;
			header+= http_version + " " + this.status_code + " " + this.status_msg + " \r\n";
			//dout.write(header.getBytes());
			//cookie handler
			if(this.session != null) {
				// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, if Session!= null " + session);
				// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, if Session!= null " + session);
				try {
					// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer,Get session id" + session.getId());
				}
				catch(Exception e) {
					System.err.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer,Error in Get session id" + e);
				}
				Cookie newCookie = new Cookie("JSESSIONID", session.getId());
				// System.out.println("[Debug] Adding Session to cookie +" + session.getId());
				// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, if Session!= null " + session.getId());
				int maxInactive = session.getMaxInactiveInterval();
				// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, After Max Interval" );
				newCookie.setMaxAge(maxInactive);
				addCookie(newCookie);
			}
			// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, before cookie loop ");
			for(Cookie c : response_cookie) 
			{
				// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, inside cookie loop " + c);
				long maxAge = 1000 * c.getMaxAge();
				time = c.getName().equals("Session") ? maxAge + session.getCreationTime() : maxAge + (new Date()).getTime();

				SimpleDateFormat cookieDate = new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss z");
				header+="Set-Cookie: " + c.getName() + "=" + c.getValue() + "; expires=" + cookieDate.format(new Date(time))+"\r\n";
				//dout.write(("Set-Cookie: " + c.getName() + "=" + c.getValue() + "; expires=" + cookieDate.format(new Date(time))).getBytes());
			}

			for(String _key : this.response_header.keySet())
			{
				// System.out.println("[DEBUG] key in response_header + " + _key);
				try
				{
					/*
					if(_key.equals("Date"))
					{
						long value=Long.valueOf(response_header.get(_key).toString());
						Date date=new Date(value);
						SimpleDateFormat sdf = new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss z");
						header+=_key+": "+sdf.format(date)+"\r\n";
					}
					*/	
				
					{	
						header+=_key + ": " + this.response_header.get(_key)+"\r\n";
					}
				}
				catch(Exception e)
				{
					// System.out.println("Exception occurred....."+e);
				}
			}	
			//dout.write((_key + ": " + this.response_header.get(_key)).getBytes());

			printWriterObj.flush();//flush the print writer object
			// System.out.println(" [Output from log4j] In ERIS Servlet Response, In flush buffer, after printwriter flush");
			if(! this.response_header.containsKey("Content-Length")){
				header+="Content-Length: " + out.toString().length()+"\r\n"+"\n";
				//dout.write(("Content-Length: " + out.toString().length()).getBytes());
			}

			// System.out.println("Header.............."+header);
			dout.write(header.getBytes());
			dout.write((out.toString()).getBytes());
			dout.close();
			dout.flush();
			isCommit = true;
		}
		catch (Exception e){
			System.err.println(" [Output from log4j] In ERISServletResponse, Error in flushBuffer function() " + e);
		}
	}

	public void resetBuffer() {
		// TODO Auto-generated method stub
		if(!this.isCommitted()) {
			this.buffer_writer.getBuffer().setLength(0);
			out = new ByteArrayOutputStream();
			return;
		}
		throw new IllegalStateException();
	}

	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return isCommit;
	}

	public void reset() {
		// TODO Auto-generated method stub
		if(!this.isCommitted()) {
			try{
				this.response_header = new HashMap<String,StringBuffer>();
				this.buffer_writer.getBuffer().setLength(0);
				out = new ByteArrayOutputStream();
				setStatus(200);
				isCommit = false;
				return;
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		throw new IllegalStateException();
	}

	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		if(!isCommitted()) {
			this.locale = arg0;
			this.setHeader("Locale", this.locale.toString());
		}
	}

	public Locale getLocale() {
		// TODO Auto-generated method stub
		if(locale != null)
			return locale;
		return null;
	}
}