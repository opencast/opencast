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

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.signature.OAuthSignatureMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

/**
 * LTI functions from tsugi-util to send content items
 */
public final class LtiUtils {

  public static final String LTI_VERSION = "lti_version";
  public static final String LTI_MESSAGE_TYPE = "lti_message_type";

  public static final String TOOL_CONSUMER_INSTANCE_GUID = "tool_consumer_instance_guid";
  public static final String TOOL_CONSUMER_INSTANCE_DESCRIPTION = "tool_consumer_instance_description";
  public static final String TOOL_CONSUMER_INSTANCE_URL = "tool_consumer_instance_url";
  public static final String TOOL_CONSUMER_INSTANCE_NAME = "tool_consumer_instance_name";
  public static final String TOOL_CONSUMER_INSTANCE_CONTACT_EMAIL = "tool_consumer_instance_contact_email";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(LtiUtils.class);

  private LtiUtils() {
  }

  /**
    * Add the necessary fields and sign.
    *
    * @param postProp
    * @param url
    * @param method
    * @param oauthConsumerKey
    * @param oauthConsumerSecret
    * @param toolConsumerInstanceGuid
    * @param toolConsumerInstanceDescription
    * @param toolConsumerInstanceUrl
    * @param toolConsumerInstanceName
    * @param toolConsumerInstanceContactEmail
    * @param extra
    * @return
  */
  public static Map<String, String> signProperties(
      Map<String, String> postProp, String url, String method,
      String oauthConsumerKey, String oauthConsumerSecret,
      String toolConsumerInstanceGuid,
      String toolConsumerInstanceDescription,
      String toolConsumerInstanceUrl, String toolConsumerInstanceName,
      String toolConsumerInstanceContactEmail,
      Map<String, String> extra) {

    if (postProp.get(LTI_VERSION) == null) {
      postProp.put(LTI_VERSION, "LTI-1p0");
    }
    if (postProp.get(LTI_MESSAGE_TYPE) == null) {
      postProp.put(LTI_MESSAGE_TYPE, "basic-lti-launch-request");
    }

    if (toolConsumerInstanceGuid != null) {
      postProp.put(TOOL_CONSUMER_INSTANCE_GUID, toolConsumerInstanceGuid);
    }
    if (toolConsumerInstanceDescription != null) {
      postProp.put(TOOL_CONSUMER_INSTANCE_DESCRIPTION,
          toolConsumerInstanceDescription);
    }
    if (toolConsumerInstanceUrl != null) {
      postProp.put(TOOL_CONSUMER_INSTANCE_URL, toolConsumerInstanceUrl);
    }
    if (toolConsumerInstanceName != null) {
      postProp.put(TOOL_CONSUMER_INSTANCE_NAME, toolConsumerInstanceName);
    }
    if (toolConsumerInstanceContactEmail != null) {
      postProp.put(TOOL_CONSUMER_INSTANCE_CONTACT_EMAIL,
          toolConsumerInstanceContactEmail);
    }

    if (postProp.get("oauth_callback") == null) {
      postProp.put("oauth_callback", "about:blank");
    }

    if (oauthConsumerKey == null || oauthConsumerSecret == null) {
      logger.debug("No signature generated in signProperties");
      return postProp;
    }

    OAuthMessage oam = new OAuthMessage(method, url, postProp.entrySet());
    OAuthConsumer cons = new OAuthConsumer("about:blank", oauthConsumerKey,
        oauthConsumerSecret, null);
    OAuthAccessor acc = new OAuthAccessor(cons);
    try {
      oam.addRequiredParameters(acc);
      String baseString = OAuthSignatureMethod.getBaseString(oam);
      logger.debug("Base Message String\n{}\n", baseString);
      if (extra != null) {
        extra.put("BaseString", baseString);
      }

      List<Map.Entry<String, String>> params = oam.getParameters();

      Map<String, String> nextProp = new HashMap<String, String>();
      // Convert to Map<String, String>
      for (final Map.Entry<String, String> entry : params) {
        nextProp.put(entry.getKey(), entry.getValue());
      }
      return nextProp;
    } catch (net.oauth.OAuthException e) {
      logger.warn("BasicLTIUtil.signProperties OAuth Exception {}", e.getMessage());
      throw new Error(e);
    } catch (java.io.IOException e) {
      logger.warn("BasicLTIUtil.signProperties IO Exception {}", e.getMessage());
      throw new Error(e);
    } catch (java.net.URISyntaxException e) {
      logger.warn("BasicLTIUtil.signProperties URI Syntax Exception {}", e.getMessage());
      throw new Error(e);
    }
  }



  /**
  * Create the HTML to render a POST form and then automatically submit it.
  *
  * @param cleanProperties
  * @param endpoint
  *  The LTI launch url.
  * @param launchtext
  *  The LTI launch text. Used if javascript is turned off.
  * @param debug
  *  Useful for viewing the HTML before posting to end point.
  * @param extra
  *  Useful for viewing the HTML before posting to end point.
  * @return the HTML ready for IFRAME src = inclusion.
  */
  public static String postLaunchHTML(
      final Map<String, String> cleanProperties, String endpoint,
      String launchtext, boolean debug, Map<String,String> extra) {
    // Assume autosubmit is true for backwards compatibility
    boolean autosubmit = true;
    return postLaunchHTML(cleanProperties, endpoint, launchtext, autosubmit, debug, extra);
  }


