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

package org.opencastproject.textextractor.api

import java.awt.Rectangle

interface TextLine {

    /**
     * Returns the text.
     *
     * @return the text
     */
    val text: String

    /**
     * Returns the text's bounding box, if one exists. Note that the box was calculated from the line of text that
     * contained this text, so while the vertical position as well as the height will be ok, the box will most probably be
     * much wider than this single text.
     *
     * @return the boundaries
     */
    val boundaries: Rectangle

}
