package com.github.ruediste.classReload;

import java.net.URL;

public class ResourceInfo {
	private final String resourceName;
	final ClassLoader loader;

	static ResourceInfo of(String resourceName, ClassLoader loader) {
		if (resourceName.endsWith(DynamicClassLoader.CLASS_FILE_NAME_EXTENSION)) {
			return new ClassInfo(resourceName, loader);
		} else {
			return new ResourceInfo(resourceName, loader);
		}
	}

	ResourceInfo(String resourceName, ClassLoader loader) {
		this.resourceName = resourceName;
		this.loader = loader;
	}

	/** Returns the url identifying the resource. */
	public final URL url() {
		return loader.getResource(resourceName);
	}

	/**
	 * Returns the fully qualified name of the resource. Such as
	 * "com/mycomp/foo/bar.txt".
	 */
	public final String getResourceName() {
		return resourceName;
	}

	@Override
	public int hashCode() {
		return resourceName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResourceInfo) {
			ResourceInfo that = (ResourceInfo) obj;
			return resourceName.equals(that.resourceName)
					&& loader == that.loader;
		}
		return false;
	}

	// Do not change this arbitrarily. We rely on it for sorting
	// ResourceInfo.
	@Override
	public String toString() {
		return resourceName;
	}
}