  /**
   * Create the HTML to render a POST form and then automatically submit it.
   *
   * @param cleanProperties
   * @param endpoint
   *  The LTI launch url.
   * @param launchtext
   *  The LTI launch text. Used if javascript is turned off.
   * @param autosubmit
   *  Whether or not we want the form autosubmitted
   * @param extra
   *  Useful for viewing the HTML before posting to end point.
   * @return the HTML ready for IFRAME src = inclusion.
   */
  public static String postLaunchHTML(
      final Map<String, String> cleanProperties, String endpoint,
      String launchtext, boolean autosubmit, boolean debug,
      Map<String,String> extra) {

    if (cleanProperties == null || cleanProperties.isEmpty()) {
      throw new IllegalArgumentException(
          "cleanProperties == null || cleanProperties.isEmpty()");
    }
    if (endpoint == null) {
      throw new IllegalArgumentException("endpoint == null");
    }
    Map<String, String> newMap = null;
    if (debug) {
      // sort the properties for readability
      newMap = new TreeMap<String, String>(cleanProperties);
    } else {
      newMap = cleanProperties;
    }
    StringBuilder text = new StringBuilder();
    // paint form
    String submitUuid = UUID.randomUUID().toString().replace("-","_");
    text.append("<div id=\"ltiLaunchFormArea_");
    text.append(submitUuid);
    text.append("\">\n");
    text.append("<form action=\"");
    text.append(endpoint);
    text.append("\" name=\"ltiLaunchForm\" id=\"ltiLaunchForm_" + submitUuid + "\" method=\"post\" ");
    text.append(" encType=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");
    if (debug) {
    }
    for (Entry<String, String> entry : newMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (value == null) {
        continue;
      }
      // This will escape the contents pretty much - at least
      // we will be safe and not generate dangerous HTML
      key = htmlspecialchars(key);
      value = htmlspecialchars(value);
      text.append("<input type=\"hidden\" name=\"");
      text.append(key);
      text.append("\" value=\"");
      text.append(value);
      text.append("\"/>\n");
    }

    // Paint the submit button
    if (debug) {
      text.append("<input type=\"submit\" value=\"");
      text.append(htmlspecialchars(launchtext));
      text.append("\">\n");

      text.append(" <input type=\"Submit\" value=\"Show Launch Data\" onclick=\"document.getElementById('ltiLaunchDebug_");
      text.append(submitUuid);
      text.append("').style.display = 'block';return false;\">\n");
    } else {
      text.append("<input type=\"submit\" style=\"display: none\" value=\"");
      text.append(htmlspecialchars(launchtext));
      text.append("\">\n");
    }

    if (extra != null) {
      String buttonHtml = extra.get("button_html");
      if (buttonHtml != null) {
        text.append(buttonHtml);
      }
    }

    text.append("</form>\n");
    text.append("</div>\n");

    // Paint the auto-pop up if we are transitioning from https: to http:
    // and are not already the top frame...
    text.append("<script type=\"text/javascript\">\n");
    text.append("if (window.top!=window.self) {\n");
    text.append("  theform = document.getElementById('ltiLaunchForm_");
    text.append(submitUuid);
    text.append("');\n");
    text.append("  if ( theform && theform.action ) {\n");
    text.append("   formAction = theform.action;\n");
    text.append("   ourUrl = window.location.href;\n");
    text.append("   if ( formAction.indexOf('http://') == 0 && ourUrl.indexOf('https://') == 0 ) {\n");
    text.append("      theform.target = '_blank';\n");
    text.append("      window.console && console.log('Launching http from https in new window!');\n");
    text.append("    }\n");
    text.append("  }\n");
    text.append("}\n");
    text.append("</script>\n");

    // paint debug output
    if (debug) {
      text.append("<pre id=\"ltiLaunchDebug_");
      text.append(submitUuid);
      text.append("\" style=\"display: none\">\n");
      text.append("<b>BasicLTI Endpoint</b>\n");
      text.append(endpoint);
      text.append("\n\n");
      text.append("<b>BasicLTI Parameters:</b>\n");
      for (Entry<String, String> entry : newMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        if (value == null) {
          continue;
        }
        text.append(htmlspecialchars(key));
        text.append("=");
        text.append(htmlspecialchars(value));
        text.append("\n");
      }
      text.append("</pre>\n");
      if (extra != null) {
        String baseString = extra.get("BaseString");
        if (baseString != null) {
          text.append("<!-- Base String\n");
          text.append(baseString.replaceAll("-->","__>"));
          text.append("\n-->\n");
        }
      }
    } else if (autosubmit) {
      // paint auto submit script
      text.append("<script language=\"javascript\"> \n");
      text.append("    document.getElementById('ltiLaunchFormArea_");
      text.append(submitUuid);
      text.append("').style.display = \"none\";\n");
      text.append("    document.getElementById('ltiLaunchForm_");
      text.append(submitUuid);
      text.append("').submit(); \n</script> \n");
    }

    String htmltext = text.toString();
    return htmltext;
  }

  // Basic utility to encode form text - handle the "safe cases"
  public static String htmlspecialchars(String input) {
    if (input == null) {
      return null;
    }
    String retval = input.replace("&", "&amp;");
    retval = retval.replace("\"", "&quot;");
    retval = retval.replace("<", "&lt;");
    retval = retval.replace(">", "&gt;");
    retval = retval.replace("=", "&#61;");
    return retval;
  }
}
