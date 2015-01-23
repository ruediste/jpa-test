package com.github.ruediste;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

import javax.persistence.Persistence;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.github.ruediste.framework.entry.Application;
import com.github.ruediste.framework.entry.ApplicationModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Hello world!
 *
 */
public class App {

	public static void main(String[] args) throws SQLException {
		Injector injector = Guice.createInjector(new ApplicationModule());

		// open and hold DB connection
		Connection connection = DriverManager.getConnection(
				"jdbc:h2:mem:testdb", "sa", "");
		
		// initialize schema
		{
			HashMap<String, Object> props=new HashMap<>();
			props.put(PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION, PersistenceUnitProperties.SCHEMA_GENERATION_DROP_AND_CREATE_ACTION);
			Persistence.generateSchema("my-app", props);
		}
		
		injector.getInstance(Application.class).start(
				TestApplicationInstance.class);
	}

}
