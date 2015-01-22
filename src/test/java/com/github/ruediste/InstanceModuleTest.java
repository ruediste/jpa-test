package com.github.ruediste;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class InstanceModuleTest {

	private Injector injector;
	private TestService service;

	@Singleton
	public static class TestService {

		private String foo;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@Before
	public void setup() {
		injector = Guice.createInjector(new InstanceModule());
		service = injector.getInstance(TestService.class);
	}

	@Test
	public void testSingletonBehaviour() {
		service.setFoo("bar");
		assertEquals("bar", service.getFoo());
		service = injector.getInstance(TestService.class);
		assertEquals("bar", service.getFoo());
	}

	@Test
	public void testSerialization() throws Exception {
		service.setFoo("bar");

		// write
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(service);
		out.flush();

		// read
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		TestService readService = (TestService) ois.readObject();

		// check
		assertEquals("bar", readService.getFoo());
	}
}
