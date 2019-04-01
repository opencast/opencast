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

package org.opencastproject.rest

import java.lang.String.format

import org.opencastproject.job.api.JobProducer
import org.opencastproject.serviceregistry.api.ServiceRegistry

/**
 * Refined implementation of [org.opencastproject.rest.AbstractJobProducerEndpoint] suitable for use in an
 * OSGi environment.
 *
 *
 * OSGi dependency injection methods are provided to reduce the amount of boilerplate code needed per
 * service implementation.
 *
 *
 * Declare as type variable the [job producing service][AbstractJobProducerEndpoint.getService] on which the
 * endpoint depends.
 *
 *
 * **Example:** The endpoint for the WorkflowService can be declared like this:
 * <pre>
 * public final class WorkflowServiceEndpoint extends OsgiAbstractJobProducerEndpoint&lt;WorkflowServiceImpl&gt;
</pre> *
 *
 *
 * **Implementation note:** Type variable `A` *cannot* have upper bound
 * [org.opencastproject.job.api.JobProducer]. Even though this may seem reasonable it will cause trouble
 * with OSGi dependency injection. The dependency will most likely be declared on the service's *interface*
 * and *not* the concrete implementation. But only the concrete implementation is a `JobProducer`.
 * With `A` having an upper bound of `JobProducer` the signature of [.setService]
 * will be fixed to that bound: `setService(JobProducer)`. Now the service cannot be injected anymore.
 */
abstract class OsgiAbstractJobProducerEndpoint<A> : AbstractJobProducerEndpoint() {
    var svc: A? = null
        private set
    override var serviceRegistry: ServiceRegistry? = null

    override val service: JobProducer?
        get() = if (svc is JobProducer) {
            svc as JobProducer?
        } else {
            throw RuntimeException(format("Service %s is expected to be of type JobProducer", svc))
        }

    fun setService(service: A) {
        this.svc = service
    }
}
