/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.graphql.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is a utility class that provides methods for scanning annotations in classes.
 * It provides a public method to find and return a collection of classes that are annotated with a specific annotation.
 * It also provides private methods to recursively scan a given path in a bundle for class files
 * and to load a class given its fully qualified name and a class loader.*
 */
public final class AnnotationScanner {

  private static final Logger logger = LoggerFactory.getLogger(AnnotationScanner.class);

  private AnnotationScanner() {
  }


  /**
   * Finds and returns a collection of classes that are annotated with a specific annotation.
   *
   * @param context The class from which the context class loader is derived. This is typically the class in which
   *                you are working.
   * @param annotation The annotation that the classes must have to be included in the returned collection.
   * @return A collection of classes that are annotated with the specified annotation.
   */
  public static Collection<Class<?>> findAnnotatedClasses(Class<?> context, Class<? extends Annotation> annotation) {
    return FrameworkUtil.getBundle(context.getClassLoader())
        .map(b -> entryPaths(b, context.getPackageName().replace('.', '/')))
        .orElse(Stream.empty())
        .map(clazz -> loadClass(clazz, context.getClassLoader()))
        .filter(Objects::nonNull)
        .filter(c -> c.isAnnotationPresent(annotation))
        .collect(Collectors.toSet());
  }

  /**
   * Recursively scans the given path in the bundle for class files, returning a stream of their fully qualified names.
   *
   * @param bundle The bundle to scan for class files.
   * @param path The path in the bundle to start scanning from.
   * @return A stream of fully qualified class names found in the bundle starting from the given path.
   */
  private static Stream<String> entryPaths(Bundle bundle, String path) {
    Enumeration<String> entries = bundle.getEntryPaths(path);
    if (entries == null) {
      return Stream.empty();
    }
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(entries.asIterator(),
        Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT), false)
        .flatMap(name ->
            name.endsWith(".class") && !name.contains("$")
                ? Stream.of(name.replace('/', '.').replace(".class", ""))
                : entryPaths(bundle, name)
        );
  }

  /**
   * Attempts to load a class given its fully qualified name and a class loader.
   *
   * @param name The fully qualified name of the class to load.
   * @param classLoader The class loader to use to load the class.
   * @return The loaded class, or null if the class could not be found or loaded.
   */
  private static Class<?> loadClass(String name, ClassLoader classLoader) {
    try {
      return Class.forName(name, true, classLoader);
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      logger.debug("Class not found: {}", name);
      return null;
    }
  }
}
