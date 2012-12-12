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
package org.opencastproject.dataloader;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.jpa.JpaUser;
import org.opencastproject.userdirectory.jpa.JpaUserAndRoleProvider;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A data loader to populate the series and JPA user provider with sample data.
 */
public class UserAndSeriesLoader {

  /** The second tenant */
  public static final String TENANT1 = "tenant1";

  /** The number of series to load */
  public static final int NUM_SERIES = 10;

  /** The number of students per series to load */
  public static final int STUDENTS_PER_SERIES = 20;

  /** The number of instructors per series to load */
  public static final int INSTRUCTORS_PER_SERIES = 2;

  /** The number of admins per series to load */
  public static final int ADMINS_PER_SERIES = 1;

  /** The series prefix */
  public static final String SERIES_PREFIX = "SERIES_";

  /** The user role */
  public static final String USER_ROLE = "ROLE_USER";

  /** The instructor role */
  public static final String INSTRUCTOR_ROLE = "ROLE_INSTRUCTOR";

  /** The course admin role */
  public static final String COURSE_ADMIN_ROLE = "ROLE_COURSE_ADMIN";

  /** The student role suffix */
  public static final String STUDENT_PREFIX = "STUDENT";

  /** The instructor role suffix */
  public static final String INSTRUCTOR_PREFIX = "INSTRUCTOR";

  /** The departmental admin (not the super admin) role suffix */
  public static final String ADMIN_PREFIX = "ADMIN";

  /** The read permission */
  public static final String READ = "read";

  /** The write permission */
  public static final String WRITE = "write";

  /** The contribute permission */
  public static final String CONTRIBUTE = "contribute";

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(UserAndSeriesLoader.class);

  /** The series service */
  protected SeriesService seriesService = null;

  /** The JPA-based user provider, which includes an addUser() method */
  protected JpaUserAndRoleProvider jpaUserProvider = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The organization directory */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Callback on component activation.
   */
  protected void activate(ComponentContext cc) {

    String loadUsers = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.loadusers"));

    // Load the demo users, if necessary
    if (Boolean.valueOf(loadUsers)) {
      // Load 100 series and 1000 users, but don't block activation
      new Loader().start();
    }
  }

