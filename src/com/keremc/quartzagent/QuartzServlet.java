package com.keremc.quartzagent;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class QuartzServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        QuartzResponseManager manager = QuartzAgent.getResponseManager();
        Option option = null;

        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {
            String param = e.nextElement();
            String val = request.getParameter(param);

            try {
                option = Option.valueOf(val);
                break;
            } catch (Exception ex) {
            }
        }

        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);

        if (option == null) {
            PrintWriter out = response.getWriter();
            out.print("3|Unknown option - ");

            boolean first = true;

            for (Job j : manager.getJobs()) {

                out.print((!first ? ", " : "") + j.toString());
                first = false;
            }
            out.flush();
            return;
        }


        PrintWriter out = response.getWriter();
        out.print(manager.process(option));
        out.flush();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
