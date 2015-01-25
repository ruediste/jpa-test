package com.github.ruediste.laf.core.entry;

import com.google.inject.Guice;

public class TestFrontServlet extends FrontServlet{

	@Override
	protected void initImpl() throws Exception {
		Guice.createInjector(new ApplicationModule()).injectMembers(this);
	}

}
