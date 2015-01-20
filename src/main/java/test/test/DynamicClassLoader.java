package test.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.common.reflect.Reflection;

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

	private static final String CLASS_FILE_NAME_EXTENSION = ".class";

	private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(
			" ").omitEmptyStrings();

	public static class ResourceInfo {
		private final String resourceName;
		final ClassLoader loader;

		static ResourceInfo of(String resourceName, ClassLoader loader) {
			if (resourceName.endsWith(CLASS_FILE_NAME_EXTENSION)) {
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

	public static final class ClassInfo extends ResourceInfo {
		private final String className;

		ClassInfo(String resourceName, ClassLoader loader) {
			super(resourceName, loader);
			this.className = getClassName(resourceName);
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

	private class Scanner {
		private final ImmutableSortedSet.Builder<ResourceInfo> resources = new ImmutableSortedSet.Builder<ResourceInfo>(
				Ordering.usingToString());
		private final Set<URI> scannedUris = Sets.newHashSet();

		private void scan(ClassLoader cl) {
			if (cl.getParent() != null)
				scan(cl.getParent());

			if (cl instanceof URLClassLoader) {
				for (URL url : ((URLClassLoader) cl).getURLs()) {
					try {
						URI uri = url.toURI();
						scan(uri, cl);
					} catch (URISyntaxException | IOException e) {
						log.warn("Error while scanning classpath, ignoring", e);
					}

				}
			}
		}

		void scan(URI uri, ClassLoader classloader) throws IOException {
			if (uri.getScheme().equals("file") && scannedUris.add(uri)) {
				scanFrom(new File(uri), classloader);
			}
		}

		void scanFrom(File file, ClassLoader classloader) throws IOException {
			if (!file.exists()) {
				return;
			}
			if (file.isDirectory()) {
				scanDirectory(file, classloader);
			} else {
				scanJar(file, classloader);
			}
		}

		private void scanDirectory(File directory, ClassLoader classloader)
				throws IOException {
			scanDirectory(directory, classloader, "", ImmutableSet.<File> of());
		}

		private void scanDirectory(File directory, ClassLoader classloader,
				String packagePrefix, ImmutableSet<File> ancestors)
				throws IOException {
			File canonical = directory.getCanonicalFile();
			if (ancestors.contains(canonical)) {
				// A cycle in the filesystem, for example due to a symbolic
				// link.
				return;
			}
			File[] files = directory.listFiles();
			if (files == null) {
				log.warn("Cannot read directory " + directory + ", ignoring");
				// IO error, just skip the directory
				return;
			}
			ImmutableSet<File> newAncestors = ImmutableSet.<File> builder()
					.addAll(ancestors).add(canonical).build();
			for (File f : files) {
				String name = f.getName();
				if (f.isDirectory()) {
					scanDirectory(f, classloader, packagePrefix + name + "/",
							newAncestors);
				} else {
					String resourceName = packagePrefix + name;
					if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
						resources.add(ResourceInfo
								.of(resourceName, classloader));
					}
				}
			}
		}

		private void scanJar(File file, ClassLoader classloader)
				throws IOException {
			JarFile jarFile;
			try {
				jarFile = new JarFile(file);
			} catch (IOException e) {
				// Not a jar file
				return;
			}
			try {
				for (URI uri : getClassPathFromManifest(file,
						jarFile.getManifest())) {
					scan(uri, classloader);
				}
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.isDirectory()
							|| entry.getName().equals(JarFile.MANIFEST_NAME)) {
						continue;
					}
					resources
							.add(ResourceInfo.of(entry.getName(), classloader));
				}
			} finally {
				try {
					jarFile.close();
				} catch (IOException ignored) {
				}
			}
		}

		/**
		 * Returns the class path URIs specified by the {@code Class-Path}
		 * manifest attribute, according to <a href=
		 * "http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes"
		 * > JAR File Specification</a>. If {@code manifest} is null, it means
		 * the jar file has no manifest, and an empty set will be returned.
		 */
		ImmutableSet<URI> getClassPathFromManifest(File jarFile,
				Manifest manifest) {
			if (manifest == null) {
				return ImmutableSet.of();
			}
			ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
			String classpathAttribute = manifest.getMainAttributes().getValue(
					Attributes.Name.CLASS_PATH.toString());
			if (classpathAttribute != null) {
				for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR
						.split(classpathAttribute)) {
					URI uri;
					try {
						uri = getClassPathEntry(jarFile, path);
					} catch (URISyntaxException e) {
						// Ignore bad entry
						log.warn("Invalid Class-Path entry: " + path);
						continue;
					}
					builder.add(uri);
				}
			}
			return builder.build();
		}

		/**
		 * Returns the absolute uri of the Class-Path entry value as specified
		 * in <a href=
		 * "http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes"
		 * > JAR File Specification</a>. Even though the specification only
		 * talks about relative urls, absolute urls are actually supported too
		 * (for example, in Maven surefire plugin).
		 */
		URI getClassPathEntry(File jarFile, String path)
				throws URISyntaxException {
			URI uri = new URI(path);
			if (uri.isAbsolute()) {
				return uri;
			} else {
				return new File(jarFile.getParentFile(), path.replace('/',
						File.separatorChar)).toURI();
			}
		}
	}

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
