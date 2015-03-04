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
package org.opencastproject.adminui.util;

import org.opencastproject.adminui.exception.IllegalLanguageFilenameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A utility class which provides methods related to translation files, it
 * operates on its {@link #LANGUAGE_PATTERN}.
 * 
 * @author ademasi
 * 
 */
public final class LanguageFileUtil {

    private LanguageFileUtil() {
        // No default constructor for utility class.
    }

    /**
     * The convention used when naming translated json files.
     */
    public static final String LANGUAGE_PATTERN = "^lang-(([a-z])+[_[A-Z]+]?).*.json$";

    private static final Logger logger = LoggerFactory
            .getLogger(LanguageFileUtil.class);

    /**
     * Executes {@link #stripLanguageFromFilename(String)} on each of the
     * provided <code>filenames</code>.
     * 
     * @param filenames
     * @return
     */
    public static List<String> extractLanguagenamesFromFilenames(
            List<String> filenames) {
        List<String> result = new ArrayList<String>();
        for (String filename : filenames) {
            try {
                result.add(stripLanguageFromFilename(filename));
            } catch (IllegalLanguageFilenameException e) {
                logger.warn(
                        "There is an illegal language filename lurking around. Excluding it from the available languages list.",
                        e);
            }
        }
        return result;
    }

    /**
     * Finds the language substring in a translation file (e.g. lang-de_DE.json
     * will result in de_DE). Is gracefule for non-language-file strings, such
     * as de or de_DE.
     * 
     * @param filename
     *            a language code according to the patterns lang-xy_AB.json,
     *            yy_XX, yy-QQ, yy.
     * @return The language substring, e.g. de_DE.
     * @throws IllegalLanguageFilenameException
     *             if none of the compliant patterns are met by filename
     */
    public static String stripLanguageFromFilename(String filename)
            throws IllegalLanguageFilenameException {
        String result = null;
        if (filename.matches("[a-z]{1,2}")) {
            result = filename;
        } else if (filename.matches(LANGUAGE_PATTERN)) {
            result = filename.replaceAll("lang-", "").replaceAll(".json", "");
        } else if (filename.matches(CompositeLanguageCodeParser.COMPOSITE_LANGUAGE_NAME) && filename.length() < 4) {
            result = filename;
        }
        if (result != null) {
            return result;
        }
        throw new IllegalLanguageFilenameException(
                String.format(
                        "The filename %s does not comply with the expected pattern lang-xy_AB.json",
                        filename));
    }

    public static String safelyStripLanguageFromFilename(String filename) {
        try {
            return stripLanguageFromFilename(filename);
        } catch (IllegalLanguageFilenameException e) {
            logger.warn(
                    "Could not strip the language name from the filename {}. This indicates that the filename on the server is not compliant with the naming convention.",
                    filename, e);
            return filename;
        }
    }

    /**
     * Finds the part before the - or _ of a composited language code like de_DE
     * (as it is returned by {@link #stripLanguageFromFilename(String)} or
     * {@link #safelyStripLanguageFromFilename(String)}. If the languageCode is
     * not composited, it will be returned as is.
     * 
     * @param compositedLanguageCode
     * @return The ISO part of a composited language code, if not composited,
     *         the languageCode.
     */
    public static String getIsoLanguagePart(String languageCode) {
        CompositeLanguageCodeParser parser = new CompositeLanguageCodeParser(
                languageCode);
        if (parser.isComposite()) {
            return parser.getSimpleLanguage();
        } else {
            return languageCode;
        }
    }

    /**
     * Returns the displayName from the Locale that belongs to the language
     * code.
     * 
     * @param languageCode
     * @return
     */
    public static String getDisplayLanguageFromLanguageCode(String languageCode) {
        CompositeLanguageCodeParser parser = new CompositeLanguageCodeParser(
                languageCode);
        if (parser.isComposite()) {
            return new Locale(parser.getSimpleLanguage()).getDisplayLanguage();
        }
        return new Locale(languageCode).getDisplayLanguage();
    }

}
