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

package org.opencastproject.userdirectory.sakai;

import org.opencastproject.security.api.CachingUserProviderMXBean;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.PatternSyntaxException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A UserProvider that reads user roles from Sakai.
 */
public class SakaiUserProviderInstance implements UserProvider, RoleProvider, CachingUserProviderMXBean {

  private static final String LTI_LEARNER_ROLE = "Learner";

  private static final String LTI_INSTRUCTOR_ROLE = "Instructor";

  public static final String PROVIDER_NAME = "sakai";

  private static final String OC_USERAGENT = "Opencast";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SakaiUserProviderInstance.class);

  /** The organization */
  private Organization organization = null;

  /** Total number of requests made to load users */
  private AtomicLong requests = null;

  /** The number of requests made to Sakai */
  private AtomicLong sakaiLoads = null;

  /** A cache of users, which lightens the load on Sakai */
  private LoadingCache<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /** The URL of the Sakai instance */
  private String sakaiUrl = null;

  /** The username used to call Sakai REST webservices */
  private String sakaiUsername = null;

  /** The password of the user used to call Sakai REST webservices */
  private String sakaiPassword = null;

  /** Regular expression for matching valid sites */
  private String sitePattern;

  /** Regular expression for matching valid users */
  private String userPattern;

  /** A map of roles which are regarded as Instructor roles */
  private Set<String> instructorRoles;

  /**
   * Constructs an Sakai user provider with the needed settings.
   *
   * @param pid
   *          the pid of this service
   * @param organization
   *          the organization
   * @param url
   *          the url of the Sakai server
   * @param userName
   *          the user to authenticate as
   * @param password
   *          the user credentials
   * @param cacheSize
   *          the number of users to cache
   * @param cacheExpiration
   *          the number of minutes to cache users
   */
  public SakaiUserProviderInstance(String pid, Organization organization, String url, String userName, String password,
          String sitePattern, String userPattern, Set<String> instructorRoles, int cacheSize, int cacheExpiration) {

    this.organization = organization;
    this.sakaiUrl = url;
    this.sakaiUsername = userName;
    this.sakaiPassword = password;
    this.sitePattern = sitePattern;
    this.userPattern = userPattern;
    this.instructorRoles = instructorRoles;

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

    logger.info("Creating new SakaiUserProviderInstance(pid={}, url={}, cacheSize={}, cacheExpiration={})",
                 pid, url, cacheSize, cacheExpiration);

    // Setup the caches
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Object>() {
              @Override
              public Object load(String id) throws Exception {
                User user = loadUserFromSakai(id);
                return user == null ? nullToken : user;
              }
            });

    registerMBean(pid);
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * Registers an MXBean.
   */
  protected void registerMBean(String pid) {
    // register with jmx
    requests = new AtomicLong();
    sakaiLoads = new AtomicLong();
    try {
      ObjectName name;
      name = SakaiUserProviderFactory.getObjectName(pid);
      Object mbean = this;
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      try {
        mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
        logger.debug(name + " was not registered");
      }
      mbs.registerMBean(mbean, name);
    } catch (Exception e) {
      logger.error("Unable to register {} as an mbean: {}", this, e);
    }
  }

  // UserProvider methods

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization.getId();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    logger.debug("loaduser(" + userName + ")");

    try {
      if ((userPattern != null) && !userName.matches(userPattern)) {
        logger.debug("load user {} failed regexp {}", userName, userPattern);
        return null;
      }
    } catch (PatternSyntaxException e) {
      logger.warn("Invalid regular expression for user pattern {} - disabling checks", userPattern);
      userPattern = null;
    }

    requests.incrementAndGet();
    try {
      Object user = cache.getUnchecked(userName);
      if (user == nullToken) {
        logger.debug("Returning null user from cache");
        return null;
      } else {
        logger.debug("Returning user " + userName + " from cache");
        return (JaxbUser) user;
      }
    } catch (ExecutionError e) {
      logger.warn("Exception while loading user {}", userName, e);
      return null;
    } catch (UncheckedExecutionException e) {
      logger.warn("Exception while loading user {}", userName, e);
      return null;
    }
  }

  /**
   * Loads a user from Sakai.
   * 
   * @param userName
   *          the username
   * @return the user
   */
  protected User loadUserFromSakai(String userName) {

    if (cache == null) {
      throw new IllegalStateException("The Sakai user detail service has not yet been configured");
    }

    // Don't answer for admin, anonymous or empty user
    if ("admin".equals(userName) || "".equals(userName) || "anonymous".equals(userName)) {
      cache.put(userName, nullToken);
      logger.debug("we don't answer for: " + userName);
      return null;
    }

    logger.debug("In loadUserFromSakai, currently processing user : {}", userName);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

    // update cache statistics
    sakaiLoads.incrementAndGet();

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassloader = currentThread.getContextClassLoader();
    try {

      // Sakai userId (internal id), email address and display name
      String[] sakaiUser = getSakaiUser(userName);

      if (sakaiUser == null) {
        // user not known to this provider
        logger.debug("User {} not found in Sakai system", userName);
        cache.put(userName, nullToken);
        return null;
      }

      String userId = sakaiUser[0];
      String email = sakaiUser[1];
      String displayName = sakaiUser[2];

      // Get the set of Sakai roles for the user
      String[] sakaiRoles = getRolesFromSakai(userId);

      // if Sakai doesn't know about this user we need to return
      if (sakaiRoles == null) {
        cache.put(userName, nullToken);
        return null;
      }

      logger.debug("Sakai roles for eid " + userName + " id " + userId + ": " + Arrays.toString(sakaiRoles));

      Set<JaxbRole> roles = new HashSet<JaxbRole>();

      boolean isInstructor = false;

      for (String r : sakaiRoles) {
        roles.add(new JaxbRole(r, jaxbOrganization, "Sakai external role", Role.Type.EXTERNAL));

        if (r.endsWith(LTI_INSTRUCTOR_ROLE))
          isInstructor = true;
      }

      // Group role for all Sakai users
      roles.add(new JaxbRole(Group.ROLE_PREFIX + "SAKAI", jaxbOrganization, "Sakai Users", Role.Type.EXTERNAL_GROUP));

      // Group role for Sakai users who are an instructor in one more sites
      if (isInstructor)
        roles.add(new JaxbRole(Group.ROLE_PREFIX + "SAKAI_INSTRUCTOR", jaxbOrganization, "Sakai Instructors", Role.Type.EXTERNAL_GROUP));

      logger.debug("Returning JaxbRoles: " + roles);

      // JaxbUser(String userName, String password, String name, String email, String provider, boolean canLogin, JaxbOrganization organization, Set<JaxbRole> roles)
      User user = new JaxbUser(userName, null, displayName, email, PROVIDER_NAME, true, jaxbOrganization, roles);

      cache.put(userName, user);
      logger.debug("Returning user {}", userName);

      return user;

    } finally {
      currentThread.setContextClassLoader(originalClassloader);
    }

  }

  /*
   ** Verify that the user exists
   ** Query with /direct/user/:ID:/exists
   */
  private boolean verifySakaiUser(String userId) {

      logger.debug("verifySakaiUser({})", userId);

      try {
        if ((userPattern != null) && !userId.matches(userPattern)) {
          logger.debug("verify user {} failed regexp {}", userId, userPattern);
          return false;
        }
      } catch (PatternSyntaxException e) {
        logger.warn("Invalid regular expression for user pattern {} - disabling checks", userPattern);
        userPattern = null;
      }

      int code;

      try {
          // This webservice does not require authentication
          URL url = new URL(sakaiUrl + "/direct/user/" + userId + "/exists");

          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setRequestMethod("GET");
          connection.setRequestProperty("User-Agent", OC_USERAGENT);

          connection.connect();
          code = connection.getResponseCode();
      } catch (Exception e) {
          logger.warn("Exception verifying Sakai user " + userId + " at " + sakaiUrl + ": " + e.getMessage());
          return false;
      }

      // HTTP OK 200 for site exists, return false for everything else (typically 404 not found)
      return (code == 200);
  }

  /*
   ** Verify that the site exists
   ** Query with /direct/site/:ID:/exists
   */
  private boolean verifySakaiSite(String siteId) {

      // We could additionally cache positive and negative siteId lookup results here

      logger.debug("verifySakaiSite(" + siteId + ")");

      try {
        if ((sitePattern != null) && !siteId.matches(sitePattern)) {
          logger.debug("verify site {} failed regexp {}", siteId, sitePattern);
          return false;
        }
      } catch (PatternSyntaxException e) {
        logger.warn("Invalid regular expression for site pattern {} - disabling checks", sitePattern);
        sitePattern = null;
      }

      int code;

      try {
          // This webservice does not require authentication
          URL url = new URL(sakaiUrl + "/direct/site/" + siteId + "/exists");

          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setRequestMethod("GET");
          connection.setRequestProperty("User-Agent", OC_USERAGENT);

          connection.connect();
          code = connection.getResponseCode();
      } catch (Exception e) {
          logger.warn("Exception verifying Sakai site " + siteId + " at " + sakaiUrl + ": " + e.getMessage());
          return false;
      }

      // HTTP OK 200 for site exists, return false for everything else (typically 404 not found)
      return (code == 200);
  }

  private String[] getRolesFromSakai(String userId) {
    logger.debug("getRolesFromSakai(" + userId + ")");
    try {

      URL url = new URL(sakaiUrl + "/direct/membership/fastroles/" + userId + ".xml" + "?__auth=basic");
      String encoded = Base64.encodeBase64String((sakaiUsername + ":" + sakaiPassword).getBytes("utf8"));

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", "Basic " + encoded);
      connection.setRequestProperty("User-Agent", OC_USERAGENT);

      String xml = IOUtils.toString(new BufferedInputStream(connection.getInputStream()));
      logger.debug(xml);

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();

      Document document = parser.parse(new org.xml.sax.InputSource(new StringReader(xml)));

      Element root = document.getDocumentElement();
      NodeList nodes = root.getElementsByTagName("membership");
      List<String> roleList = new ArrayList<String>();
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item(i);
        // The Role in sakai
        String sakaiRole = getTagValue("memberRole", element);

        // the location in sakai e.g. /site/admin
        String sakaiLocationReference = getTagValue("locationReference", element);
        // we don't do the sakai admin role
        if ("/site/!admin".equals(sakaiLocationReference)) {
          continue;
        }

        String opencastRole = buildOpencastRole(sakaiLocationReference, sakaiRole);
        roleList.add(opencastRole);
      }

      return roleList.toArray(new String[0]);

    } catch (FileNotFoundException fnf) {
      // if the return is 404 it means the user wasn't found
      logger.debug("user id " + userId + " not found on " + sakaiUrl);
    } catch (Exception e) {
      logger.warn("Exception getting site/role membership for Sakai user {} at {}: {}", userId, sakaiUrl, e.getMessage());
    }

    return null;
  }

  /**
   * Get the internal Sakai user Id for the supplied user. If the user exists, set the user's email address.
   * 
   * @param eid
   * @return
   */
  private String[] getSakaiUser(String eid) {

    try {

      URL url = new URL(sakaiUrl + "/direct/user/" + eid + ".xml" + "?__auth=basic");
      logger.debug("Sakai URL: " + sakaiUrl);
      String encoded = Base64.encodeBase64String((sakaiUsername + ":" + sakaiPassword).getBytes("utf8"));
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", "Basic " + encoded);
      connection.setRequestProperty("User-Agent", OC_USERAGENT);

      String xml = IOUtils.toString(new BufferedInputStream(connection.getInputStream()));
      logger.debug(xml);

      // Parse the document
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
      Document document = parser.parse(new org.xml.sax.InputSource(new StringReader(xml)));
      Element root = document.getDocumentElement();

      String sakaiID = getTagValue("id", root);
      String sakaiEmail = getTagValue("email", root);
      String sakaiDisplayName = getTagValue("displayName", root);

      return new String[]{sakaiID, sakaiEmail, sakaiDisplayName};

    } catch (FileNotFoundException fnf) {
      logger.debug("user {} does not exist on Sakai system: {}", eid, fnf);
    } catch (Exception e) {
      logger.warn("Exception getting Sakai user information for user {} at {}: {}", eid, sakaiUrl, e);
    }

    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.CachingUserProviderMXBean#getCacheHitRatio()
   */
  @Override
  public float getCacheHitRatio() {
    if (requests.get() == 0) {
      return 0;
    }
    return (float) (requests.get() - sakaiLoads.get()) / requests.get();
  }

  /**
   * Build a Opencast role "foo_user" from the given Sakai locations
   * 
   * @param sakaiLocationReference
   * @param sakaiRole
   * @return
   */
  private String buildOpencastRole(String sakaiLocationReference, String sakaiRole) {

    // we need to parse the site id from the reference
    String siteId = sakaiLocationReference.substring(sakaiLocationReference.indexOf("/", 2) + 1);

    // map Sakai role to LTI role
    String ltiRole = instructorRoles.contains(sakaiRole) ? LTI_INSTRUCTOR_ROLE : LTI_LEARNER_ROLE;

    return siteId + "_" + ltiRole;
  }

  /**
   * Get a value for for a tag in the element
   * 
   * @param sTag
   * @param eElement
   * @return
   */
  private static String getTagValue(String sTag, Element eElement) {
    if (eElement.getElementsByTagName(sTag) == null)
      return null;

    NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
    Node nValue = nlList.item(0);
    return (nValue != null) ? nValue.getNodeValue() : null;
  }

  @Override
  public Iterator<User> findUsers(String query, int offset, int limit) {

    if (query == null)
      throw new IllegalArgumentException("Query must be set");

    if (query.endsWith("%")) {
      query = query.substring(0, query.length() - 1);
    }

    if (query.isEmpty()) {
      return Collections.emptyIterator();
    }

    // Verify if a user exists (non-wildcard searches only)
    if (!verifySakaiUser(query)) {
      return Collections.emptyIterator();
    }

    List<User> users = new LinkedList<User>();
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    JaxbUser queryUser = new JaxbUser(query, PROVIDER_NAME, jaxbOrganization, new HashSet<JaxbRole>());
    users.add(queryUser);

    return users.iterator();
  }

  @Override
  public Iterator<User> getUsers() {
    // We never enumerate all users
    return Collections.emptyIterator();
  }

  @Override
  public void invalidate(String userName) {
    cache.invalidate(userName);
  }

  @Override
  public long countUsers() {
    // Not meaningful, as we never enumerate users
    return 0;
  }

  // RoleProvider methods

   @Override
   public Iterator<Role> getRoles() {

     // We won't ever enumerate all Sakai sites, so return an empty list here
     return Collections.emptyIterator();
   }

   @Override
   public List<Role> getRolesForUser(String userName) {

      List<Role> roles = new LinkedList<Role>();

      // Don't answer for admin, anonymous or empty user
      if ("admin".equals(userName) || "".equals(userName) || "anonymous".equals(userName)) {
         logger.debug("we don't answer for: " + userName);
         return roles;
      }

      logger.debug("getRolesForUser(" + userName + ")");

      User user = loadUser(userName);
      if (user != null) {
        logger.debug("Returning cached roleset for {}", userName);
        return new ArrayList<Role>(user.getRoles());
      }

     // Not found
     logger.debug("Return empty roleset for {} - not found on Sakai");
     return new LinkedList<Role>();
   }

   @Override
   public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {

     // We search for SITEID, SITEID_Learner, SITEID_Instructor

     logger.debug("findRoles(query=" + query + " offset=" + offset + " limit=" + limit + ")");

     // Don't return roles for users or groups
     if (target == Role.Target.USER) {
        return Collections.emptyIterator();
     }

     boolean exact = true;
     boolean ltirole = false;

     if (query.endsWith("%")) {
       exact = false;
       query = query.substring(0, query.length() - 1);
     }

     if (query.isEmpty()) {
        return Collections.emptyIterator();
     }

     // Verify that role name ends with LTI_LEARNER_ROLE or LTI_INSTRUCTOR_ROLE
     if (exact && !query.endsWith("_" + LTI_LEARNER_ROLE) && !query.endsWith("_" + LTI_INSTRUCTOR_ROLE)) {
        return Collections.emptyIterator();
     }

     String sakaiSite = null;

     if (query.endsWith("_" + LTI_LEARNER_ROLE)) {
       sakaiSite = query.substring(0, query.lastIndexOf("_" + LTI_LEARNER_ROLE));
       ltirole = true;
     } else if (query.endsWith("_" + LTI_INSTRUCTOR_ROLE)) {
       sakaiSite = query.substring(0, query.lastIndexOf("_" + LTI_INSTRUCTOR_ROLE));
       ltirole = true;
     }

     if (!ltirole) {
       sakaiSite = query;
     }

     if (!verifySakaiSite(sakaiSite)) {
        return Collections.emptyIterator();
     }

     // Roles list
     List<Role> roles = new LinkedList<Role>();

     JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);

     if (ltirole) {
       // Query is for a Site ID and an LTI role (Instructor/Learner)
       roles.add(new JaxbRole(query, jaxbOrganization, "Sakai Site Role", Role.Type.EXTERNAL));
     } else {
       // Site ID - return both roles
       roles.add(new JaxbRole(sakaiSite + "_" + LTI_INSTRUCTOR_ROLE, jaxbOrganization, "Sakai Site Instructor Role", Role.Type.EXTERNAL));
       roles.add(new JaxbRole(sakaiSite + "_" + LTI_LEARNER_ROLE, jaxbOrganization, "Sakai Site Learner Role", Role.Type.EXTERNAL));
     }

     return roles.iterator();
   }

}
