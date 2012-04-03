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
package org.opencastproject.lti;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A servlet to accept an LTI login via POST. The actual authentication happens in LtiProcessingFilter. GET requests
 * produce JSON containing the LTI parameters passed during LTI launch.
 */
public class LtiServlet extends HttpServlet {

	private static final String LTI_CUSTOM_PREFIX = "custom_";

  /** The logger */
	private static final Logger logger = LoggerFactory.getLogger(LtiServlet.class);

  /** The serialization uid */
  private static final long serialVersionUID = 6138043870346176520L;

  /** The key used to store the LTI attributes in the HTTP session */
  public static final String SESSION_ATTRIBUTE_KEY = "org.opencastproject.lti.LtiServlet";

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
  }

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
    session.setAttribute(SESSION_ATTRIBUTE_KEY, getLtiValuesAsMap(req));

    // We must return a 200 for some oauth client libraries to accept this as a valid response

    // The URL of the LTI tool. If no specific tool is passed we use the test tool
    String toolReq = req.getParameter("custom_tool");
    String toolUrl = "/ltisample/";
    if (toolReq != null) {
      toolUrl = toolReq;
      if (!(toolUrl.indexOf("/") == 0)) {
        //if not supplied we assume this is a root path to the tool 
        toolUrl = "/" + toolUrl;
      }
    }

    String customParams = getCustomParams(req);
    if (customParams != null) {
      toolUrl = toolUrl + "?" + customParams; 
    }
    
    // Always set the session cookie
    resp.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + ";Path=/");
    
    // The client can specify debug option by passing a value to test
    String testString = req.getParameter("custom_test");
    boolean test = false;
    if (testString != null) {
      logger.debug("test: {}", req.getParameter("custom_test"));
      test = Boolean.valueOf(testString).booleanValue();
    } 
    
    //we need to add the custom params to the outgoing request
    
    
    // if in test mode display details where we go 
    if (test) {
      resp.getWriter().write("<html><body>Welcome to matterhorn lti, you are going to " + toolUrl + "<br>");
      resp.getWriter().write("<a href=\"" + toolUrl + "\">continue...</a></body></html>");
      //TODO we should probably print the paramaters.
    } else {
      logger.debug(toolUrl);
      resp.sendRedirect(toolUrl);
    }
  }
  
  /**
   * Get a list of custom params to pass to the tool 
   * @param req
   * @return 
   */
  @SuppressWarnings("unchecked")
  protected String getCustomParams(HttpServletRequest req) {
    Map<String, String> paramMap = req.getParameterMap();
    StringBuilder builder = new StringBuilder();
    Set<String> entries = paramMap.keySet();
    Iterator<String> iterator = entries.iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      logger.debug("got key: " + key);
      if (key.indexOf(LTI_CUSTOM_PREFIX) >= 0) {
        String paramValue = req.getParameter(key);
        //we need to remove the prefix _custom
        String paramName = key.substring(LTI_CUSTOM_PREFIX.length());
        logger.debug("Found custom var: " + paramName + ":" + paramValue);
        builder.append(paramName + "=" + paramValue + "&");
      }
    }
    
    
    return builder.toString();
    
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
      resp.sendError(HttpServletResponse.SC_NOT_FOUND); // If there is no session, there is nothing to see here
    } else {
      Map<String, String> ltiAttributes = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE_KEY);
      if (ltiAttributes == null) {
        ltiAttributes = new HashMap<String, String>();
      }
      resp.setContentType("application/json");
      JSONObject.writeJSONString(ltiAttributes, resp.getWriter());
    }
  }
}
