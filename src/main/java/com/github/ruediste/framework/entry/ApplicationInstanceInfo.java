package com.github.ruediste.framework.entry;

public class ApplicationInstanceInfo {

	public ApplicationInstance instance;
	public ClassLoader classLoader;
	public ApplicationInstanceInfo(ApplicationInstance instance,
			ClassLoader classLoader) {
		super();
		this.instance = instance;
		this.classLoader = classLoader;
	}
	
}