  protected class Loader extends Thread {
    @Override
    public void run() {
      logger.info("Adding sample series...");
      String[] organizationIds = new String[] { DefaultOrganization.DEFAULT_ORGANIZATION_ID, TENANT1 };

      for (int i = 1; i <= NUM_SERIES; i++) {
        String seriesId = SERIES_PREFIX + i;
        DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
        AccessControlList acl = new AccessControlList();

        // Add read permissions for viewing the series content in engage
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + STUDENT_PREFIX, READ, true));
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, READ, true));
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, READ, true));

        // Add contribute permissions for adding recordings to these series
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, CONTRIBUTE, true));
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, CONTRIBUTE, true));

        // Add write permissions for the instructors and admins to make changes to the series themselves
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + INSTRUCTOR_PREFIX, WRITE, true));
        acl.getEntries().add(new AccessControlEntry(SERIES_PREFIX + i + "_" + ADMIN_PREFIX, WRITE, true));

        try {
          dc.set(DublinCore.PROPERTY_IDENTIFIER, seriesId);
          dc.set(DublinCore.PROPERTY_TITLE, "Series #" + i);
          dc.set(DublinCore.PROPERTY_CREATOR, "Creator #" + i);
          dc.set(DublinCore.PROPERTY_CONTRIBUTOR, "Contributor #" + i);

          for (String orgId : organizationIds) {
            Organization org = organizationDirectoryService.getOrganization(orgId);
            try {
              securityService.setUser(new User("userandseriesloader", orgId,
                      new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE }));
              securityService.setOrganization(org);

              try {
                // Test if the serie already exist, it does not overwrite it.
                if (seriesService.getSeries(seriesId) != null)
                  continue;
              } catch (NotFoundException e) {
                // If the series does not exist, we create it.
                seriesService.updateSeries(dc);
                seriesService.updateAccessControl(seriesId, acl);
              }
            } catch (UnauthorizedException e) {
              logger.warn(e.getMessage());
            } catch (SeriesException e) {
              logger.warn("Unable to create series {}", dc);
            } catch (NotFoundException e) {
              logger.warn("Unable to find series {}", dc);
            } finally {
              securityService.setOrganization(null);
              securityService.setUser(null);
            }
          }
          logger.debug("Added series {}", dc);
        } catch (NotFoundException e) {
          logger.warn("Unable to find organization {}", e.getMessage());
        }
      }

      load(STUDENT_PREFIX, 20, new String[] { USER_ROLE }, DefaultOrganization.DEFAULT_ORGANIZATION_ID);
      load(STUDENT_PREFIX, 20, new String[] { USER_ROLE }, TENANT1);

      load(INSTRUCTOR_PREFIX, 2, new String[] { USER_ROLE, INSTRUCTOR_ROLE },
              DefaultOrganization.DEFAULT_ORGANIZATION_ID);
      load(INSTRUCTOR_PREFIX, 2, new String[] { USER_ROLE, INSTRUCTOR_ROLE }, TENANT1);

      load(ADMIN_PREFIX, 1, new String[] { USER_ROLE, COURSE_ADMIN_ROLE }, DefaultOrganization.DEFAULT_ORGANIZATION_ID);
      load(ADMIN_PREFIX, 1, new String[] { USER_ROLE, COURSE_ADMIN_ROLE }, TENANT1);

      loadLdapUser(DefaultOrganization.DEFAULT_ORGANIZATION_ID);
      loadLdapUser(TENANT1);

      logger.info("Finished loading sample series and users");
    }
  }

  /**
   * Loads demo users into persistence.
   * 
   * @param rolePrefix
   *          the role prefix
   * @param numPerSeries
   *          the number of users to load per series
   * @param additionalRoles
   *          any additional roles to add for each user
   * @param orgId
   *          the organization id
   */
  protected void load(String rolePrefix, int numPerSeries, String[] additionalRoles, String orgId) {
    String lowerCasePrefix = rolePrefix.toLowerCase();
    int totalUsers = numPerSeries * NUM_SERIES;

    logger.info("Adding sample {}s, usernames and passwords are {}1/{}1... {}{}/{}{}", new Object[] { lowerCasePrefix,
            lowerCasePrefix, lowerCasePrefix, lowerCasePrefix, totalUsers, lowerCasePrefix, totalUsers });

    for (int i = 1; i <= totalUsers; i++) {
      if (jpaUserProvider.loadUser(lowerCasePrefix + i, orgId) == null) {
        Set<String> roleSet = new HashSet<String>();
        for (String additionalRole : additionalRoles) {
          roleSet.add(additionalRole);
        }
        roleSet.add(SERIES_PREFIX + (((i - 1) % NUM_SERIES) + 1) + "_" + rolePrefix);
        JpaUser user = new JpaUser(lowerCasePrefix + i, lowerCasePrefix + i, orgId, roleSet);
        try {
          jpaUserProvider.addUser(user);
          logger.debug("Added {}", user);
        } catch (Exception e) {
          logger.warn("Can not add {}: {}", user, e);
        }
      }
    }
  }

  /**
   * Load a user for testing the ldap provider
   * 
   * @param organizationId
   *          the organization
   */
  protected void loadLdapUser(String organizationId) {
    Set<String> ldapUserRoles = new HashSet<String>();
    ldapUserRoles.add(USER_ROLE);
    // This is the public identifier for Josh Holtzman in the UC Berkeley Directory, which is available for anonymous
    // binding.
    String ldapUserId = "231693";

    if (jpaUserProvider.loadUser(ldapUserId, organizationId) == null) {
      jpaUserProvider.addUser(new JpaUser(ldapUserId, "ldap", organizationId, ldapUserRoles));
      logger.debug("Added ldap user '{}' into organization '{}'", ldapUserId, organizationId);
    }
  }

  /**
   * @param jpaUserProvider
   *          the jpaUserProvider to set
   */
  public void setJpaUserProvider(JpaUserAndRoleProvider jpaUserProvider) {
    this.jpaUserProvider = jpaUserProvider;
  }

  /**
   * @param seriesService
   *          the seriesService to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

}
