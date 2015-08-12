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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.RestUtil.splitCommaSeparatedParam;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.adminui.util.ParticipationUtils;
import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.EmailConfiguration;
import org.opencastproject.messages.Mail;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.TemplateMessageQuery;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.messages.TemplateType.Type;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.EmailSender;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Val;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "emailservice", title = "Email Service", notes = "", abstractText = "Provides email operations")
public class EmailEndpoint {
  /** The course name to use for the demo template. */
  private static final String DEFAULT_DEMO_COURSE_NAME = "Demo Course Name";
  /** The course description to use for the demo template. */
  private static final String DEFAULT_DEMO_COURSE_DESCRIPTION = "Demo Course Description";
  /** The default number of demo recordings to show in a demo template. */
  private static final int DEMO_RECORDING_NUMBER = 5;

  /** The key for the name of the template variable. */
  private static final String NAME_KEY = "NAME";
  /** The key for the template variable's description in English. */
  private static final String DESCRIPTION_EN_KEY = "DESCRIPTION.EN";
  /** The key for the template variable. */
  private static final String VARIABLE_KEY = "VARIABLE";
  /** Keys for the template variables. */
  /** The template variable to put the module / course description into an email. */
  private static final String MODULE_DESCRIPTION_KEY = "MODULE.DESCRIPTION";
  /** The template variable to put the module / course name into an email. */
  private static final String MODULE_NAME_KEY = "MODULE.NAME";
  /** The template variable to put the link to opt out of recording into an email. */
  private static final String OPT_OUT_LINK_KEY = "OPT.OUT.LINK";
  /** The template variable to put the staff's name into an email. */
  private static final String STAFF_NAME_KEY = "STAFF.NAME";

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(EmailEndpoint.class);

  /** The participation management email sender */
  private EmailSender emailSender;

  /** The mail service */
  private MailService mailService;

  /** The security service */
  private SecurityService securityService;

  /** The participation management database */
  private ParticipationManagementDatabase participationDatabase;

  /** The scheduler service */
  private SchedulerService schedulerService;

  /** A parser for handling JSON documents inside the body of a request. **/
  private JSONParser parser = new JSONParser();

  /** OSGi callback for the mail service. */
  public void setMailService(MailService mailService) {
    this.mailService = mailService;
  }

  /** OSGi callback for the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for the participation database. */
  public void setParticipationDatabase(ParticipationManagementDatabase participationDatabase) {
    this.participationDatabase = participationDatabase;
  }

  /** OSGi callback for the participation email sender. */
  public void setEmailSender(EmailSender emailSender) {
    this.emailSender = emailSender;
  }

