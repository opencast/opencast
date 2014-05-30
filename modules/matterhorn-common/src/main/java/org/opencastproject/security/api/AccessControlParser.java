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
package org.opencastproject.security.api;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

/**
 * Marshals and unmarshals {@link AccessControlList}s to/from XML.
 */
public final class AccessControlParser {
  /** Role constant used in JSON formatted access control entries */
  public static final String ROLE = "role";

  /** Action constant used in JSON formatted access control entries */
  public static final String ACTION = "action";

  /** Allow constant used in JSON formatted access control entries */
  public static final String ALLOW = "allow";

  /** ACL constant used in JSON formatted access control entries */
  public static final String ACL = "acl";

  /** ACE constant used in JSON formatted access control entries */
  public static final String ACE = "ace";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  private static final JAXBContext jaxbContext;

  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.security.api",
              AccessControlParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Disallow construction of this utility class.
   */
  private AccessControlParser() {
  }

  /**
   * Parses a string into an ACL.
   * 
   * @param serializedForm
   *          the string containing the xml or json formatted access control list.
   * @return the access control list
   * @throws IOException
   *           if the encoding is invalid
   * @throws AccessControlParsingException
   *           if the format is invalid
   */
  public static AccessControlList parseAcl(String serializedForm) throws IOException, AccessControlParsingException {
    // Determine whether to parse this as XML or JSON
    if (serializedForm.startsWith("{")) {
      return parseJson(serializedForm);
    } else {
      return parseXml(IOUtils.toInputStream(serializedForm, ENCODING));
    }
  }

  /** Same like {@link #parseAcl(String)} but throws runtime exceptions in case of an error. */
  public static AccessControlList parseAclSilent(String serializedForm) {
    try {
      return parseAcl(serializedForm);
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /** {@link #parseAclSilent(String)} as a function. */
  public static final Function<String, AccessControlList> parseAclSilent = new Function<String, AccessControlList>() {
    @Override
    public AccessControlList apply(String s) {
      return parseAclSilent(s);
    }
  };

  /** Functional version of {@link #parseAcl(String)}. */
  public static final Function<String, Either<Exception, AccessControlList>> parseAcl = new Function<String, Either<Exception, AccessControlList>>() {
    @Override
    public Either<Exception, AccessControlList> apply(String s) {
      try {
        return right(parseAcl(s));
      } catch (Exception e) {
        return left(e);
      }
    }
  };

  /**
   * Unmarshals an ACL from an xml input stream.
   * 
   * @param in
   *          the xml input stream
   * @return the acl
   * @throws IOException
   *           if there is a problem unmarshaling the stream
   * @throws AccessControlParsingException
   *           if the format is invalid
   */
  public static AccessControlList parseAcl(InputStream in) throws IOException, AccessControlParsingException {
    return parseAcl(IOUtils.toString(in, ENCODING));
  }

  /**
   * Parses a JSON stream to an ACL.
   * 
   * @param content
   *          the JSON stream
   * @return the access control list
   * @throws AccessControlParsingException
   *           if the json is not properly formatted
   */
  private static AccessControlList parseJson(String content) throws AccessControlParsingException {
    try {
      JSONObject json = (JSONObject) new JSONParser().parse(content);
      JSONObject jsonAcl = (JSONObject) json.get(ACL);
      Object jsonAceObj = jsonAcl.get(ACE);

      AccessControlList acl = new AccessControlList();
      if (jsonAceObj == null)
        return acl;

      if (jsonAceObj instanceof JSONObject) {
        JSONObject jsonAce = (JSONObject) jsonAceObj;
        acl.getEntries().add(getAce(jsonAce));
      } else {
        JSONArray jsonAceArray = (JSONArray) jsonAceObj;
        for (Object element : jsonAceArray) {
          JSONObject jsonAce = (JSONObject) element;
          acl.getEntries().add(getAce(jsonAce));
        }
      }
      return acl;
    } catch (ParseException e) {
      throw new AccessControlParsingException(e);
    }
  }

  /**
   * Converts a JSON representation of an access control entry to an {@link AccessControlEntry}.
   * 
   * @param jsonAce
   *          the json object
   * @return the access control entry
   */
  private static AccessControlEntry getAce(JSONObject jsonAce) {
    String role = (String) jsonAce.get(ROLE);
    String action = (String) jsonAce.get(ACTION);
    Boolean allow = (Boolean) jsonAce.get(ALLOW);
    return new AccessControlEntry(role, action, allow);
  }

  /**
   * Parses an XML stream to an ACL.
   * 
   * @param in
   *          the XML stream
   * @throws IOException
   *           if there is a problem unmarshaling the stream
   */
  private static AccessControlList parseXml(InputStream in) throws IOException, AccessControlParsingException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), AccessControlList.class).getValue();
    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      } else {
        throw new AccessControlParsingException(e);
      }
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Serializes an AccessControlList to its XML form.
   * 
   * @param acl
   *          the access control list
   * @return the xml as a string
   * @throws IOException
   *           if there is a problem marshaling the xml
   */
  public static String toXml(AccessControlList acl) throws IOException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(acl, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static String toJson(AccessControlList acl) throws IOException {
    JSONObject json = new JSONObject();
    JSONObject jsonAcl = new JSONObject();
    List<AccessControlEntry> entries = acl.getEntries();
    int numEntries = entries.size();
    switch (numEntries) {
      case 0:
        break;
      case 1:
        AccessControlEntry singleEntry = entries.get(0);
        JSONObject singleJsonEntry = new JSONObject();
        jsonAcl.put(ACE, singleJsonEntry);
        singleJsonEntry.put(ROLE, singleEntry.getRole());
        singleJsonEntry.put(ACTION, singleEntry.getAction());
        singleJsonEntry.put(ALLOW, singleEntry.isAllow());
        break;
      default:
        JSONArray jsonEntryArray = new JSONArray();
        jsonAcl.put(ACE, jsonEntryArray);
        for (AccessControlEntry entry : entries) {
          JSONObject jsonEntry = new JSONObject();
          jsonEntry.put(ROLE, entry.getRole());
          jsonEntry.put(ACTION, entry.getAction());
          jsonEntry.put(ALLOW, entry.isAllow());
          jsonEntryArray.add(jsonEntry);
        }
    }
    json.put(ACL, jsonAcl);
    return json.toJSONString();
  }

  public static String toJsonSilent(AccessControlList acl) {
    try {
      return toJson(acl);
    } catch (IOException e) {
      return chuck(e);
    }
  }

  public static final Function<AccessControlList, String> toJsonSilent = new Function<AccessControlList, String>() {
    @Override
    public String apply(AccessControlList acl) {
      return toJsonSilent(acl);
    }
  };
}
