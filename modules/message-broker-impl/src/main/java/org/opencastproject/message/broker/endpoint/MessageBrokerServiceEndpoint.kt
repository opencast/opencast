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

package org.opencastproject.message.broker.endpoint

import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE

import org.opencastproject.message.broker.api.MessageReceiver
import org.opencastproject.message.broker.api.MessageSender
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "messagebrokerservice", title = "Message Broker Service", abstractText = "Handles publishers and subscribers connecting to message brokers", notes = [])
class MessageBrokerServiceEndpoint {

    private var messageReceiver: MessageReceiver? = null
    private var messageSender: MessageSender? = null

    val status: Response
        @GET
        @Path("status")
        @RestQuery(name = "status", description = "Return status of message broker", returnDescription = "Return status of message broker", reponses = [RestResponse(responseCode = SC_NO_CONTENT, description = "Connection to message broker ok"), RestResponse(responseCode = SC_SERVICE_UNAVAILABLE, description = "Not connected to message broker")])
        get() = if (messageReceiver!!.isConnected && messageSender!!.isConnected) {
            Response.status(SC_NO_CONTENT).build()
        } else Response.status(SC_SERVICE_UNAVAILABLE).build()

    fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    fun setMessageSender(messageSender: MessageSender) {
        this.messageSender = messageSender
    }

}
