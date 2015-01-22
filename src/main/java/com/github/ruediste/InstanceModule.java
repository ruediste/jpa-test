package com.github.ruediste;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.github.ruediste.framework.loggerBinding.LoggerBindingModule;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.spi.Element;
import com.google.inject.spi.ScopeBinding;

class InstanceModule extends AbstractModule {

	@Override
	protected void configure() {
		Binder binder = binder();
		
		try {
			Field elementsField = binder.getClass().getDeclaredField(
					"elements");
			elementsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			ArrayList<Element> elements = (ArrayList<Element>) elementsField
					.get(binder);
			ArrayList<Element> clone = new ArrayList<Element>(elements);
			elements.clear();
			elements.addAll(clone.stream()
					.filter(x -> !(x instanceof ScopeBinding))
					.collect(Collectors.toList()));
		} catch (Exception e){
			throw new RuntimeException(e);
		}

		bindScope(javax.inject.Singleton.class, new SingletonScope());
		bindScope(com.google.inject.Singleton.class, new SingletonScope());

		install(new LoggerBindingModule());
	}

}