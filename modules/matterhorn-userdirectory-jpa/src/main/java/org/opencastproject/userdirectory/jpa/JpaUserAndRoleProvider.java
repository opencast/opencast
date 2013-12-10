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
package org.opencastproject.userdirectory.jpa;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.PasswordEncoder;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages and locates users using JPA. Note that this also provides a REST endpoint to manage user roles. Since this is
 * not intended to be production code, the REST concerns have not be factored into a separate class. Feel free to
 * refactor.
 */
@RestService(name = "users", title = "Internal user account manager", notes = "This service offers the ability to manage the roles for internal accounts.", abstractText = "Intended for QA and testing, not for production.")
@Path("/")
public class JpaUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserAndRoleProvider.class);

  /** Username constant used in JSON formatted users */
  public static final String USERNAME = "username";

  /** Role constant used in JSON formatted users */
  public static final String ROLES = "roles";

  /** Encoding expected from all inputs */
  public static final String ENCODING = "UTF-8";

  /** The delimiter for the User cache */
  private static final String DELIMITER = ";==;";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** A cache of users, which lightens the load on the SQL server */
  private ConcurrentMap<String, Object> cache = null;

  /** A token to store in the miss cache */
  protected Object nullToken = new Object();

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  protected Map<String, Object> persistenceProperties;

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * Callback for activation of this component.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Setup the caches
    cache = new MapMaker().expireAfterWrite(1, TimeUnit.MINUTES).makeComputingMap(new Function<String, Object>() {
      public Object apply(String id) {
        String[] key = id.split(DELIMITER);
        logger.trace("Loading user '{}':'{}' from database", key[0], key[1]);
        User user = loadUser(key[0], key[1]);
        return user == null ? nullToken : user;
      }
    });

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);
  }

  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  public User loadUser(String userName, String organizationId) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("user");
      q.setParameter("u", userName);
      q.setParameter("o", organizationId);
      JpaUser user = null;
      try {
        user = (JpaUser) q.getSingleResult();
      } catch (NoResultException e) {
        return null;
      }
      Set<String> roles = user.getRoles();
      return new User(userName, user.getPassword(), user.getOrganization(), roles.toArray(new String[roles.size()]));
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    String orgId = securityService.getOrganization().getId();
    Object user = cache.get(userName.concat(DELIMITER).concat(orgId));
    if (user == nullToken) {
      return null;
    } else {
      return (User) user;
    }
  }

  /**
   * A utility class to load the user directory.
   * 
   * @param user
   *          the user object
   */
  public void addUser(JpaUser user) {

    // Create a JPA user with an encoded password.
    String encodedPassword = PasswordEncoder.encode(user.getPassword(), user.getUsername());
    user = new JpaUser(user.getUsername(), encodedPassword, user.getOrganization(), user.getRoles());

    // Then save the user
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.persist(user);
      tx.commit();
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public String[] getRoles() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("roles");
      @SuppressWarnings("unchecked")
      List<String> results = q.getResultList();
      return results.toArray(new String[results.size()]);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return ALL_ORGANIZATIONS;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("users.json")
  @RestQuery(name = "allusers", description = "Returns a list of users", returnDescription = "Returns a JSON representation of the list of user accounts", restParameters = {
          @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  @SuppressWarnings("unchecked")
  public String getUsersAsJson(@QueryParam("limit") int limit, @QueryParam("offset") int offset) throws IOException {
    if (limit < 1) {
      limit = 100;
    }
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("users").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("o", securityService.getOrganization().getId());
      List<JpaUser> jpaUsers = q.getResultList();
      JSONArray jsonArray = new JSONArray();
      for (JpaUser user : jpaUsers) {
        Set<String> roles = user.getRoles();
        jsonArray.add(toJson(new User(user.getUsername(), user.getOrganization(),
                roles.toArray(new String[roles.size()]))));
      }
      return jsonArray.toJSONString();
    } finally {
      if (em != null)
        em.close();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{username}.json")
  @RestQuery(name = "user", description = "Returns a user", returnDescription = "Returns a JSON representation of a user", pathParameters = { @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user account.") })
  public Response getUserAsJson(@PathParam("username") String username) {
    User user = loadUser(username);
    if (user == null) {
      return null;
    } else {
      try {
        return Response.ok(toJson(user).toJSONString()).build();
      } catch (IOException e) {
        return Response.status(BAD_REQUEST).build();
      }
    }
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(name = "roleupdate", description = "Updates a user's roles", returnDescription = "No content", restParameters = @RestParameter(name = "roles", type = TEXT, isRequired = true, description = "The user roles as a json array"), pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "The user roles have been updated.") })
  public Response updateUserFromJson(@PathParam("username") String username, @FormParam("roles") String roles) {
    JSONArray rolesArray = (JSONArray) JSONValue.parse(roles);
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing user
      Query q = em.createNamedQuery("user");
      q.setParameter("u", username);
      q.setParameter("o", securityService.getOrganization().getId());
      JpaUser jpaUser = null;
      try {
        jpaUser = (JpaUser) q.getSingleResult();
        jpaUser.roles.clear();
        for (Object role : rolesArray) {
          jpaUser.roles.add((String) role);
        }
        em.merge(jpaUser);
      } catch (NoResultException e) {
        return null; // this will be translated into a 404
      }
      tx.commit();
      return Response.noContent().build();
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Formats a user object to json.
   * 
   * @param user
   *          the user
   * @return the json representation
   */
  @SuppressWarnings("unchecked")
  protected JSONObject toJson(User user) throws IOException {
    JSONObject json = new JSONObject();
    JSONArray roles = new JSONArray();
    for (String role : user.getRoles()) {
      roles.add(role);
    }
    json.put(USERNAME, user.getUserName());
    json.put(ROLES, roles);
    return json;
  }

  /**
   * Parses a User from a json representation.
   * 
   * @param json
   *          the user as json
   * @return the user
   * @throws IOException
   *           if the json can not be parsed
   */
  protected User fromJson(String json) throws IOException {
    JSONObject jsonObject;
    try {
      jsonObject = (JSONObject) new JSONParser().parse(json);
    } catch (ParseException e) {
      throw new IOException(e);
    }
    String username = (String) jsonObject.get(USERNAME);
    String org = securityService.getOrganization().getId();
    JSONArray roleArray = (JSONArray) jsonObject.get(ROLES);
    String[] roles = new String[roleArray.size()];
    for (int i = 0; i < roleArray.size(); i++) {
      roles[i] = (String) roleArray.get(i);
    }
    return new User(username, org, roles);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public String[] getRolesForUser(String userName) {
    return new String[0];
  }

}
