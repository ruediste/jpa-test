package com.github.ruediste.framework.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.google.common.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Module;

public abstract class ContainerTestBase<T extends ApplicationInstance> {

	protected T applicationInstance;

	protected String serverUrl;

	private Application application;

	protected WebDriver createDriver() {
		return new HtmlUnitDriver();
	}

	/**
	 * Hook to add additional {@link Guice}- {@link Module}s for the
	 * application-injector
	 */
	protected void collectApplicationModules(Consumer<Module[]> consumer) {

	}

	@SuppressWarnings("unchecked")
	@Before
	public void setupContainerTestBase() throws Exception {

		ArrayList<Module> modules = new ArrayList<>();
		modules.add(new ApplicationModule());
		collectApplicationModules(m -> modules.addAll(Arrays.asList(m)));

		application = Guice.createInjector(modules).getInstance(
				Application.class);

		Class<?> instanceType = TypeToken.of(getClass())
				.resolveType(ContainerTestBase.class.getTypeParameters()[0])
				.getRawType();
		try {
			applicationInstance = (T) instanceType.getDeclaredConstructor(
					getClass()).newInstance(this);
		} catch (NoSuchMethodException e) {
			// try other variant
			applicationInstance = (T) instanceType.getDeclaredConstructor()
					.newInstance();
		}

		serverUrl = application.startForTesting(applicationInstance);
	}

	@After
	public void tearDownContainerTestBase() {
		application.stop();
	}

}
