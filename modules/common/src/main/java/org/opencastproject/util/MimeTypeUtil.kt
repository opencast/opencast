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

package org.opencastproject.util

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

/** Utility functions for mime types.  */
object MimeTypeUtil {

    /** [org.opencastproject.util.MimeType.getSuffix] as a function.  */
    val suffix: Function<MimeType, Option<String>> = object : Function<MimeType, Option<String>>() {
        override fun apply(mimeType: MimeType): Option<String> {
            return mimeType.suffix
        }
    }

    /** [org.opencastproject.util.MimeType.toString] as a function.  */
    val toString: Function<MimeType, String> = object : Function<MimeType, String>() {
        override fun apply(mimeType: MimeType): String {
            return mimeType.toString()
        }
    }

    object Fns {

        /** [org.opencastproject.util.MimeType.getSuffix] as a function.  */
        val suffix: Fn<MimeType, Opt<String>> = object : Fn<MimeType, Opt<String>>() {
            override fun apply(mimeType: MimeType): Opt<String> {
                return mimeType.suffix.toOpt()
            }
        }

        /** [org.opencastproject.util.MimeType.toString] as a function.  */
        val toString: Fn<MimeType, String> = object : Fn<MimeType, String>() {
            override fun apply(mimeType: MimeType): String {
                return mimeType.toString()
            }
        }
    }
}
