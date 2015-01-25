package com.github.ruediste.laf.core.entry;

import javax.inject.Inject;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;

public class Application {

	Logger log;

	@Inject
	FrontServlet frontServlet;
	
	private Server server;

	public String startForTesting(ApplicationInstance instance) {
		try {
			FrontServlet servlet=new FrontServlet();
			servlet.setFixedApplicationInstance(instance);
			
			ServletHolder holder=new ServletHolder(servlet);

			ServletContextHandler ctx=new ServletContextHandler(ServletContextHandler.SESSIONS);
			ctx.setContextPath("/");
			ctx.addServlet(holder,"/");
			
			Server server = new Server();
			ServerConnector connector=new ServerConnector(server);
			connector.setPort(0);
			server.setConnectors(new Connector[]{connector});
			
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

	public void start(Class<? extends ApplicationInstance> instanceClass) {

		try {
			ServletHolder holder=new ServletHolder(FrontServlet.class);
			holder.setInitParameter(FrontServlet.APPLICATION_INSTANCE_CLASS_NAME_INIT_PARAMETER_KEY, instanceClass.getName());

			ServletContextHandler ctx=new ServletContextHandler(ServletContextHandler.SESSIONS);
			ctx.setContextPath("");
			ctx.addServlet(holder,"/");
			
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
