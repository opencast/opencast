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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

/**
 * Utility class to preprocess potentially malformed Lucene query strings.
 *
 * The following sanitations are performed:
 *
 * - Escape special characters that would potentially lead to malformed queries
 * - Enable partial search by adding '*' as both prefix and suffix to individual terms
 * - Exception: Double-quoted terms
 * - Sanitize use of double quotes by appending a double quote at the end of the sanitized query in case
 * the closing double quote is missing
 * - Ensure that + und - are interpreted in a user-friendly way, i.e. test-unit is not interpreted as test -unit
 */
object QueryPreprocessor {

    private val logger = LoggerFactory.getLogger(QueryPreprocessor::class.java)

    private val DOUBLE_QUOTE = '"'
    private val MINUS = '-'
    private val PLUS = '+'
    private val ASTERISK = '*'
    private val QUESTION_MARK = '?'
    private val EXPLANATION_MARK = '!'
    private val BACKSLASH = '\\'
    private val AMPERSAND = '&'
    private val PIPE = '|'

    private val ESCAPED_CHARACTERS = HashSet(Arrays.asList(
            MINUS,
            PLUS,
            EXPLANATION_MARK,
            BACKSLASH,
            AMPERSAND,
            PIPE,
            '(', ')', '{', '}', '[', ']', ':', '^', '~'
    ))

    private val UNARY_OPERATORS = HashSet(Arrays.asList(
            MINUS,
            PLUS,
            EXPLANATION_MARK
    ))

    private val BINARY_OPERATORS = HashSet(Arrays.asList("&&", "||"))

    /**
     * Sanitize a potentially malformed query string so it conforms to the Lucene query syntax
     *
     * @param query
     * potentially malformed Lucene query string
     * @return
     * sanitized query string
     */
    fun sanitize(query: String): String {
        var sanitizedQuery = ""
        var sanitizedToken: String
        val tokens = tokenize(query)
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            if (isUnaryOperator(token)) {
                sanitizedToken = sanitizeUnaryOperator(token)
            } else if (isBinaryOperator(token)) {
                if (i == 0 || isBinaryOperator(tokens[i - 1]) || i >= tokens.size - 1 || isBinaryOperator(tokens[i + 1])) {
                    // Escape operator since operands missing
                    sanitizedToken = "" + BACKSLASH + token
                } else {
                    sanitizedToken = token
                }
            } else {
                sanitizedToken = enablePartialMatches(token, 0)
            }

            if (i != 0) {
                sanitizedQuery += " "
            }
            sanitizedQuery += sanitizedToken
            i++
        }
        logger.debug("Sanitized input '{}' to '{}'", query, sanitizedQuery)
        return sanitizedQuery
    }

    private fun isUnaryOperator(token: String): Boolean {
        return token.length > 0 && UNARY_OPERATORS.contains(token[0])
    }

    private fun isBinaryOperator(token: String): Boolean {
        return BINARY_OPERATORS.contains(token)
    }

    /**
     * Helper method to enable partial matching for string literals or operand
     *
     * @param string
     * token to be sanitized
     * @param begin
     * first character of operand, 0 for string literals
     * @return
     * the character found at specified position or ' ' if position not within string
     */
    private fun enablePartialMatches(string: String, begin: Int): String {
        var result = string

        var ch = string[begin]
        if (ch != DOUBLE_QUOTE && ch != ASTERISK) {
            result = ""
            if (begin > 0) {
                result += string.substring(0, begin)
            }
            result += ASTERISK
            result += string.substring(begin, string.length)
        }

        ch = result[result.length - 1]
        if (ch != DOUBLE_QUOTE && ch != ASTERISK) {
            result += ASTERISK
        }
        return result
    }

    /**
     * Helper method to sanitize unary operator tokens
     * This method performes the following sanitizitations:
     * - Escape unary operator in case of missing argument (ensure syntactical correctness)
     * - Enable partial matching for the operand
     *
     * @param token
     * token to be sanitized
     * @return
     * the character found at specified position or ' ' if position not within string
     */
    private fun sanitizeUnaryOperator(token: String): String {
        val sanitizedToken: String
        if (token.length == 1) {
            // Escape unary operator because of missing operand
            sanitizedToken = "" + BACKSLASH + token[0]
        } else {
            sanitizedToken = enablePartialMatches(token, 1)
        }
        return sanitizedToken
    }

    /**
     * Helper method to (pseudo)-tokenize a character sequence
     *
     * @param query
     * string to be tokenized
     * @return
     * list of tokens
     */
    private fun tokenize(query: String): ArrayList<String> {

        val tokens = ArrayList<String>()
        var currentToken = ""

        var openDoubleQuote = false
        var i = 0

        while (i < query.length) {

            val ch = query[i]

            if (ch == DOUBLE_QUOTE) {
                if (openDoubleQuote) {
                    currentToken += DOUBLE_QUOTE
                    tokens.add(currentToken)
                    currentToken = ""
                    openDoubleQuote = false
                } else if (currentToken.isEmpty() || isUnaryOperator("" + charAt(i - 1, query)) && Character.isWhitespace(charAt(i - 2, query))) {
                    currentToken += DOUBLE_QUOTE
                    openDoubleQuote = true
                } else {
                    // Escape double quote character to enforce whitespace separated tokens
                    currentToken += "" + BACKSLASH + DOUBLE_QUOTE
                }
            } else if (openDoubleQuote) {
                // No special handling of characters within quoted strings
                currentToken += ch
            } else if (isUnaryOperator("" + ch) && Character.isWhitespace(charAt(i - 1, query))) {
                // We only allow unary operators as first character of a token
                currentToken += ch
            } else if (isBinaryOperator("" + ch + charAt(i + 1, query))
                    && Character.isWhitespace(charAt(i - 1, query))
                    && Character.isWhitespace(charAt(i + 2, query))) {
                // Binary operator detected, i.e. whitespace delimited && or ||
                tokens.add("" + ch + ch)
                i++ // We nastily skip the binary operator, i.e. we are taken two characters in this round
            } else if (Character.isWhitespace(ch)) {
                // Whitespace delimits tokens
                if (!currentToken.isEmpty()) {
                    tokens.add(currentToken)
                    currentToken = ""
                }
            } else {
                if (ESCAPED_CHARACTERS.contains(ch)) {
                    currentToken += "" + BACKSLASH + ch
                } else {
                    currentToken += ch
                }
            }
            i++
        }
        if (!currentToken.isEmpty()) {
            if (openDoubleQuote) {
                // Syntax error detected. We fix this.
                currentToken += DOUBLE_QUOTE
            }
            tokens.add(currentToken)
        }

        return tokens
    }

    /**
     * Helper method to look up characters in strings without resulting in IndexOutOfBound exceptions
     *
     * @param position
     * position within string get the characters
     * @param string
     * the string we want to lookup a character
     * @return
     * the character found at specified position or ' ' if position not within string
     */
    private fun charAt(position: Int, string: String): Char {
        return if (0 <= position && position < string.length) {
            string[position]
        } else {
            ' '
        }
    }

}
