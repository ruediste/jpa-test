package com.github.ruediste.framework.entry;

import java.util.HashSet;

import javax.inject.Inject;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.SessionHandler;
import org.slf4j.Logger;

import com.github.ruediste.framework.classReload.DynamicClassLoader;
import com.github.ruediste.framework.classReload.Gate;
import com.google.inject.Provider;

public class Application {

	Logger log;

	@Inject
	Provider<DynamicClassLoader> loaderProvider;

	private Class<? extends ApplicationInstance> instanceClass;

	@Inject
	FrontHandler handler;

	private Gate applicationInstanceInitiallyLoaded = new Gate();

	private Server server;

	public String startForTesting(ApplicationInstance instance) {
		handler.currentInstance = new ApplicationInstanceInfo(instance, Thread
				.currentThread().getContextClassLoader());
		try {
			server = new Server();

			SocketConnector connector = new SocketConnector();
			connector.setPort(0); // let connector pick an unused port #
			server.addConnector(connector);

			SessionHandler sessionHandler = new SessionHandler();
			sessionHandler.setHandler(handler);
			sessionHandler.setServer(server);

			server.setHandler(sessionHandler);
			server.start();

			instance.start();

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
		this.instanceClass = instanceClass;
		// initialize the application instance
		{
			Thread reloadThread = new Thread(new ApplicationInstanceReloader(),
					"Application Instance reload thread");
			reloadThread.setDaemon(true);
			reloadThread.start();
		}

		applicationInstanceInitiallyLoaded.pass();

		try {
			Server server = new Server(8080);
			server.setHandler(handler);
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

	private class ApplicationInstanceReloader implements Runnable {

		Gate reloadGate = new Gate(true);
		private boolean initialLoading = true;

		@Override
		public void run() {
			while (true) {
				log.info("Waiting for reload trigger");

				reloadGate.pass();

				log.info("Reloading application instance");
				// wait a bit to consolidate changes
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				// close the gate, such that the changes can open it again
				reloadGate.close();

				try {
					// close current instance
					if (handler.currentInstance != null) {
						handler.currentInstance.instance.close();
					}
				} catch (Throwable t) {
					log.error("Error while closing current instance", t);
				}

				DynamicClassLoader cl;
				try {
					cl = loaderProvider.get();
					// create new class loader
					HashSet<String> projects = new HashSet<>();
					projects.add("test");
					cl.initialize(projects, new Runnable() {

						boolean fired;

						@Override
						public synchronized void run() {
							log.debug("class was changed ");

							// fire once only
							synchronized (this) {
								if (fired)
									return;
								fired = true;
							}
							cl.close();
							// when anything changes, update instance
							reloadGate.open();
						}
					});
				} catch (Throwable t) {
					log.error(
							"Error while creating new class loader, quiting reload loop. Restart server",
							t);
					throw t;
				}

				try {
					// create application instance
					ApplicationInstance instance;

					Thread currentThread = Thread.currentThread();
					ClassLoader old = currentThread.getContextClassLoader();
					try {
						currentThread.setContextClassLoader(cl);

						instance = (ApplicationInstance) cl.loadClass(
								instanceClass.getName()).newInstance();

					} finally {
						currentThread.setContextClassLoader(old);
					}
					handler.currentInstance = new ApplicationInstanceInfo(
							instance, cl);

					instance.start();
					log.info("Reloading complete");
				} catch (Throwable t) {
					log.warn("Error loading application instance", t);
				}

				if (initialLoading) {
					initialLoading = false;
					applicationInstanceInitiallyLoaded.open();
				}
			}
		}

	}
}
