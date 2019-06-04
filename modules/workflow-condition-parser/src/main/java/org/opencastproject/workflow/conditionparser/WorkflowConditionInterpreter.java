/**
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

package org.opencastproject.workflow.conditionparser;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionLexer;
import org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorkflowConditionInterpreter {
  private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(?<varname>[^:}]+)(:(?<def>[^}]+))?}");

  private WorkflowConditionInterpreter() {
  }

  /**
   * Replaces all occurrences of <code>${.*+}</code> with the property in the provided map, or if not available in the
   * map, from the bundle context properties, if available.
   *
   * @param source
   *          The source string
   * @param properties
   *          The map of properties to replace
   * @return The resulting string
   */
  public static String replaceVariables(String source, Function<String, String> systemPropertyGetter,
          Map<String, String> properties, boolean quoteStrings) {
    Matcher matcher = PROPERTY_PATTERN.matcher(source);
    StringBuilder result = new StringBuilder();
    int cursor = 0;
    boolean matchFound = matcher.find();
    if (!matchFound)
      return source;
    while (matchFound) {
      int matchStart = matcher.start();
      int matchEnd = matcher.end();
      result.append(source, cursor, matchStart); // add the content before the match
      String key = source.substring(matchStart + 2, matchEnd - 1);
      String systemProperty = systemPropertyGetter.apply(key);
      String providedProperty = null;
      if (properties != null) {
        providedProperty = properties.get(key);
      }
      final String toAppend;
      if (isNotBlank(providedProperty)) {
        toAppend = providedProperty;
      } else if (isNotBlank(systemProperty)) {
        toAppend = systemProperty;
      } else {
        toAppend = null;
      }
      if (toAppend != null) {
        if (!quoteStrings) {
          result.append(toAppend);
        } else {
          try {
            if ("false".equals(toAppend) || "true".equals(toAppend)) {
              result.append(toAppend);
            } else {
              Integer.parseInt(toAppend);
              result.append(toAppend);
            }
          } catch (NumberFormatException e) {
            result.append("'").append(toAppend.replace("''", "'")).append("'");
          }
        }
      } else {
        result.append(source, matchStart, matchEnd);
      }
      cursor = matchEnd;
      matchFound = matcher.find();
      if (!matchFound)
        result.append(source.substring(matchEnd));
    }
    return result.toString();
  }

  static String replaceDefaults(String source) {
    final Matcher matcher = PROPERTY_PATTERN.matcher(source);
    final StringBuilder result = new StringBuilder();
    int cursor = 0;
    boolean matchFound = matcher.find();
    if (!matchFound) {
      return source;
    }
    while (matchFound) {
      int matchStart = matcher.start();
      int matchEnd = matcher.end();
      result.append(source, cursor, matchStart); // add the content before the match
      String defaultValue = matcher.group("def");
      if (defaultValue == null) {
        defaultValue = "false";
      }
      result.append(defaultValue);
      cursor = matchEnd;
      matchFound = matcher.find();
      if (!matchFound)
        result.append(source.substring(matchEnd));
    }
    return result.toString();
  }


  public static boolean interpret(final String input) throws IllegalArgumentException {
    final String s = replaceDefaults(input);
    final WorkflowConditionLexer l = new WorkflowConditionLexer(CharStreams.fromString(s));
    l.removeErrorListeners();
    final ANTLRErrorListener listener = new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
              String msg, RecognitionException e)
              throws ParseCancellationException {
        throw new IllegalArgumentException("line " + line + ":" + charPositionInLine + " " + msg);
      }
    };
    final WorkflowConditionParser p = new WorkflowConditionParser(new CommonTokenStream(l));
    p.removeErrorListeners();
    p.addErrorListener(listener);
    ParseTree tree = p.booleanExpression();
    return new WorkflowConditionBooleanInterpreter().visit(tree);
  }
}
