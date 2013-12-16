/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.util;

import org.opencastproject.fn.juc.Immutables;
import org.opencastproject.fn.juc.Iterables;
import org.opencastproject.fn.juc.Mutables;
import org.opencastproject.util.data.Prelude;
import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import static java.lang.String.format;

/**
 * A logger that maintains a "unit of work" context" to facilitate the grouping of log statements.
 * <p/>
 * Log methods that take message formats and arguments use the
 * {@link String#format(java.util.Locale, String, Object...)} string format syntax.
 */
public final class Log {
  private static final String FQCN = Log.class.getName();
  private static final String JVM_SESSION = randomString();

  /** Hold the unit of work stack. */
  private static final ThreadLocal<Stack<String>> uows = new ThreadLocal<Stack<String>>() {
    @Override protected Stack<String> initialValue() {
      return Mutables.stack(JVM_SESSION);
    }
  };

  /** Hold the current unit of work hierarchy as a string ready to use for the log methods. */
  private static final ThreadLocal<String> current = new ThreadLocal<String>() {
    @Override protected String initialValue() {
      return uowString();
    }
  };

  private final Logger logger;
  private final boolean isLocationAware;

  /** Create a new logger from an slf4j logger. */
  public Log(Logger logger) {
    this.logger = logger;
    this.isLocationAware = logger instanceof LocationAwareLogger;
  }

  private static void updateCurrent() {
    current.set(uowString());
  }

  private static String uowString() {
    return Iterables.mkString(uows.get(), "_", "[>", "] ");
  }

  private static String randomString() {
    return UUID.randomUUID().toString().split("-")[0];
  }

  /** Start a new unit of work. */
  public void startUnitOfWork() {
    uows.get().push(randomString());
    updateCurrent();
  }

  /** End a unit of work. */
  public void endUnitOfWork() {
    if (uows.get().size() > 1) {
      uows.get().pop();
      updateCurrent();
    }
  }

  /** Continue a unit of work. */
  public void continueUnitOfWork(Collection<String> init) {
    uows.set(Mutables.stack(init));
    updateCurrent();
  }

  /** Return the current unit of work. */
  public List<String> getUnitOfWork() {
    return Immutables.mk(uows.get());
  }

  public void debug(String msg) {
    log(LocationAwareLogger.DEBUG_INT, null, msg);
  }

  public void debug(String msg, Object... args) {
    log(LocationAwareLogger.DEBUG_INT, null, msg, args);
  }

  public void info(String msg) {
    log(LocationAwareLogger.INFO_INT, null, msg);
  }

  public void info(String msg, Object... args) {
    log(LocationAwareLogger.INFO_INT, null, msg, args);
  }

  public void warn(String msg) {
    log(LocationAwareLogger.WARN_INT, null, msg);
  }

  public void warn(String msg, Object... args) {
    log(LocationAwareLogger.WARN_INT, null, msg, args);
  }

  public void warn(Throwable t, String msg) {
    log(LocationAwareLogger.WARN_INT, t, msg);
  }

  public void error(String msg) {
    log(LocationAwareLogger.ERROR_INT, null, msg);
  }

  public void error(String msg, Object... args) {
    log(LocationAwareLogger.ERROR_INT, null, msg, args);
  }

  public void error(Throwable t, String msg, Object... args) {
    log(LocationAwareLogger.ERROR_INT, t, msg, args);
  }

  /** <code>t</code> maybe null */
  private void log(int level, Throwable t, String format, Object... args) {
    final String msg = current.get() + format(format, args);
    if (isLocationAware) {
      ((LocationAwareLogger) logger).log(null, FQCN, level, msg, null, t);
    } else {
      switch (level) {
        case LocationAwareLogger.INFO_INT:
          logger.info(msg);
          break;
        case LocationAwareLogger.WARN_INT:
          logger.warn(msg);
          break;
        case LocationAwareLogger.ERROR_INT:
          logger.error(msg);
          break;
        default:
          Prelude.unexhaustiveMatch();
      }
    }
  }
}
