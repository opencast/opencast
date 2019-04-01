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
package org.opencastproject.assetmanager.api.query

import com.entwinemedia.fn.Stream

/**
 * The result of a [ASelectQuery]. Groups [ARecord]s.
 */
interface AResult : Iterable<ARecord> {
    /** Return the found records.  */
    val records: Stream<ARecord>

    /**
     * Return the size of the retrieved [slice][.getRecords].
     * This value is &lt;= [.getLimit].
     *
     * @see .getLimit
     */
    val size: Long

    /** Return a string representation of the query.  */
    val query: String

    /**
     * Return the number of items the query could potentially yield.
     *
     * @return the total value or -1 to indicate that the value has not been calculated
     */
    val totalSize: Long

    /**
     * Return the set's size limit. It reflects the requested page size if specified in the query.
     * This value is &gt;= [.getSize].
     *
     * @return the requested size limit or -1 if none has been specified
     * @see .getSize
     */
    val limit: Long

    /** Return the offset within the total result set.  */
    val offset: Long

    /**
     * Return the search time of the query.
     *
     * @return the search time or -1 to indicate that it has not been measured
     */
    val searchTime: Long
}
