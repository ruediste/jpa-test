package com.github.ruediste;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;

import javax.persistence.Persistence;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.github.ruediste.laf.core.entry.ApplicationInstance;
import com.github.ruediste.laf.core.entry.ApplicationModule;
import com.github.ruediste.laf.core.entry.FrontServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class DemoFrontServlet extends FrontServlet {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unused")
	private Connection connection;

	@Override
	protected void initImpl() throws Exception{
		Injector injector = Guice.createInjector(new ApplicationModule());
		injector.injectMembers(this);

		// open connection and hold it
		connection = DriverManager.getConnection(
				"jdbc:h2:mem:testdb", "sa", "");

		// initialize schema
		{
			HashMap<String, Object> props = new HashMap<>();
			props.put(
					PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION,
					PersistenceUnitProperties.SCHEMA_GENERATION_DROP_AND_CREATE_ACTION);
			Persistence.generateSchema("my-app", props);
		}
	}

	@Override
	protected Class<? extends ApplicationInstance> getApplicationInstanceClass() {
		return DemoApplicationInstance.class;
	}

}
