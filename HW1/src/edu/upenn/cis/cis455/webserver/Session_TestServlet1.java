package edu.upenn.cis.cis455.webserver;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

@SuppressWarnings("serial")
public class Session_TestServlet1 extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		session.setAttribute("TestAttribute", "12345");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<HTML><HEAD><TITLE>Session Servlet 1</TITLE></HEAD><BODY>");
		out.println("<P>TestAttribute set to 12345.</P>");
		out.println("<P>Continue to <A HREF=\"Session_TestServlet2\">Session Servlet 2</A>.</P>");
		String val = (String) session.getAttribute("TestAttribute");
		out.println("<P>TestAttribute value is '" + val + "'.</P>");
		out.println("<P>Continue to <A HREF=\"Session_TestServlet3\">Session Servlet 3</A>.</P>");
		System.out.println("Before invalidate..");
		//session.invalidate();
	}
}