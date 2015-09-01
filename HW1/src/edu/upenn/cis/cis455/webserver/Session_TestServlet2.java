package edu.upenn.cis.cis455.webserver;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

@SuppressWarnings("serial")
public class Session_TestServlet2 extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try {
			// System.out.println(" [Output from log4j] In Session Servlet , Entering Servlet ");
			HttpSession session = request.getSession();
			String val = (String) session.getAttribute("TestAttribute");
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			// System.out.println(" [Output from log4j] In Session Servlet , After PrintWriter Function");
			out.println("<HTML><HEAD><TITLE>Session Servlet 1</TITLE></HEAD><BODY>");
			out.println("<P>TestAttribute value is '" + val + "'.</P>");
			out.println("<P>Session invalidated.</P>");
			out.println("<P>Continue to <A HREF=\"Session_TestServlet3\">Session Servlet 3</A>.</P>");
			out.println("</BODY></HTML>");
			
			//session.invalidate();
		}
		catch (Exception e) {
			System.err.println(" [Output from log4j] In Session Servlet , Error Occured " + e);
		}
	}
}