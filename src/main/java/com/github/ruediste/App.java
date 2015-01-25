package com.github.ruediste;

import java.sql.SQLException;

import com.github.ruediste.laf.core.entry.Application;

/**
 * Hello world!
 *
 */
public class App {

	public static void main(String[] args) throws SQLException {
		new Application().start(DemoFrontServlet.class);
	}

}
