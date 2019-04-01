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


package org.opencastproject.mediapackage

/**
 * General exception that is raised when problems occur while manipulating media packages like adding or removing media
 * package elements, creating manifests or moving and copying the media package itself.
 */
class MediaPackageException : Exception {

    /**
     * Creates a new media package exception with the specified message.
     *
     * @param msg
     * the error message
     */
    constructor(msg: String) : super(msg) {}

    /**
     * Creates a new media package exception caused by Throwable `t`.
     *
     * @param t
     * the original exception
     */
    constructor(t: Throwable) : super(t.message, t) {}

    /**
     * Creates a new media package exception caused by Throwable `t`.
     *
     * @param msg
     * individual error message
     * @param t
     * the original exception
     */
    constructor(msg: String, t: Throwable) : super(msg, t) {}

    companion object {

        /** Serial version uid  */
        private val serialVersionUID = -1645569283274593366L
    }

}
