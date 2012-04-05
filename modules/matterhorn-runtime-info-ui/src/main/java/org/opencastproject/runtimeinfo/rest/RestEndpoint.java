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
package org.opencastproject.runtimeinfo.rest;

import org.opencastproject.util.doc.DocData;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Deprecated
public class RestEndpoint {
  
  public static enum Type {
    WRITE, READ
  };

  public static enum Method {
    GET, POST, PUT, DELETE, ANY
  };

  private String name; // unique key
  private String method;
  private String path;
  private String description;
  private Param bodyParam;
  private List<Param> pathParams;
  private List<Param> requiredParams;
  private List<Param> optionalParams;
  private List<Format> formats;
  private List<Status> statuses;
  private List<String> notes;
  private RestTestForm form;

  private boolean autoPathFormat = false;
  private String pathFormat = ""; // this is used when the automatic path format is enabled (e.g. )
  private String pathFormatHtml = ""; // this is used when the automatic path format is enabled (e.g. )

  /**
   * Create a new basic endpoint, you should use the add methods to fill in the rest of the information about the
   * endpoint data
   * 
   * @param name
   *          the endpoint name (this should be unique for this set of endpoints)
   * @param method
   *          the HTTP method used for this endpoint
   * @param path
   *          the path for this endpoint (e.g. /search OR /add/{id})
   * @param description
   *          [optional]
   */
  public RestEndpoint(String name, Method method, String path, String description) {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("name must not be null and must be alphanumeric");
    }
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    if (!DocRestData.isValidPath(path)) {
      throw new IllegalArgumentException("path must not be null and must look something like /a/b/{c}");
    }
    this.name = name;
    this.method = method.name().toUpperCase();
    this.path = path;
    this.description = description;
  }

  @Override
  public String toString() {
    return "ENDP:" + name + ":" + method + " " + path + " :body=" + bodyParam + " :req=" + requiredParams + " :opt="
            + optionalParams + " :formats=" + formats + " :status=" + statuses + " :notes=" + notes + " :form=" + form;
  }

  /**
   * This is a special parameter which indicates that this value is to be sent as the body of the request, in general
   * the type of this parameter should only be FILE or TEXT but nothing stops you from using the other types<br/>
   * This is always a required parameter as you should never design an endpoint that takes a file sometimes but not
   * always
   * 
   * @param isBinary
   *          if true then this should use an uploader to send, otherwise the data can be placed in a text area
   * @param defaultValue
   *          the default value (only viable for text)
   * @param description
   *          the optional description
   * @return the new param object in case you want to set attributes
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public Param addBodyParam(boolean isBinary, String defaultValue, String description) {
    Param.Type type = isBinary ? Param.Type.FILE : Param.Type.TEXT;
    Param param = new Param("BODY", type, defaultValue, description);
    param.setRequired(true);
    param.setAttribute("rows", "8");
    this.bodyParam = param;
    return param;
  }

  /**
   * Adds a path parameter for this endpoint, this would be a parameter which is passed as part of the path (e.g.
   * /my/path/{param}) and thus must use a name which is safe to place in a URL and does not contain a slash (/)
   * 
   * @param param
   *          the path param to add
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addPathParam(Param param) {
    if (param == null) {
      throw new IllegalArgumentException("param must not be null");
    }
    if (Param.Type.FILE.name().equals(param.getType()) || Param.Type.TEXT.name().equals(param.getType())) {
      throw new IllegalStateException("Cannot add path param of type FILE or TEXT");
    }
    param.setRequired(true);
    param.setPath(true);
    if (this.pathParams == null) {
      this.pathParams = new Vector<Param>(3);
    }
    this.pathParams.add(param);
  }

  /**
   * Adds a required form parameter for this endpoint, this would be a parameter which is passed encoded as part of the
   * request body (commonly referred to as a post or form parameter) <br/>
   * WARNING: This should generally be reserved for endpoints which are used for processing, it is better to use path
   * params unless the required parameter is not part of an identifier for the resource
   * 
   * @param param
   *          the required param to add
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addRequiredParam(Param param) {
    if (param == null) {
      throw new IllegalArgumentException("param must not be null");
    }
    if (isGetMethod()) {
      throw new IllegalStateException("Cannot add required params for GET endpoints");
    }
    param.setRequired(true);
    param.setPath(false);
    if (this.requiredParams == null) {
      this.requiredParams = new Vector<Param>(3);
    }
    this.requiredParams.add(param);
  }

  /**
   * Adds an optional parameter for this endpoint, this would be a parameter which is passed in the query string (for
   * GET) or encoded as part of the body otherwise (often referred to as a post or form parameter)
   * 
   * @param param
   *          the optional param to add
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addOptionalParam(Param param) {
    if (param == null) {
      throw new IllegalArgumentException("param must not be null");
    }
    param.setRequired(false);
    param.setPath(false);
    if (this.optionalParams == null) {
      this.optionalParams = new Vector<Param>(3);
    }
    this.optionalParams.add(param);
  }

  /**
   * Adds a format for the return data for this endpoint
   * 
   * @param format
   *          a format object
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addFormat(Format format) {
    if (format == null) {
      throw new IllegalArgumentException("format must not be null");
    }
    if (this.formats == null) {
      this.formats = new Vector<Format>(2);
    }
    this.formats.add(format);
  }

  /**
   * Adds a response status for this endpoint
   * 
   * @param status
   *          a response status object
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addStatus(Status status) {
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
    if (this.statuses == null) {
      this.statuses = new Vector<Status>(3);
    }
    this.statuses.add(status);
  }

  /**
   * Adds a note for this endpoint
   * 
   * @param note
   *          a note object
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void addNote(String note) {
    if (DocData.isBlank(note)) {
      throw new IllegalArgumentException("note must not be null");
    }
    if (this.notes == null) {
      this.notes = new Vector<String>(3);
    }
    this.notes.add(note);
  }

  /**
   * Sets the test form for this endpoint, if this is null then no test form is rendered for this endpoint
   * 
   * @param form
   *          the test form object (null to clear the form)
   * @throws IllegalArgumentException
   *           if the params are null
   */
  public void setTestForm(RestTestForm form) {
    this.form = form;
  }

  
  /**
   * @param pathFormat the pathFormat to set
   */
  public void setPathFormat(String pathFormat) {
    this.pathFormat = pathFormat;
  }
  
  /**
   * @param pathFormatHtml the pathFormatHtml to set
   */
  public void setPathFormatHtml(String pathFormatHtml) {
    this.pathFormatHtml = pathFormatHtml;
  }
  
  /**
   * Setting this to true will cause the path to be filled in with format extensions which will work with the {FORMAT}
   * convention (which is automatically filled in with the selected or default format key - e.g. json) <br/>
   * This will generate a path like /your/path.{FORMAT} and will show the following on screen GET /your/path.{xml|json}
   * if you have 2 formats in this endpoint
   * 
   * @param autoPathFormat
   *          true to enable, false to disable
   */
  public void setAutoPathFormat(boolean autoPathFormat) {
    this.autoPathFormat = autoPathFormat;
  }

  /**
   * @return true if this endpoint method is GET, otherwise false
   */
  public boolean isGetMethod() {
    boolean match = false;
    if (Method.GET.name().equals(this.method)) {
      match = true;
    }
    return match;
  }

  /**
   * @return the calculated query string for a GET endpoint (e.g. ?blah=1), will be urlencoded for html display
   */
  public String getQueryString() {
    String qs = "";
    if (isGetMethod()) {
      if (this.optionalParams != null && !this.optionalParams.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (Param p : this.optionalParams) {
          if (sb.length() > 2) {
            sb.append("&");
          }
          sb.append(p.getName());
          sb.append("=");
          if (p.getDefaultValue() != null) {
            sb.append(p.getDefaultValue());
          } else {
            sb.append("{");
            sb.append(p.getName());
            sb.append("}");
          }
        }
        /*
         * try { qs = URLEncoder.encode(sb.toString(),"UTF-8"); } catch (UnsupportedEncodingException e) { qs =
         * sb.toString(); }
         */
        qs = StringEscapeUtils.escapeHtml(sb.toString());
      }
    }
    return qs;
  }

  // GETTERS
  public String getName() {
    return name;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public String getDescription() {
    return description;
  }

  public Param getBodyParam() {
    return bodyParam;
  }

  public List<Param> getPathParams() {
    if (this.pathParams == null) {
      this.pathParams = new ArrayList<Param>(0);
    }
    return this.pathParams;
  }

  public List<Param> getRequiredParams() {
    if (this.requiredParams == null) {
      this.requiredParams = new ArrayList<Param>(0);
    }
    return this.requiredParams;
  }

  public List<Param> getOptionalParams() {
    if (this.optionalParams == null) {
      this.optionalParams = new ArrayList<Param>(0);
    }
    return this.optionalParams;
  }

  public List<Format> getFormats() {
    if (this.formats == null) {
      this.formats = new ArrayList<Format>(0);
    }
    return this.formats;
  }

  public List<Status> getStatuses() {
    if (this.statuses == null) {
      this.statuses = new ArrayList<Status>(0);
    }
    return this.statuses;
  }

  public List<String> getNotes() {
    if (this.notes == null) {
      this.notes = new ArrayList<String>(0);
    }
    return this.notes;
  }

  public RestTestForm getForm() {
    return form;
  }

  public String getPathFormat() {
    return pathFormat;
  }

  public String getPathFormatHtml() {
    return pathFormatHtml;
  }

  public boolean isAutoPathFormat() {
    return autoPathFormat;
  }
}
