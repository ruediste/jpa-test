package test.test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.io.ByteStreams;

import javafx.util.converter.ByteStringConverter;

public class FilteringClassLoader extends ClassLoader {

	private String pkg;
	private ClassLoader parent;

	public FilteringClassLoader(String pkg, ClassLoader parent) {
		this.pkg = pkg;
		this.parent = parent;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		InputStream input = parent.getResourceAsStream(name.replace(
				".", "/") + ".class");
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
				Class<?> result=findClass(name);
				if (resolve)
					resolveClass(result);
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}
}
