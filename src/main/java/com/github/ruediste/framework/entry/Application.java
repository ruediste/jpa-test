package com.github.ruediste.framework.entry;

import java.util.HashSet;

import javax.inject.Inject;

import org.mortbay.jetty.Server;
import org.slf4j.Logger;

import com.github.ruediste.framework.classReload.DynamicClassLoader;
import com.google.inject.Provider;

public class Application {

	Logger log;

	@Inject
	Provider<DynamicClassLoader> loaderProvider;

	private Class<? extends ApplicationInstance> instanceClass;

	private FrontHandler handler;

	private final Object lock = new Object();
	private boolean reloadInProgress;
	private boolean reloadRequested;

	private void reloadApplicationInstance() {
		log.debug("Entering reloadApplicationInstance");
		synchronized (lock) {
			if (reloadInProgress) {
				reloadRequested = true;
				return;
			}

			// we are going to do the reload
			reloadInProgress = true;
			reloadRequested = false;
		}
		// loop to reload until no reload is requested during reload
		while (true)
			try {
				// wait a bit to consolidate changes
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				log.info("Reloading application instance");
				// create new instance
				DynamicClassLoader cl = loaderProvider.get();
				HashSet<String> projects = new HashSet<>();
				projects.add("test");
				cl.initialize(projects, new Runnable() {

					boolean fired;

					@Override
					public synchronized void run() {
						log.debug("class was changed ");
						synchronized (this) {
							if (fired)
								return;
							fired = true;
						}
						cl.close();
						// when anything changes, update instance
						reloadApplicationInstance();
					}
				});

				// close current instnace
				if (handler.currentInstance!=null){
					handler.currentInstance.instance.close();
				}
				
				// create application instance
				ApplicationInstance instance;
				try {

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
				} catch (InstantiationException | IllegalAccessException
						| ClassNotFoundException e) {
					log.warn("Error loading application instance");
				}
				log.info("Reloading complete");
			} finally {
				synchronized (lock) {
					if (reloadRequested) {
						// do another reload
						reloadRequested = false;
					} else {
						// we are done
						reloadInProgress = false;
						break;
					}
				}
			}
	}

	public void start(Class<? extends ApplicationInstance> instanceClass) {
		this.instanceClass = instanceClass;
		try {
			handler = new FrontHandler();

			// initialize the application instance
			reloadApplicationInstance();

			// start jetty server
			Server server = new Server(8080);
			server.setHandler(handler);
			server.start();
			server.join();

		} catch (Exception e) {
			log.error("Error in Server", e);
		}
	}
}
