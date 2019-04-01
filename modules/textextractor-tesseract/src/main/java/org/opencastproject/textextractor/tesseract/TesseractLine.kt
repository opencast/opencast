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

package org.opencastproject.textextractor.tesseract

import org.opencastproject.textextractor.api.TextLine

import java.awt.Rectangle

/**
 * Representation of a line of text extracted from an image.
 */
class TesseractLine
/**
 * Creates a representation for a piece of text
 *
 * @param line
 * the extracted text
 */
(line: String) : TextLine {

    /** The text  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.textextractor.api.TextLine.getText
     */
    override var text: String? = null
        protected set(value: String?) {
            super.text = value
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.textextractor.api.TextLine.getBoundaries
     */
    override val boundaries: Rectangle
        get() = null

    init {
        this.text = line
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String? {
        return text
    }

}
