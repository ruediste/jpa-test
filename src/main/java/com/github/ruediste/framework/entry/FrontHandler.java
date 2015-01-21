package com.github.ruediste.framework.entry;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;

public class FrontHandler extends AbstractHandler {

	Logger log;

	public volatile ApplicationInstanceInfo currentInstance;

	@Override
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		if (currentInstance != null) {

			Thread currentThread = Thread.currentThread();
			ClassLoader old = currentThread.getContextClassLoader();
			try {
				currentThread
						.setContextClassLoader(currentInstance.classLoader);
				currentInstance.instance.handle(target, request, response,
						dispatch);
			} finally {
				currentThread.setContextClassLoader(old);
			}
		} else
			log.warn("current application instance is null");
	}

}
