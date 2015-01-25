package com.github.ruediste.laf.core.entry;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	private Server server;

	public String startForTesting(FrontServlet frontServlet) {
		try {
			ServletHolder holder = new ServletHolder(frontServlet);

			ServletContextHandler ctx = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			ctx.setContextPath("/");
			ctx.addServlet(holder, "/");

			Server server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(0);
			server.setConnectors(new Connector[] { connector });

			server.setHandler(ctx);
			server.start();
			server.join();

			String host = connector.getHost();
			if (host == null) {
				host = "localhost";
			}
			int port = connector.getLocalPort();
			return String.format("http://%s:%d/", host, port);

		} catch (Exception e) {
			log.error("Error starting Jetty", e);
			throw new RuntimeException(e);
		}
	}

	public void start(Class<? extends FrontServlet> frontServletClass) {

		try {
			ServletHolder holder = new ServletHolder(frontServletClass);
			holder.setInitOrder(0);

			ServletContextHandler ctx = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			ctx.setContextPath("");
			ctx.addServlet(holder, "/");

			Server server = new Server(8080);
			server.setHandler(ctx);
			server.start();
			server.join();
		} catch (Exception e) {
			log.error("Error starting Jetty", e);
		}

	}

	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			throw new RuntimeException("Error while stopping server", e);
		}
	}

}
