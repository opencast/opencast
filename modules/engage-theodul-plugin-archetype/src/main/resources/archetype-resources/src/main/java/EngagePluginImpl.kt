import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import org.opencastproject.engage.theodul.api.AbstractEngagePlugin
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
class EngagePluginImpl : AbstractEngagePlugin() {

    protected fun activate(cc: ComponentContext) {
        log.info("Activated Theodul plugin: \${plugin_name}")
    }

    @GET
    @Path("sayhello")
    @Produces(MediaType.TEXT_PLAIN)
    fun sayHello(): String {
        return "This is the \${plugin_name} plugin!"
    }

    companion object {

        private val log = LoggerFactory.getLogger(EngagePluginImpl::class.java)
    }
}
