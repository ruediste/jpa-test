package com.github.ruediste.classReload;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class Scanner {
	@Inject
	Logger log;
	private final ImmutableSortedSet.Builder<ResourceInfo> resources = new ImmutableSortedSet.Builder<ResourceInfo>(
			Ordering.usingToString());
	private final Set<URI> scannedUris = Sets.newHashSet();

	void scan(ClassLoader cl) {
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
			for (String path : DynamicClassLoader.CLASS_PATH_ATTRIBUTE_SEPARATOR
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