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

package org.opencastproject.dataloader

import org.opencastproject.util.data.Option

import org.apache.commons.lang3.StringUtils

import java.util.ArrayList
import java.util.Date

class EventEntry(val title: String, val recordingDate: Date, val duration: Int, val isArchive: Boolean, series: String,
                 val captureAgent: String, val source: String, val contributor: String, description: String, seriesName: String,
                 presenters: List<String>) {
    val series: Option<String>
    val description: Option<String>
    val seriesName: Option<String>
    private val presenters = ArrayList<String>()

    init {
        this.seriesName = Option.option(StringUtils.trimToNull(seriesName))
        this.series = Option.option(series)
        this.description = Option.option(StringUtils.trimToNull(description))
        this.presenters.addAll(presenters)
    }

    fun getPresenters(): List<String> {
        return presenters
    }

}
