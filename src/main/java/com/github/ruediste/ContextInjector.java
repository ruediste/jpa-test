package com.github.ruediste;

import com.google.inject.Injector;

/**
 * Provides access to the current {@link Injector} for the current thread.
 */
public class ContextInjector {

	private static final ThreadLocal<Injector> instance=new ThreadLocal<Injector>();
	public static Injector getInjector(){
		return instance.get();
	}
	public static void setInjector(Injector i){
		instance.set(i);
	}
}
