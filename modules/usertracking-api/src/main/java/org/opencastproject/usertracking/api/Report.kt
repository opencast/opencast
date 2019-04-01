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

package org.opencastproject.usertracking.api

import java.util.Calendar

/**
 * A class that represents a report
 *
 */
interface Report {

    /**
     * Gets the date the data of the report starts from
     *
     * @return
     */
    /**
     * Sets the the date the data of the report starts from
     *
     * @param from
     */
    var from: Calendar

    /**
     * Gets the date the data of the report is referring to
     *
     * @return
     */
    /**
     * Sets the date the data of the report is referring to
     *
     * @param to
     */
    var to: Calendar

    /**
     * Gets the sum of views of all report items
     *
     * @return
     */
    /**
     * Sets the sum of views of all report items
     *
     * @param views
     */
    var views: Int

    /**
     * Gets the sum of the number of played seconds of all report items
     *
     * @return
     */
    /**
     * Sets the sum of the number of played seconds of all report items
     *
     * @param played
     */
    var played: Long

    /**
     * Gets the total of report items
     *
     * @return
     */
    /**
     * Sets the total of report items
     *
     * @param total
     */
    var total: Int

    /**
     * Gets the maximum number of report items of the report (used for paging)
     *
     * @return
     */
    /**
     * Sets the maximum number of report items of the report (used for paging)
     *
     * @param limit
     */
    var limit: Int

    /**
     * Gets the number of the report item to start with in the list of all report items (used for paging)
     *
     * @return
     */
    /**
     * Sets the number of the report item to start with in the list of all report items (used for paging)
     *
     * @param offset
     */
    var offset: Int

    /**
     * Add an report item to the report
     *
     * @param reportItem
     */
    fun add(reportItem: ReportItem)

}
