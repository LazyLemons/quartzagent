package com.keremc.quartzagent;

import java.net.InetSocketAddress;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class QuartzAgent {
    private static QuartzResponseManager qrm = new QuartzResponseManager();
    public static String activationServletURI;

    public static void main(String[] args) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        activationServletURI = args[2];

        InetSocketAddress address = new InetSocketAddress(host, port);
        Server server = new Server(address);

        new Thread() {
            public void run() {
                qrm.query();

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }.start();

        for (Connector connector : server.getConnectors()) {
            connector.setMaxIdleTime(25000);
        }

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/quartzagent");
        server.setHandler(handler);

        handler.addServlet(new ServletHolder(new QuartzServlet()), "/*");

        server.start();
        server.join();

    }

    public static QuartzResponseManager getResponseManager() {
        return qrm;
    }
}
