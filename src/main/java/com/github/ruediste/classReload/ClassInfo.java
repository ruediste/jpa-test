package com.github.ruediste.classReload;

import com.google.common.base.CharMatcher;
import com.google.common.reflect.Reflection;

public final class ClassInfo extends ResourceInfo {
	private final String className;

	ClassInfo(String resourceName, ClassLoader loader) {
		super(resourceName, loader);
		this.className = DynamicClassLoader.getClassName(resourceName);
	}

	/**
	 * Returns the package name of the class, without attempting to load the
	 * class.
	 * 
	 * <p>
	 * Behaves identically to {@link Package#getName()} but does not require
	 * the class (or package) to be loaded.
	 */
	public String getPackageName() {
		return Reflection.getPackageName(className);
	}

	/**
	 * Returns the simple name of the underlying class as given in the
	 * source code.
	 * 
	 * <p>
	 * Behaves identically to {@link Class#getSimpleName()} but does not
	 * require the class to be loaded.
	 */
	public String getSimpleName() {
		int lastDollarSign = className.lastIndexOf('$');
		if (lastDollarSign != -1) {
			String innerClassName = className.substring(lastDollarSign + 1);
			// local and anonymous classes are prefixed with number
			// (1,2,3...), anonymous classes are
			// entirely numeric whereas local classes have the user supplied
			// name as a suffix
			return CharMatcher.DIGIT.trimLeadingFrom(innerClassName);
		}
		String packageName = getPackageName();
		if (packageName.isEmpty()) {
			return className;
		}

		// Since this is a top level class, its simple name is always the
		// part after package name.
		return className.substring(packageName.length() + 1);
	}

	/**
	 * Returns the fully qualified name of the class.
	 * 
	 * <p>
	 * Behaves identically to {@link Class#getName()} but does not require
	 * the class to be loaded.
	 */
	public String getName() {
		return className;
	}

	/**
	 * Loads (but doesn't link or initialize) the class.
	 *
	 * @throws LinkageError
	 *             when there were errors in loading classes that this class
	 *             depends on. For example, {@link NoClassDefFoundError}.
	 */
	public Class<?> load() {
		try {
			return loader.loadClass(className);
		} catch (ClassNotFoundException e) {
			// Shouldn't happen, since the class name is read from the class
			// path.
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		return className;
	}
}