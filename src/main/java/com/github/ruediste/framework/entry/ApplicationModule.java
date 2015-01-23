package com.github.ruediste.framework.entry;

import com.google.inject.AbstractModule;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new LoggerBindingModule());
	}

}