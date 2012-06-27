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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    // The URL of the LTI tool. Currently, we don't have a real LTI tool, so make due with the sample
    String toolUrl = "/ltisample/";

    // Always set the session cookie
    resp.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + ";Path=/");

    // TODO: Write client-side js to sent the user someplace useful
    resp.getWriter().write("<a href=\"" + toolUrl + "\">continue...</a>");
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