  /** OSGi callback for the scheduler service. */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  protected void activate(ComponentContext cc) {
    logger.info("Activated email service endpoint");
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("variables.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEmailTemplateVariables", description = "Returns a list of email template variables as JSON", returnDescription = "The email template variables as a JSON list.", restParameters = {}, reponses = { @RestResponse(description = "Returns the email template variables list as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getEmailTemplateVariables() throws UnauthorizedException {
    JSONArray array = new JSONArray();
    JSONObject optout = new JSONObject();
    optout.put(NAME_KEY, OPT_OUT_LINK_KEY);
    optout.put(VARIABLE_KEY, "${optOutLink}");
    optout.put(DESCRIPTION_EN_KEY, "Inserts a link for instructors to opt out of having their lectures recorded.");
    array.add(optout);

    JSONObject staff = new JSONObject();
    staff.put(NAME_KEY, STAFF_NAME_KEY);
    staff.put(VARIABLE_KEY, "${staff}");
    staff.put(DESCRIPTION_EN_KEY, "Inserts the staff member's name.");
    array.add(staff);

    JSONObject moduleName = new JSONObject();
    moduleName.put(NAME_KEY, MODULE_NAME_KEY);
    moduleName.put(VARIABLE_KEY, "<#list modules as module>${module.name}</#list>");
    moduleName.put(DESCRIPTION_EN_KEY,
            "Inserts the list of module names the staff member is involved in into the email.");
    array.add(moduleName);

    JSONObject moduleDescription = new JSONObject();
    moduleDescription.put(NAME_KEY, MODULE_DESCRIPTION_KEY);
    moduleDescription.put(VARIABLE_KEY, "<#list modules as module>${module.description}</#list>");
    moduleDescription.put(DESCRIPTION_EN_KEY, "Inserts the list of module descriptions into the email.");
    array.add(moduleDescription);

    return Response.ok(array.toJSONString()).build();
  }

  @GET
  @Path("templates.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getemailtemplates", description = "Returns a list of email templates as JSON", returnDescription = "The email template list as JSON", restParameters = { @RestParameter(name = "onlymine", description = "Whether to return only my email templates", isRequired = false, defaultValue = "true", type = RestParameter.Type.BOOLEAN) }, reponses = {
          @RestResponse(description = "Returns the email template list as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Not authorized to get all email templates!", responseCode = HttpServletResponse.SC_UNAUTHORIZED) })
  public Response getEmailTemplates(@QueryParam("limit") int limit, @QueryParam("offset") int offset,
          @QueryParam("onlymine") @DefaultValue("true") boolean onlymine) throws UnauthorizedException {
    TemplateMessageQuery messageQuery = new TemplateMessageQuery();

    if (onlymine) {
      messageQuery.withCreator(securityService.getUser().getUsername());
    } else {
      // Check for the global and local admin role
      if (!(securityService.getUser().hasRole(GLOBAL_ADMIN_ROLE) || securityService.getUser().hasRole(
              securityService.getOrganization().getAdminRole())))
        throw new UnauthorizedException("Not authorized to get all email tempaltes!");
    }

    try {
      List<MessageTemplate> messageTemplates = mailService.findMessageTemplates(messageQuery);
      int total = messageTemplates.size();

      // Apply Limit and offset
      messageTemplates = new SmartIterator<MessageTemplate>(limit, offset).applyLimitAndOffset(messageTemplates);

      List<JValue> templateJSON = new ArrayList<JValue>();
      for (MessageTemplate template : messageTemplates) {
        templateJSON.add(template.toJValue());
      }
      return okJsonList(templateJSON, offset, limit, total);
    } catch (Exception e) {
      logger.error("Could not retrieve the email templates: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("template/{templateId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getemailtemplate", description = "Returns the email template by the given id as JSON", returnDescription = "The email template as JSON", pathParameters = { @RestParameter(name = "templateId", description = "The template id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Returns the email template as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The template has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEmailTemplate(@PathParam("templateId") long templateId) throws NotFoundException {
    try {
      MessageTemplate messageTemplate = mailService.getMessageTemplate(templateId);
      return Response.ok(messageTemplate.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve the email template {}: {}", templateId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("template")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createemailtemplate", description = "Returns the created email template as JSON", returnDescription = "The created email template as JSON", restParameters = {
          @RestParameter(name = "name", description = "The template name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "type", description = "The template type", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "subject", description = "The template subject", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "body", description = "The template body", isRequired = true, type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns the created email template as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Template type wrong", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createEmailTemplate(@FormParam("name") String name, @FormParam("type") String typeString,
          @FormParam("subject") String subject, @FormParam("body") String body) throws NotFoundException {
    Type type = TemplateType.INVITATION.getType();
    try {
      type = TemplateType.Type.valueOf(typeString);
    } catch (Exception e) {
      logger.warn("Unable to parse template type {}", typeString);
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    User currentUser = securityService.getUser();
    MessageTemplate template = new MessageTemplate(name, currentUser, subject, body, type.getType(), new Date(),
            nil(Comment.class));
    try {
      MessageTemplate messageTemplate = mailService.updateMessageTemplate(template);
      return Response.ok(messageTemplate.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Could not create the email template: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Creates a given number of recordings to display in a demo email template.
   *
   * @param numberOfRecordings
   *          The number of recordings to create.
   * @param currentPerson
   *          The current person to add as a staff person to these recordings.
   * @return A Collection of Recording objects to use in a demo template.
   */
  private Collection<Recording> getExampleRecordings(int numberOf, Person currentPerson) {
    List<PersonType> personTypes = new ArrayList<PersonType>();
    Collection<Recording> exampleRecordings = new ArrayList<Recording>();
    for (int i = 0; i < numberOf; i++) {
      ArrayList<Person> staff = new ArrayList<Person>();
      Course demoCourse = new Course("Demo-Course-" + i, DEFAULT_DEMO_COURSE_NAME + " " + i,
              DEFAULT_DEMO_COURSE_DESCRIPTION + " " + i);
      // Necessary to get all of the demo courses rendered as it is looking for unique ids.
      demoCourse.setId(new Long(i));
      Room room = new Room("Example Room Name " + i);
      staff.add(new Person(null, currentPerson.getName(), currentPerson.getEmail(), personTypes));
      List<Person> participants = new ArrayList<Person>();
      CaptureAgent captureAgent = new CaptureAgent(room, "Demo Capture Agent " + i);
      Recording recording = Recording.recording(null, "Demo Recording " + i, staff, demoCourse, room, new Date(),
              new Date(), new Date(new Date().getTime() + 10000), participants, captureAgent);
      exampleRecordings.add(recording);
    }
    return exampleRecordings;
  }

  /**
   * Create a JSON Obj with all of the variables substituted with example data such as course names, descriptions etc.
   *
   * @param currentUser
   *          The current user to pull their data such as name and email.
   * @param subject
   *          The subject for the email.
   * @param body
   *          The content for the email template.
   * @return A JSON object with the subject and body with all of the variables substituted.
   */
  protected Obj getExampleBody(User currentUser, String subject, String body) {
    if (mailService == null) {
      return null;
    }
    List<PersonType> personTypes = new ArrayList<PersonType>();
    String currentName = currentUser.getName();
    if (currentName == null) {
      currentName = "Staff Name";
    }
    String currentEmail = currentUser.getEmail();
    if (currentEmail == null) {
      currentEmail = "staffemail@fake.com";
    }
    Person currentPerson = new Person(null, currentName, currentEmail, personTypes);
    Collection<Recording> exampleRecordings = getExampleRecordings(DEMO_RECORDING_NUMBER, currentPerson);
    MessageTemplate msgTemplate = new MessageTemplate("Demo Template", currentUser, subject, body);
    EmailAddress sender = new EmailAddress("sender@fake.com", "Example Sender");
    EmailAddress replyTo = new EmailAddress("reply@fake.com", "Example Reply");
    List<Comment> comments = new ArrayList<Comment>();
    MessageSignature signature = new MessageSignature(null, "Example Signature", currentUser, sender,
            Option.some(replyTo), "Example Signature Text", new Date(), comments);
    Message message = new Message(currentPerson, msgTemplate, signature);
    String exampleBody = emailSender.renderInvitationBody(message, exampleRecordings, currentPerson);
    return Jsons.obj(Jsons.p("body", exampleBody), Jsons.p("subject", subject));
  }

  @POST
  @Path("demotemplate")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getDemoEmailTemplate", description = "Returns an email template with variables filled in with demo data as JSON", pathParameters = {}, restParameters = { @RestParameter(name = "templateContent", description = "A JSON object containing the body and subject of an email template.", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the email template with demo data filled in, presented in JSON format.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Badly formed json for the body or template. ", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "The example email template as JSON")
  public Response getDemoEmailTemplate(String templateContent) {
    if (StringUtils.isBlank(templateContent)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    JSONObject decodedTemplate = null;
    try {
      decodedTemplate = (JSONObject) parser.parse(templateContent);
    } catch (ParseException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    String body = (String) decodedTemplate.get("body");
    String subject = (String) decodedTemplate.get("subject");

    Obj demoTemplate = getExampleBody(securityService.getUser(), subject, body);
    if (demoTemplate == null) {
      return Response.serverError().build();
    }
    return Response.ok(demoTemplate.toJson()).build();
  }

  @PUT
  @Path("template/{templateId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateemailtemplate", description = "Returns the updated email template by the given id as JSON", returnDescription = "The updated email template as JSON", pathParameters = { @RestParameter(name = "templateId", description = "The template id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "name", description = "The template name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "type", description = "The template type", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "subject", description = "The template subject", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "body", description = "The template body", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the email template as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Template type wrong", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "The template has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEmailTemplate(@PathParam("templateId") long templateId, @FormParam("name") String name,
          @FormParam("type") String typeString, @FormParam("subject") String subject, @FormParam("body") String body)
          throws NotFoundException {
    Type type = TemplateType.INVITATION.getType();
    try {
      type = TemplateType.Type.valueOf(typeString);
    } catch (Exception e) {
      logger.warn("Unable to parse template type {}", typeString);
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      MessageTemplate template = mailService.getMessageTemplate(templateId);
      template.setName(name);
      template.setType(type.getType());
      template.setSubject(subject);
      template.setBody(body);

      MessageTemplate messageTemplate = mailService.updateMessageTemplate(template);
      return Response.ok(messageTemplate.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update the email template {}: {}", templateId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("template/{templateId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteemailtemplate", description = "Deletes the email template by the given id", returnDescription = "No content", pathParameters = { @RestParameter(name = "templateId", description = "The template id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Email template has been deleted", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The template has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEmailTemplate(@PathParam("templateId") long templateId) throws NotFoundException {
    try {
      mailService.deleteMessageTemplate(templateId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete the email template {}: {}", templateId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("deleteTemplates")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteemailtemplates", description = "Deletes a json list of email templates by their given ids e.g. [8, 9, 10, 11]", returnDescription = "Ok with lists of successfully deleted templates, ones there were not found and ones that there were errors deleting.", reponses = {
          @RestResponse(description = "Email templates have been deleted or will be in the not found or error category.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The template has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEmailTemplate(String templateIdsContent) throws NotFoundException {
    if (StringUtils.isBlank(templateIdsContent)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    JSONArray templateIdJsonArray;
    try {
      templateIdJsonArray = (JSONArray) parser.parse(templateIdsContent);
    } catch (ParseException e) {
      logger.error("Unable to parse '{}' because: {}", templateIdsContent, ExceptionUtils.getStackTrace(e));
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (ClassCastException e) {
      logger.error("Unable to cast '{}' because: {}", templateIdsContent, ExceptionUtils.getStackTrace(e));
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    BulkOperationResult result = new BulkOperationResult();
    ArrayList<Long> templateIds = new ArrayList<Long>();
    for (Object templateIdObject : templateIdJsonArray) {
      try {
        Long currentId = Long.parseLong(templateIdObject.toString());
        mailService.getMessageTemplate(currentId);
        templateIds.add(currentId);
      } catch (NumberFormatException e) {
        logger.error("Unable to parse template id '{}' because: {}", templateIdObject, ExceptionUtils.getStackTrace(e));
        result.addServerError(templateIdObject.toString());
      } catch (MailServiceException e) {
        logger.error("Unable to find template with id '{}' because: {}", templateIdObject,
                ExceptionUtils.getStackTrace(e));
        result.addServerError(templateIdObject.toString());
      } catch (NotFoundException e) {
        logger.warn("Unable to find template with id '{}'", templateIdObject);
        result.addNotFound(templateIdObject.toString());
      }
    }

    for (Long templateId : templateIds) {
      try {
        mailService.deleteMessageTemplate(templateId);
        result.addOk(templateId);
      } catch (NotFoundException e) {
        logger.warn("Unable to delete template {}", templateId);
        result.addNotFound(templateId);
      } catch (Exception e) {
        logger.error("Could not delete the email template '{}': {}", templateId, ExceptionUtils.getStackTrace(e));
        result.addServerError(templateId);
      }
    }
    return Response.ok(result.toJson()).build();
  }

  @GET
  @Path("signatures.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getemailsignatures", description = "Returns a list of email signatures as JSON", returnDescription = "The email signature list as JSON", reponses = { @RestResponse(description = "Returns the email signature list as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getEmailSignatures() {
    try {
      List<MessageSignature> messageSignatures = mailService.getMessageSignatures();

      List<Val> signatures = new ArrayList<Val>();
      for (MessageSignature signature : messageSignatures) {
        signatures.add(signature.toJson());
      }

      return Response.ok(Jsons.arr(signatures).toJson()).build();
    } catch (Exception e) {
      logger.error("Could not retrieve the email signatures: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("signature/{signatureId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getemailsignature", description = "Returns the email signature by the given id as JSON", returnDescription = "The email signature as JSON", pathParameters = { @RestParameter(name = "signatureId", description = "The signature id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Returns the email signature as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The signature has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEmailSignature(@PathParam("signatureId") long signatureId) throws NotFoundException {
    try {
      MessageSignature messageSignature = mailService.getMessageSignature(signatureId);
      return Response.ok(messageSignature.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve the email signature {}: {}", signatureId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("signature")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createemailsignature", description = "Returns the created email signature as JSON", returnDescription = "The created email signature as JSON", restParameters = {
          @RestParameter(name = "name", description = "The signature name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_name", description = "The sender's name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_address", description = "The sender's address", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_name", description = "The reply name", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_address", description = "The reply address", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "text", description = "The signature text", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Bad request if the user already has a signature.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "Returns the created email template as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response createEmailSignature(@FormParam("name") String name, @FormParam("from_name") String emailName,
          @FormParam("from_address") String address, @FormParam("reply_name") String replyName,
          @FormParam("reply_address") String replyAddress, @FormParam("text") String text) throws NotFoundException {
    User currentUser = securityService.getUser();
    try {
      if (mailService.getSignatureTotalByUserName() > 0) {
        return badRequest("Unable to create another signature as a user can't have more than 1 signature");
      }
    } catch (Exception e) {
      logger.error("Couldn't get the number of signatures for a user: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    Option<EmailAddress> reply = Option.<EmailAddress> none();
    if (StringUtils.isNotBlank(replyAddress) && StringUtils.isNotBlank(replyName))
      reply = Option.some(EmailAddress.emailAddress(replyAddress, replyName));

    EmailAddress emailAddress = EmailAddress.emailAddress(address, emailName);
    MessageSignature messageSignature = new MessageSignature(null, name, currentUser, emailAddress, reply, text,
            new Date(), nil(Comment.class));
    try {
      MessageSignature updatedMessageSignature = mailService.updateMessageSignature(messageSignature);
      return Response.ok(updatedMessageSignature.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Could not create the email signature: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("signature/{signatureId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateemailsignature", description = "Returns the updated email signature by the given id as JSON", returnDescription = "The updated email signature as JSON", pathParameters = { @RestParameter(name = "signatureId", description = "The signature id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "name", description = "The signature name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_name", description = "The signature type", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_address", description = "The sender's address", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_name", description = "The reply name", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_address", description = "The reply address", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "text", description = "The signature text", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the email signature as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The signature has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEmailSignature(@PathParam("signatureId") long signatureId, @FormParam("name") String name,
          @FormParam("from_name") String emailName, @FormParam("from_address") String address,
          @FormParam("reply_name") String replyName, @FormParam("reply_address") String replyAddress,
          @FormParam("text") String text) throws NotFoundException {

    Option<EmailAddress> reply = Option.<EmailAddress> none();
    if (StringUtils.isNotBlank(replyAddress) && StringUtils.isNotBlank(replyName))
      reply = Option.some(EmailAddress.emailAddress(replyAddress, replyName));

    try {
      MessageSignature messageSignature = mailService.getMessageSignature(signatureId);
      messageSignature.setName(name);
      messageSignature.setSender(EmailAddress.emailAddress(address, emailName));
      messageSignature.setReplyTo(reply);
      messageSignature.setSignature(text);

      MessageSignature updatedMessageSignature = mailService.updateMessageSignature(messageSignature);
      return Response.ok(updatedMessageSignature.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update the email signature {}: {}", signatureId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("signature/{signatureId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "deleteemailtemplate", description = "Deletes the email signature by the given id", returnDescription = "No content", pathParameters = { @RestParameter(name = "signatureId", description = "The signature id", isRequired = true, type = RestParameter.Type.INTEGER) }, reponses = {
          @RestResponse(description = "Email signature has been deleted", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The email signature has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteEmailSignature(@PathParam("signatureId") long signatureId) throws NotFoundException {
    try {
      mailService.deleteMessageSignature(signatureId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete the email signature {}: {}", signatureId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getemailconfiguration", description = "Returns the email configuration as JSON", returnDescription = "The email configuration as JSON", reponses = { @RestResponse(description = "Returns the email configuration as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response getEmailConfiguration() throws NotFoundException {
    try {
      EmailConfiguration emailConfiguration = mailService.getEmailConfiguration();
      return Response.ok(emailConfiguration.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Could not retrieve the email configuration: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateemailconfiguration", description = "Updates the email configuration", returnDescription = "The email configuration as JSON", restParameters = {
          @RestParameter(name = "transport", description = "The mail transport", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "server", description = "The server hostname", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "port", description = "The server port address", isRequired = true, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "username", description = "The username", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "password", description = "The password", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "ssl", description = "Whether SSL is used", isRequired = true, type = RestParameter.Type.BOOLEAN) }, reponses = { @RestResponse(description = "Returns the updated blacklist as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response updateEmailConfiguration(@FormParam("transport") String transport,
          @FormParam("server") String server, @FormParam("port") int port, @FormParam("username") String username,
          @FormParam("password") String password, @FormParam("ssl") boolean ssl) {
    try {
      EmailConfiguration emailConfiguration = new EmailConfiguration(transport, server, port, username, password, ssl);
      mailService.updateEmailConfiguration(emailConfiguration);
      return Response.ok(emailConfiguration.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Could not update the email configuration: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("preview/{templateId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "previewmail", description = "Render a mail with the current email configuration for reviewing it", returnDescription = "The renderend message as JSON", pathParameters = { @RestParameter(name = "templateId", description = "The template id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "eventIds", description = "A comma separated list of event ids", isRequired = true, type = RestParameter.Type.TEXT),
          @RestParameter(name = "personIds", description = "A comma separated list of person ids", isRequired = true, type = RestParameter.Type.TEXT),
          @RestParameter(name = "signature", description = "Whether to include the signature", isRequired = false, type = RestParameter.Type.BOOLEAN),
          @RestParameter(name = "body", description = "The optional message body", isRequired = false, type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "No recording or person ids have been set!", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No message signature is configured or no PM user to perform this action!", responseCode = HttpServletResponse.SC_PRECONDITION_FAILED),
          @RestResponse(description = "Email has been sent", responseCode = HttpServletResponse.SC_NO_CONTENT) })
  public Response previewMail(@PathParam("templateId") long template, @FormParam("eventIds") String eventIdsString,
          @FormParam("personIds") String personIdsString,
          @FormParam("signature") @DefaultValue("false") boolean signature, @FormParam("subject") String subject,
          @FormParam("body") String body) {
    if (participationDatabase == null)
      return Response.status(Status.SERVICE_UNAVAILABLE).build();

    final Monadics.ListMonadic<String> eventIds = splitCommaSeparatedParam(option(eventIdsString));
    final Monadics.ListMonadic<Long> personIds = splitCommaSeparatedParam(option(personIdsString)).map(toLong);
    if (eventIds.value().isEmpty() || personIds.value().isEmpty())
      return badRequest();

    Person creator;
    try {
      creator = participationDatabase.getPerson(securityService.getUser().getEmail());
    } catch (Exception e) {
      logger.error("No valid participation managment user found for preview this mail! {}",
              ExceptionUtils.getStackTrace(e));
      return Response.status(Status.PRECONDITION_FAILED).build();
    }

    try {
      List<Recording> recordings = ParticipationUtils.getRecordingsByEventId(schedulerService, participationDatabase,
              eventIds.value());
      List<Person> recipients = getPersonById(personIds.value());

      MessageTemplate messageTemplate = mailService.getMessageTemplate(template);
      if (StringUtils.isNotBlank(body))
        messageTemplate.setBody(body);

      MessageSignature messageSignature = null;

      if (signature) {
        try {
          messageSignature = mailService.getCurrentUsersSignature();
        } catch (NotFoundException e) {
          return Response.status(Status.PRECONDITION_FAILED).build();
        }
        if (messageSignature == null)
          return Response.status(Status.PRECONDITION_FAILED).build();
      } else {
        // Create an empty message signature.
        messageSignature = new MessageSignature(0L, creator.getName(), securityService.getUser(), new EmailAddress(
                creator.getEmail(), creator.getName()), Option.<EmailAddress> none(), "", new Date(),
                (new ArrayList<Comment>()));
      }
      messageTemplate.setBody(messageTemplate.getBody().concat("\r\n").concat(messageSignature.getSignature()));
      Message message = new Message(creator, messageTemplate, messageSignature, signature);

      List<Val> renderArr = new ArrayList<Jsons.Val>();
      for (Person recipient : recipients) {
        renderArr.add(Jsons.v(emailSender.renderInvitationBody(message, recordings, recipient)));
      }
      return Response.ok(Jsons.arr(renderArr).toJson()).build();
    } catch (Exception e) {
      logger.error("Could not create a preview email: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("send/{templateId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "sendmail", description = "Render an email with the current email configuration for reviewing it", returnDescription = "The renderend message as JSON", pathParameters = { @RestParameter(name = "templateId", description = "The template id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "eventIds", description = "A comma separated list of event ids", isRequired = true, type = RestParameter.Type.TEXT),
          @RestParameter(name = "personIds", description = "A comma separated list of person ids", isRequired = true, type = RestParameter.Type.TEXT),
          @RestParameter(name = "signature", description = "Whether to include the signature", isRequired = false, type = RestParameter.Type.BOOLEAN),
          @RestParameter(name = "subject", description = "The optional message subject", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "body", description = "The optional message body", isRequired = false, type = RestParameter.Type.TEXT),
          @RestParameter(name = "store", description = "Whether to store the message to the audit trail", isRequired = false, type = RestParameter.Type.BOOLEAN) }, reponses = {
          @RestResponse(description = "No recording or person ids have been set!", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "No message signature is configured or no PM user to perform this action!", responseCode = HttpServletResponse.SC_PRECONDITION_FAILED),
          @RestResponse(description = "Email has been sent", responseCode = HttpServletResponse.SC_NO_CONTENT) })
  public Response sendMail(@PathParam("templateId") long template, @FormParam("eventIds") String eventIdsString,
          @FormParam("personIds") String personIdsString,
          @FormParam("signature") @DefaultValue("false") boolean signature, @FormParam("subject") String subject,
          @FormParam("body") String body, @FormParam("store") @DefaultValue("false") boolean store) {
    if (participationDatabase == null)
      return Response.status(Status.SERVICE_UNAVAILABLE).build();

    final Monadics.ListMonadic<String> eventIds = splitCommaSeparatedParam(option(eventIdsString));
    final Monadics.ListMonadic<Long> personIds = splitCommaSeparatedParam(option(personIdsString)).map(toLong);
    if (eventIds.value().isEmpty() || personIds.value().isEmpty())
      return badRequest();

    Person creator;
    try {
      creator = participationDatabase.getPerson(securityService.getUser().getEmail());
    } catch (Exception e) {
      logger.error("No valid participation management user found for sending this mail!");
      return Response.status(Status.PRECONDITION_FAILED).build();
    }

    try {
      List<Recording> recordings = ParticipationUtils.getRecordingsByEventId(schedulerService, participationDatabase,
              eventIds.value());
      List<Person> recipients = getPersonById(personIds.value());

      MessageTemplate messageTemplate = mailService.getMessageTemplate(template);
      if (StringUtils.isNotBlank(subject))
        messageTemplate.setSubject(subject);
      if (StringUtils.isNotBlank(body))
        messageTemplate.setBody(body);

      MessageSignature messageSignature = mailService.getCurrentUsersSignature();
      if (messageSignature == null)
        return Response.status(Status.PRECONDITION_FAILED).build();

      Message message = new Message(creator, messageTemplate, messageSignature, signature);
      emailSender.sendMessagesForRecordings(recordings, recipients, message, store);

      for (String eventId : eventIds.value()) {
        try {
          schedulerService.updateReviewStatus(eventId, ReviewStatus.UNCONFIRMED);
        } catch (Exception e) {
          logger.error("Unable to update review status of event {}", eventId);
          continue;
        }
      }
      return Response.noContent().build();
    } catch (Exception e) {
      logger.error("Could not update the email configuration: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("sendtestmail")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "sendtestmail", description = "Sends an test mail with the current email configuration", returnDescription = "The sent message as JSON", restParameters = {
          @RestParameter(name = "to", description = "A semi-colon separated list of recipient addresses", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "subject", description = "The message subject", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "body", description = "The message body", isRequired = true, type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "Email has been sent", responseCode = HttpServletResponse.SC_NO_CONTENT) })
  public Response sendTestMail(@FormParam("to") String to, @FormParam("subject") String subject,
          @FormParam("body") String bodyString) {
    try {
      User user = securityService.getUser();

      MessageTemplate template = new MessageTemplate("Test Mail", user, subject, bodyString, TemplateType.INVITATION,
              new Date(), nil(Comment.class));
      MessageSignature messageSignature = MessageSignature.messageSignature("Test Signature", user,
              EmailAddress.emailAddress(user.getEmail(), user.getName()), "This is a test mail signature");

      List<EmailAddress> addresses = new ArrayList<EmailAddress>();
      for (String address : StringUtils.split(to, ";")) {
        addresses.add(EmailAddress.emailAddress(address, null));
      }

      String body = template.getBody().concat("\r\n").concat(messageSignature.getSignature());
      final Mail mail = new Mail(messageSignature.getSender(), messageSignature.getReplyTo(), addresses,
              template.getSubject(), body);
      mailService.send(mail);
      return Response.noContent().build();
    } catch (Exception e) {
      logger.error("Could not update the email configuration: {}", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private List<Person> getPersonById(List<Long> ids) {
    List<Person> persons = new ArrayList<Person>();
    for (long personId : ids) {
      try {
        persons.add(participationDatabase.getPerson(personId));
      } catch (ParticipationManagementDatabaseException e) {
        logger.error("Unable to get person with id {}: {}", personId, ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(e);
      } catch (NotFoundException e) {
        logger.info("Didn't find any person with id {}", personId);
        continue;
      }
    }
    return persons;
  }

  private Function<String, Long> toLong = new Function<String, Long>() {
    @Override
    public Long apply(String a) {
      return Long.parseLong(a);
    }
  };

}
