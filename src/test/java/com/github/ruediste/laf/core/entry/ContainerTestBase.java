package com.github.ruediste.laf.core.entry;

import java.lang.reflect.Constructor;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.google.common.reflect.TypeToken;

public abstract class ContainerTestBase<T extends ApplicationInstance> {

	protected T applicationInstance;

	protected String serverUrl;

	private Application application;

	protected WebDriver createDriver() {
		return new HtmlUnitDriver();
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setupContainerTestBase() throws Exception {

		TestFrontServlet servlet = new TestFrontServlet();

		Class<?> instanceType = TypeToken.of(getClass())
				.resolveType(ContainerTestBase.class.getTypeParameters()[0])
				.getRawType();
		try {
			Constructor<?> constructor = instanceType.getDeclaredConstructor(
					getClass());
			constructor.setAccessible(true);
			applicationInstance = (T) constructor.newInstance(this);
		} catch (NoSuchMethodException e) {
			// try other variant
			Constructor<?> constructor = instanceType.getDeclaredConstructor();
			constructor.setAccessible(true);
			applicationInstance = (T) constructor
					.newInstance();
		}

		servlet.setFixedApplicationInstance(applicationInstance);
		
		application=new Application();
		serverUrl = application.startForTesting(servlet);
	}

	@After
	public void tearDownContainerTestBase() {
		application.stop();
	}

}
