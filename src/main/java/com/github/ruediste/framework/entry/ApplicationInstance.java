package com.github.ruediste.framework.entry;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ApplicationInstance {

	
	public final void start() {
		
		startImpl();
	}

	protected void startImpl() {
		
	}

	public abstract void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException;

	public final void close() {
		closeImpl();
	}

	protected void closeImpl() {
		
	};
}
