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

package org.opencastproject.message.broker.api.index

import java.io.Serializable

class IndexRecreateObject : Serializable {

    var indexName: String? = null
        private set
    val message: String
    var total: Int = 0
        private set
    var current: Int = 0
        private set
    var status: Status? = null
        private set
    var service: Service? = null
        private set

    enum class Status {
        Start, Update, End, Error
    }

    /**
     * New services may be added in arbitrary order since the elements are identified by name, not their ordinal.
     */
    enum class Service {
        Acl, AssetManager, Comments, Groups, Scheduler, Series, Themes, Workflow
    }

    /**
     * Constructor for a start or stop message.
     *
     * @param indexName
     * The index name
     * @param service
     * The service this message relates to.
     * @param status
     * The status of the message.
     */
    private constructor(indexName: String, service: Service, status: Status) {
        this.indexName = indexName
        this.service = service
        this.status = status
    }

    /**
     * Constructor for an update message.
     *
     * @param indexName
     * The index name
     * @param service
     * The service that has been updated.
     * @param total
     * The total number of objects that will be re-added.
     * @param current
     * The current number that have been re-added.
     */
    private constructor(indexName: String, service: Service, total: Int, current: Int) {
        this.indexName = indexName
        this.service = service
        this.status = Status.Update
        this.total = total
        this.current = current
    }

    /**
     * The constructor for an error message.
     *
     * @param indexName
     * The index name
     * @param service
     * The service that has had the error.
     * @param total
     * The total number of objects that were supposed to be added.
     * @param current
     * The current number of objects added before the error.
     * @param message
     * The error message about the problem.
     */
    private constructor(indexName: String, service: Service, total: Int, current: Int, message: String) {
        this.indexName = indexName
        this.service = service
        this.status = Status.Error
        this.total = total
        this.current = current
        this.message = message
    }

    companion object {

        private const val serialVersionUID = 6076737478411640536L

        fun start(indexName: String, service: Service): IndexRecreateObject {
            return IndexRecreateObject(indexName, service, Status.Start)
        }

        fun update(indexName: String, service: Service, total: Int, current: Int): IndexRecreateObject {
            return IndexRecreateObject(indexName, service, total, current)
        }

        fun end(indexName: String, service: Service): IndexRecreateObject {
            return IndexRecreateObject(indexName, service, Status.End)
        }

        fun error(indexName: String, service: Service, total: Int, current: Int, message: String): IndexRecreateObject {
            return IndexRecreateObject(indexName, service, total, current)
        }
    }
}
