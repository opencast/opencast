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

package org.opencastproject.lti;

import org.opencastproject.kernel.security.OAuthConsumerDetailsService;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.provider.ConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetailsService;
import org.tsugi.basiclti.BasicLTIConstants;
import org.tsugi.basiclti.BasicLTIUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.UriBuilder;

/**
 * A servlet to accept an LTI login via POST. The actual authentication happens in LtiProcessingFilter. GET requests
 * produce JSON containing the LTI parameters passed during LTI launch.
 */
@Component(
    service = Servlet.class,
    property = {
        "service.description=LTI Servlet",
    }
)
@HttpWhiteboardServletName("/lti")
@HttpWhiteboardServletPattern("/lti/*")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=opencast)")
public class LtiServlet extends HttpServlet implements ManagedService {

  private static final String LTI_CUSTOM_PREFIX = "custom_";
  private static final String LTI_CUSTOM_TOOL = "custom_tool";
  private static final String LTI_CUSTOM_DL_TOOL = "custom_dl_tool";
  private static final String LTI_CUSTOM_TEST = "custom_test";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LtiServlet.class);

  /** The serialization uid */
  private static final long serialVersionUID = 6138043870346176520L;

  /** The key used to store the LTI attributes in the HTTP session */
  public static final String SESSION_ATTRIBUTE_KEY = "org.opencastproject.lti.LtiServlet";

  /** Path under which all the LTI tools are available */
  private static final String TOOLS_URL = "/ltitools";

  /** Path under which content items are embedded */
  private static final String CONTENT_ITEMS_URI = "/lti/ci";

  // The following LTI launch parameters are made available to GET requests at the /lti endpoint.
  // See https://www.imsglobal.org/specs/ltiv1p2/implementation-guide for the meaning of each.

  /** See the LTI specification */
  public static final String LTI_MESSAGE_TYPE = "lti_message_type";

  /** See the LTI specification */
  public static final String LTI_VERSION = "lti_version";

  /** See the LTI specification */
  public static final String RESOURCE_LINK_ID = "resource_link_id";

  /** See the LTI specification */
  public static final String RESOURCE_LINK_TITLE = "resource_link_title";

  /** See the LTI specification */
  public static final String RESOURCE_LINK_DESCRIPTION = "resource_link_description";

  /** See the LTI specification */
  public static final String USER_ID = "user_id";

  /** See the LTI specification */
  public static final String USER_IMAGE = "user_image";

  /** See the LTI specification */
  public static final String ROLES = "roles";

  /** See the LTI specification */
  public static final String GIVEN_NAME = "lis_person_name_given";

  /** See the LTI specification */
  public static final String FAMILY_NAME = "lis_person_name_family";

  /** See the LTI specification */
  public static final String FULL_NAME = "lis_person_name_full";

  /** See the LTI specification */
  public static final String EMAIL = "lis_person_contact_email_primary";

  /** See the LTI specification */
  public static final String CONTEXT_ID = "context_id";

  /** See the LTI specification */
  public static final String CONTEXT_TYPE = "context_type";

  /** See the LTI specification */
  public static final String CONTEXT_TITLE = "context_title";

  /** See the LTI specification */
  public static final String CONTEXT_LABEL = "context_label";

  /** See the LTI specification */
  public static final String LOCALE = "launch_presentation_locale";

  /** See the LTI specification */
  public static final String TARGET = "launch_presentation_document_target";

  /** See the LTI specification */
  public static final String WIDTH = "launch_presentation_width";

  /** See the LTI specification */
  public static final String HEIGHT = "launch_presentation_height";

  /** See the LTI specification */
  public static final String RETURN_URL = "launch_presentation_return_url";

  /** See the LTI specification */
  public static final String CONSUMER_GUID = "tool_consumer_instance_guid";

  /** See the LTI specification */
  public static final String CONSUMER_NAME = "tool_consumer_instance_name";

  /** See the LTI specification */
  public static final String CONSUMER_DESCRIPTION = "tool_consumer_instance_description";

  /** See the LTI specification */
  public static final String CONSUMER_URL = "tool_consumer_instance_url";

  /** See the LTI specification */
  public static final String CONSUMER_CONTACT = "tool_consumer_instance_contact_email";

  /** See the LTI specification */
  public static final String COURSE_OFFERING = "lis_course_offering_sourcedid";

  /** See the LTI specification */
  public static final String COURSE_SECTION = "lis_course_section_sourcedid";

  /** needed to keep track of which secret to sign ContentItemSelection message **/
  private static final String CONSUMER_KEY = "consumer_key";
  private static final String OAUTH_CONSUMER_KEY = BasicLTIConstants.OAUTH_PREFIX + CONSUMER_KEY;

  /** LTI parameter not specified in tsugi library **/
  private static final String CONTENT_ITEMS = "content_items";

  public static final SortedSet<String> LTI_CONSTANTS;

  static {
    LTI_CONSTANTS = new TreeSet<String>();
    LTI_CONSTANTS.add(LTI_MESSAGE_TYPE);
    LTI_CONSTANTS.add(LTI_VERSION);
    LTI_CONSTANTS.add(RESOURCE_LINK_ID);
    LTI_CONSTANTS.add(RESOURCE_LINK_TITLE);
    LTI_CONSTANTS.add(RESOURCE_LINK_DESCRIPTION);
    LTI_CONSTANTS.add(USER_ID);
    LTI_CONSTANTS.add(USER_IMAGE);
    LTI_CONSTANTS.add(ROLES);
    LTI_CONSTANTS.add(GIVEN_NAME);
    LTI_CONSTANTS.add(FAMILY_NAME);
    LTI_CONSTANTS.add(FULL_NAME);
    LTI_CONSTANTS.add(EMAIL);
    LTI_CONSTANTS.add(CONTEXT_ID);
    LTI_CONSTANTS.add(CONTEXT_TYPE);
    LTI_CONSTANTS.add(CONTEXT_TITLE);
    LTI_CONSTANTS.add(CONTEXT_LABEL);
    LTI_CONSTANTS.add(LOCALE);
    LTI_CONSTANTS.add(TARGET);
    LTI_CONSTANTS.add(WIDTH);
    LTI_CONSTANTS.add(HEIGHT);
    LTI_CONSTANTS.add(RETURN_URL);
    LTI_CONSTANTS.add(CONSUMER_GUID);
    LTI_CONSTANTS.add(CONSUMER_NAME);
    LTI_CONSTANTS.add(CONSUMER_DESCRIPTION);
    LTI_CONSTANTS.add(CONSUMER_URL);
    LTI_CONSTANTS.add(CONSUMER_CONTACT);
    LTI_CONSTANTS.add(COURSE_OFFERING);
    LTI_CONSTANTS.add(COURSE_SECTION);
    LTI_CONSTANTS.add(BasicLTIConstants.DATA);
    LTI_CONSTANTS.add(BasicLTIConstants.CONTENT_ITEM_RETURN_URL);
    LTI_CONSTANTS.add(OAUTH_CONSUMER_KEY);
  }

  private OAuthConsumerDetailsService consumerDetailsService;

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    // Store the LTI data as a map in the session
    HttpSession session = req.getSession(false);

    // Always set the session cookie
    resp.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + ";Path=/");

    // Send content item (deep linking) message back to LMS
    if (CONTENT_ITEMS_URI.equals(req.getRequestURI())) {
      sendContentItem(req, resp);
      return;
    }

    session.setAttribute(SESSION_ATTRIBUTE_KEY, getLtiValuesAsMap(req));

    // The URL of the LTI tool. If no specific tool is passed we use the test tool
    UriBuilder builder;
    String messageType = StringUtils.trimToEmpty(req.getParameter(LTI_MESSAGE_TYPE));
    try {
      URI toolUri;

      if (messageType.equals(BasicLTIConstants.LTI_MESSAGE_TYPE_CONTENTITEMSELECTIONREQUEST)) {
        toolUri = new URI(URLDecoder.decode(StringUtils.trimToEmpty(
                req.getParameter(LTI_CUSTOM_DL_TOOL)), "UTF-8"));
      } else if (req.getRequestURI().startsWith("/lti/player/")) {
        String mpID = req.getRequestURI().replace("/lti/player/", "");
        String redirectUrl = "/play/" + mpID;
        logger.debug("Received LTI content play request for {}: redirecting to {}", mpID, redirectUrl);
        resp.sendRedirect(redirectUrl);
        return;
      } else {
        String customTool = URLDecoder.decode(StringUtils.trimToEmpty(
                req.getParameter(LTI_CUSTOM_TOOL)), StandardCharsets.UTF_8.displayName());
        toolUri = new URI(customTool);
      }

      if (toolUri.getPath().isEmpty()) {
        throw new URISyntaxException(toolUri.toString(), "Provided 'custom_tool' has an empty path");
      }

      // Make sure that the URI path starts with '/'. Otherwise, UriBuilder handles URIs incorrectly
      if (!toolUri.isOpaque() && !toolUri.getPath().startsWith("/")) {
        // Also, remove the schema and "authority" parts of the URI for security reasons
        builder = UriBuilder
                .fromUri(new URI(null, null, '/' + toolUri.getPath(), toolUri.getQuery(), toolUri.getFragment()));
      } else {
        // Remove the schema and "authority" parts of the URI for security reasons.
        // "authority" consists of user-info, host and port.
        builder = UriBuilder.fromUri(toolUri).scheme(null).host(null).userInfo(null).port(-1);
      }
    } catch (URISyntaxException ex) {
      logger.warn("The 'custom_tool' parameter was invalid: '{}'. Reverting to default: '{}'",
              Arrays.toString(req.getParameterValues(LTI_CUSTOM_TOOL)), TOOLS_URL);
      builder = UriBuilder.fromPath(TOOLS_URL);
    }

    // We need to add the custom params to the outgoing request
    for (String key : req.getParameterMap().keySet()) {
      logger.debug("Found query parameter '{}'", key);
      if (key.startsWith(LTI_CUSTOM_PREFIX) && (!LTI_CUSTOM_TOOL.equals(key))) {
        String paramValue = req.getParameter(key);
        // we need to remove the prefix custom_
        String paramName = key.substring(LTI_CUSTOM_PREFIX.length());
        builder.queryParam(paramName, paramValue);
      }
    }

    // Add locale param from LMS
    String localeParamValue = req.getParameter(LOCALE);
    if (StringUtils.isNotBlank(localeParamValue)) {
      // 'lng' is query param for i18next-browser-languagedetector
      builder.queryParam("lng", localeParamValue);
    }

    // add params required for content item
    if (messageType.equals(BasicLTIConstants.LTI_MESSAGE_TYPE_CONTENTITEMSELECTIONREQUEST)) {
      if (req.getParameterMap().containsKey(BasicLTIConstants.DATA)) {
        builder.queryParam(BasicLTIConstants.DATA, req.getParameter(BasicLTIConstants.DATA));
      }
      builder.queryParam(CONSUMER_KEY, req.getParameter(BasicLTIConstants.OAUTH_PREFIX + CONSUMER_KEY));
      builder.queryParam(
              BasicLTIConstants.CONTENT_ITEM_RETURN_URL, req.getParameter(BasicLTIConstants.CONTENT_ITEM_RETURN_URL));
    }

    // Build the final URL (as a string)
    String redirectUrl = builder.build().toString();

    // The client can specify debug option by passing a value to test
    // if in test mode display details where we go
    if (Boolean.valueOf(StringUtils.trimToEmpty(req.getParameter(LTI_CUSTOM_TEST)))) {
      resp.setContentType("text/html");
      resp.getWriter().write("<html><body>Welcome to Opencast LTI; you are going to " + redirectUrl + "<br>");
      resp.getWriter().write("<a href=\"" + redirectUrl + "\">continue...</a></body></html>");
      // TODO we should probably print the parameters.
    } else {
      resp.sendRedirect(redirectUrl);
    }
  }

  /**
   * Sends a ContentItemSelection response back to the LMS
   *
   * @param req
   *          the HttpServletRequest
   * @param resp
   *          the HttpServletResponse
   */
  private void sendContentItem(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String consumerKey = req.getParameter(CONSUMER_KEY);
    ConsumerDetails consumer = consumerDetailsService.loadConsumerByConsumerKey(consumerKey);
    String consumerSecret = ((SharedConsumerSecretImpl) consumer.getSignatureSecret()).getConsumerSecret();

    String contentItems = req.getParameter(CONTENT_ITEMS);
    String returnUrl = req.getParameter(BasicLTIConstants.CONTENT_ITEM_RETURN_URL);

    Map<String, String> props = new HashMap<String, String>();
    props.put(BasicLTIConstants.LTI_MESSAGE_TYPE, BasicLTIConstants.LTI_MESSAGE_TYPE_CONTENTITEMSELECTION);
    props.put(CONTENT_ITEMS, contentItems);
    props.put(BasicLTIConstants.DATA, req.getParameter(BasicLTIConstants.DATA));
    Map<String, String> properties = BasicLTIUtil.signProperties(props, returnUrl,
            "POST", consumerKey, consumerSecret, "", "", "", "", "", null);
    resp.setContentType("text/html");

    // whether to show debug info before sending content items to tool consumer
    boolean test = "true".equals(req.getParameter(LTI_CUSTOM_TEST));

    resp.getWriter().write(BasicLTIUtil.postLaunchHTML(properties, returnUrl, "Send content to LMS", test, null));
  }

  /**
   * Builds a map of LTI parameters
   *
   * @param req
   *          the LTI Launch HttpServletRequest
   * @return the map of LTI parameters to the values for this launch
   */
  protected Map<String, String> getLtiValuesAsMap(HttpServletRequest req) {
    Map<String, String> ltiValues = new HashMap<String, String>();
    for (String key : LTI_CONSTANTS) {
      String value = StringUtils.trimToNull(req.getParameter(key));
      if (value != null) {
        ltiValues.put(key, value);
      }
    }
    return ltiValues;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(false);
    if (session == null) {
      // If there is no session, there is nothing to see here
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    } else {
      Map<String, String> ltiAttributes = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE_KEY);
      if (ltiAttributes == null) {
        ltiAttributes = new HashMap<>();
        ltiAttributes.put("roles", "Instructor");
      }
      resp.setContentType("application/json");
      JSONObject.writeJSONString(ltiAttributes, resp.getWriter());
    }
  }

  /**
   * Sets the consumer details service
   *
   * @param consumerDetailsService
   *          the consumer details service to set
   */
  @Reference
  public void setConsumerDetailsService(ConsumerDetailsService consumerDetailsService) {
    this.consumerDetailsService = (OAuthConsumerDetailsService) consumerDetailsService;
  }

  @Override
  public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
    logger.info("LTI Servlet updated.");
  }
}
