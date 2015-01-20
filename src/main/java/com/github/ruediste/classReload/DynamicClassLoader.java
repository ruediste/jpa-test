package com.github.ruediste.classReload;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import javafx.util.converter.ByteStringConverter;

public class DynamicClassLoader extends ClassLoader {
	@Inject
	Logger log;

	private String pkg;
	private ClassLoader parent;

	public DynamicClassLoader(String pkg, ClassLoader parent) {
		this.pkg = pkg;
		this.parent = parent;
		Scanner scanner = new Scanner();
		scanner.scan(parent);
	}

	static final String CLASS_FILE_NAME_EXTENSION = ".class";

	static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(
			" ").omitEmptyStrings();

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		InputStream input = parent.getResourceAsStream(name.replace(".", "/")
				+ ".class");
		Class<?> result;
		try {
			byte[] bb = ByteStreams.toByteArray(input);
			result = defineClass(name, bb, 0, bb.length);
			input.close();
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
		return result;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (name.startsWith(pkg)) {
			synchronized (getClassLoadingLock(name)) {
				Class<?> result = findClass(name);
				if (resolve)
					resolveClass(result);
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}

	static String getClassName(String filename) {
		int classNameEnd = filename.length()
				- CLASS_FILE_NAME_EXTENSION.length();
		return filename.substring(0, classNameEnd).replace('/', '.');
	}
}
