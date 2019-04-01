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

/**
 * This exception is thrown during text extraction.
 */
class TextExtractorException : Exception {

    /**
     * Creates a new text extration exception with `message` as a reason.
     *
     * @param message
     * the reason of failure
     */
    constructor(message: String) : super(message) {}

    /**
     * Creates a new text extration exception where `cause` identifies the original reason of failure.
     *
     * @param cause
     * the root cause for the failure
     */
    constructor(cause: Throwable) : super(cause) {}

    /**
     * Creates a new text extration exception with `message` as a reason and `cause` as the original
     * cause of failure.
     *
     * @param message
     * the reason of failure
     * @param cause
     * the root cause for the failure
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}

    companion object {

        /** The serial version ui  */
        private val serialVersionUID = 8647839276281407394L
    }

}
