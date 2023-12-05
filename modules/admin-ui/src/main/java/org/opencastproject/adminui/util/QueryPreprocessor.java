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

package org.opencastproject.adminui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to preprocess potentially malformed Lucene query strings.
 *
 * The following sanitations are performed:
 *
 * - Escape special characters that would potentially lead to malformed queries
 * - Enable partial search by adding '*' as both prefix and suffix to individual terms
 *   - Exception: Double-quoted terms
 * - Sanitize use of double quotes by appending a double quote at the end of the sanitized query in case
 *   the closing double quote is missing
 * - Ensure that + und - are interpreted in a user-friendly way, i.e. test-unit is not interpreted as test -unit
 */
public final class QueryPreprocessor {

  private static final Logger logger = LoggerFactory.getLogger(QueryPreprocessor.class);

  private static final char DOUBLE_QUOTE = '"';
  private static final char MINUS = '-';
  private static final char PLUS = '+';
  private static final char ASTERISK = '*';
  private static final char EXPLANATION_MARK = '!';
  private static final char BACKSLASH = '\\';
  private static final char AMPERSAND = '&';
  private static final char PIPE = '|';

  private static final Set<Character> ESCAPED_CHARACTERS = new HashSet<Character>(Arrays.asList(
    MINUS,
    PLUS,
    EXPLANATION_MARK,
    BACKSLASH,
    AMPERSAND,
    PIPE,
    '(', ')', '{', '}', '[', ']', ':', '^', '~'
  ));

  private static final Set<Character> UNARY_OPERATORS = new HashSet<Character>(Arrays.asList(
    MINUS,
    PLUS,
    EXPLANATION_MARK
  ));

  private static final Set<String> BINARY_OPERATORS = new HashSet<String>(Arrays.asList("&&", "||"));

  private QueryPreprocessor() {

  }

  /**
   * Sanitize a potentially malformed query string so it conforms to the Lucene query syntax
   *
   * @param query
   *          potentially malformed Lucene query string
   * @return
   *        sanitized query string
   */
  public static String sanitize(String query) {
    String sanitizedQuery = "";
    String sanitizedToken;
    ArrayList<String> tokens = tokenize(query);
    int i = 0;
    while (i < tokens.size()) {
      String token = tokens.get(i);

      if (isUnaryOperator(token)) {
        sanitizedToken = sanitizeUnaryOperator(token);
      } else if (isBinaryOperator(token)) {
        if ((i == 0) || isBinaryOperator(tokens.get(i - 1)) || (i >= tokens.size() - 1) || isBinaryOperator(tokens.get(i + 1))) {
          // Escape operator since operands missing
          sanitizedToken = "" + BACKSLASH + token;
        } else {
          sanitizedToken = token;
        }
      } else {
        sanitizedToken = enablePartialMatches(token, 0);
      }

      if (i != 0) {
        sanitizedQuery += " ";
      }
      sanitizedQuery += sanitizedToken;
      i++;
    }
    logger.debug("Sanitized input '{}' to '{}'", query, sanitizedQuery);
    return sanitizedQuery;
  }

  private static boolean isUnaryOperator(String token) {
    return (token.length() > 0) && UNARY_OPERATORS.contains(token.charAt(0));
  }

  private static boolean isBinaryOperator(String token) {
    return BINARY_OPERATORS.contains(token);
  }

  /**
   * Helper method to enable partial matching for string literals or operand
   *
   * @param string
   *          token to be sanitized
   * @param begin
   *          first character of operand, 0 for string literals
   * @return
   *        the character found at specified position or ' ' if position not within string
   */
  private static String enablePartialMatches(String string, int begin) {
    String result = string;

    char ch = string.charAt(begin);
    if ((ch != DOUBLE_QUOTE) && (ch != ASTERISK)) {
      result = "";
      if (begin > 0) {
        result += string.substring(0, begin);
      }
      result += ASTERISK;
      result += string.substring(begin, string.length());
    }

    ch = result.charAt(result.length() - 1);
    if ((ch != DOUBLE_QUOTE) && (ch != ASTERISK)) {
      result += ASTERISK;
    }
    return result;
  }

  /**
   * Helper method to sanitize unary operator tokens
   * This method performes the following sanitizitations:
   * - Escape unary operator in case of missing argument (ensure syntactical correctness)
   * - Enable partial matching for the operand
   *
   * @param token
   *          token to be sanitized
   * @return
   *        the character found at specified position or ' ' if position not within string
   */
  private static String sanitizeUnaryOperator(String token) {
    String sanitizedToken;
    if (token.length() == 1) {
      // Escape unary operator because of missing operand
      sanitizedToken = "" + BACKSLASH + token.charAt(0);
    } else {
      sanitizedToken = enablePartialMatches(token, 1);
    }
    return sanitizedToken;
  }

  /**
   * Helper method to (pseudo)-tokenize a character sequence
   *
   * @param query
   *          string to be tokenized
   * @return
   *        list of tokens
   */
  private static ArrayList<String> tokenize(String query) {

    ArrayList<String> tokens = new ArrayList<String>();
    String currentToken = "";

    boolean openDoubleQuote = false;
    int i = 0;

    while (i < query.length()) {

      char ch = query.charAt(i);

      if (ch == DOUBLE_QUOTE) {
        if (openDoubleQuote) {
          currentToken += DOUBLE_QUOTE;
          tokens.add(currentToken);
          currentToken = "";
          openDoubleQuote = false;
        } else if (currentToken.isEmpty()
                   || (isUnaryOperator("" + charAt(i - 1, query)) && Character.isWhitespace(charAt(i - 2, query)))) {
          currentToken += DOUBLE_QUOTE;
          openDoubleQuote = true;
        } else {
          // Escape double quote character to enforce whitespace separated tokens
          currentToken += "" + BACKSLASH + DOUBLE_QUOTE;
        }
      } else if (openDoubleQuote) {
        // No special handling of characters within quoted strings
        currentToken += ch;
      } else if (isUnaryOperator("" + ch) && Character.isWhitespace(charAt(i - 1, query))) {
        // We only allow unary operators as first character of a token
        currentToken += ch;
      } else if (isBinaryOperator("" + ch + charAt(i + 1, query))
                 && Character.isWhitespace(charAt(i - 1, query))
                 && Character.isWhitespace(charAt(i + 2, query))) {
          // Binary operator detected, i.e. whitespace delimited && or ||
          tokens.add("" + ch + ch);
          i++; // We nastily skip the binary operator, i.e. we are taken two characters in this round
      } else if (Character.isWhitespace(ch)) {
        // Whitespace delimits tokens
        if (!currentToken.isEmpty()) {
          tokens.add(currentToken);
          currentToken = "";
        }
      } else {
        if (ESCAPED_CHARACTERS.contains(ch)) {
          currentToken += "" + BACKSLASH + ch;
        } else {
          currentToken += ch;
        }
      }
      i++;
    }
    if (!currentToken.isEmpty()) {
      if (openDoubleQuote) {
        // Syntax error detected. We fix this.
        currentToken += DOUBLE_QUOTE;
      }
      tokens.add(currentToken);
    }

    return tokens;
  }

  /**
   * Helper method to look up characters in strings without resulting in IndexOutOfBound exceptions
   *
   * @param position
   *          position within string get the characters
   * @param string
   *          the string we want to lookup a character
   * @return
   *        the character found at specified position or ' ' if position not within string
   */
  private static char charAt(int position, String string) {
    if ((0 <= position) && (position < string.length())) {
      return string.charAt(position);
    } else {
      return ' ';
    }
  }

}
