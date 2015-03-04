/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.message.broker.endpoint;

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.message.broker.impl.MessageSenderImpl;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "messagebrokerservice", title = "Message Broker Service", notes = "", abstractText = "Handles publishers and subscribers connecting to message brokers")
public class MessageBrokerServiceEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MessageBrokerServiceEndpoint.class);

  @POST
  @Path("sendTextMessage")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "testMessage", description = "Testing Message Sending", returnDescription = "OK", restParameters = {
          @RestParameter(name = "destinationId", description = "The unique id of the destination to send this message to", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "type", description = "The type of destination either queue or topic", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "message", description = "The content of the message.", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the event as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response testMessage(@FormParam("destinationId") String destinationId, @FormParam("type") String type, @FormParam("message") String message) throws Exception {
    DestinationType destinationType;
    if (type.equalsIgnoreCase(MessageSender.DestinationType.Queue.toString())) {
      destinationType = DestinationType.Queue;
    } else if (type.equalsIgnoreCase(MessageSender.DestinationType.Topic.toString())) {
      destinationType = DestinationType.Topic;
    } else {
      logger.error("The type of destination needs to be either {} or {} but was {}", new Object[] {
              DestinationType.Queue.toString(), DestinationType.Topic.toString(), type });
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(destinationId)) {
      logger.error("The destinationId cannot be blank!");
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(message)) {
      logger.error("The message cannot be blank!");
      return Response.status(Status.BAD_REQUEST).build();
    }

    MessageSenderImpl messageSenderImpl = new MessageSenderImpl();
    messageSenderImpl.sendTextMessage(destinationId, destinationType, message);
    return Response.ok().build();
  }

}
