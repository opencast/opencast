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

package org.opencastproject.index.service.impl.index.series;

import org.opencastproject.matterhorn.search.SearchTerms;
import org.opencastproject.matterhorn.search.impl.AbstractSearchQuery;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This interface defines a fluent api for a query object used to lookup series in the search index.
 */
public class SeriesSearchQuery extends AbstractSearchQuery {

  protected List<String> identifiers = new ArrayList<String>();
  private String title = null;
  private User user = null;
  private String description = null;
  private Set<String> actions = new HashSet<String>();
  private List<String> subjects = new ArrayList<String>();
  private String organization = null;
  private String language = null;
  private String creator = null;
  private String license = null;
  private String accessPolicy = null;
  private String managedAcl = null;
  private List<String> organizers = new ArrayList<String>();
  private List<String> contributors = new ArrayList<String>();
  private List<String> publishers = new ArrayList<String>();
  private Boolean optOut = false;
  private Date createdFrom = null;
  private Date createdTo = null;
  private boolean editOnly = false;
  private String rightsHolder = null;
  private String seriesAbstract = null;
  private Long theme = null;

  @SuppressWarnings("unused")
  private SeriesSearchQuery() {
  }

  /**
   * Creates a query that will return series documents.
   */
  public SeriesSearchQuery(String organization, User user) {
    super(Series.DOCUMENT_TYPE);
    this.organization = organization;
    this.user = user;
    this.actions.add(Permissions.Action.READ.toString());
    if (!user.getOrganization().getId().equals(organization))
      throw new IllegalStateException("User's organization must match search organization");
  }

  /**
   * Selects series with the given identifier.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple series.
   *
   * @param id
   *          the series identifier
   * @return the enhanced search query
   */
  public SeriesSearchQuery withIdentifier(String id) {
    if (StringUtils.isBlank(id))
      throw new IllegalArgumentException("Identifier cannot be null");
    this.identifiers.add(id);
    return this;
  }

  /**
   * Returns the list of series identifiers or an empty array if no identifiers have been specified.
   *
   * @return the identifiers
   */
  public String[] getIdentifier() {
    return identifiers.toArray(new String[identifiers.size()]);
  }

  /**
   * Selects series with the given title.
   *
   * @param title
   *          the title
   * @return the enhanced search query
   */
  public SeriesSearchQuery withTitle(String title) {
    clearExpectations();
    this.title = title;
    return this;
  }

  /**
   * Returns the title of the series.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Filter the series without any action checked.
   *
   * @return the enhanced search query
   */
  public SeriesSearchQuery withoutActions() {
    clearExpectations();
    this.actions.clear();
    return this;
  }

  /**
   * Filter the series with the given action.
   * <p>
   * Note that this method may be called multiple times to support filtering by multiple actions.
   *
   * @param action
   *          the action
   * @return the enhanced search query
   */
  public SeriesSearchQuery withAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("Action cannot be null");
    clearExpectations();
    this.actions.add(action.toString());
    return this;
  }

  /**
   * Returns the list of actions or an empty array if no actions have been specified.
   *
   * @return the actions
   */
  public String[] getActions() {
    return actions.toArray(new String[actions.size()]);
  }

  /**
   * Selects series with the given description.
   *
   * @param description
   *          the description
   * @return the enhanced search query
   */
  public SeriesSearchQuery withDescription(String description) {
    clearExpectations();
    this.description = description;
    return this;
  }

  /**
   * Returns the description of the series.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Selects series with the given subject.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple series.
   *
   * @param subject
   *          the subject
   * @return the enhanced search query
   */
  public SeriesSearchQuery withSubject(String subject) {
    if (StringUtils.isBlank(subject))
      throw new IllegalArgumentException("Subject cannot be null");
    clearExpectations();
    this.subjects.add(subject);
    return this;
  }

  /**
   * Returns the list of recording subjects or an empty array if no subject have been specified.
   *
   * @return the subjects
   */
  public String[] getSubjects() {
    return subjects.toArray(new String[subjects.size()]);
  }

  /**
   * Returns the organization of the series.
   *
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Returns the user of this search query
   *
   * @return the user of this search query
   */
  public User getUser() {
    return user;
  }

  /**
   * Selects series with the given language.
   *
   * @param language
   *          the language
   * @return the enhanced search query
   */
  public SeriesSearchQuery withLanguage(String language) {
    clearExpectations();
    this.language = language;
    return this;
  }

  /**
   * Returns the language of the series.
   *
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Selects series with the given creator.
   *
   * @param creator
   *          the creator
   * @return the enhanced search query
   */
  public SeriesSearchQuery withCreator(String creator) {
    clearExpectations();
    this.creator = creator;
    return this;
  }

  /**
   * Returns the creator of the series.
   *
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Selects series with the given license.
   *
   * @param license
   *          the license
   * @return the enhanced search query
   */
  public SeriesSearchQuery withLicense(String license) {
    clearExpectations();
    this.license = license;
    return this;
  }

  /**
   * Returns the license of the series.
   *
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Selects series with the given access policy.
   *
   * @param accessPolicy
   *          the access policy
   * @return the enhanced search query
   */
  public SeriesSearchQuery withAccessPolicy(String accessPolicy) {
    clearExpectations();
    this.accessPolicy = accessPolicy;
    return this;
  }

  /**
   * Returns the access policy of the series.
   *
   * @return the access policy
   */
  public String getAccessPolicy() {
    return accessPolicy;
  }

  /**
   * Selects series with the given theme.
   *
   * @param theme
   *          the theme
   * @return the enhanced search query
   */
  public SeriesSearchQuery withTheme(long theme) {
    clearExpectations();
    this.theme = theme;
    return this;
  }

  /**
   * Returns the theme of the series.
   *
   * @return the theme
   */
  public Long getTheme() {
    return theme;
  }

  /**
   * Selects series with the given managed ACL name.
   *
   * @param managedAcl
   *          the name of the managed ACL
   * @return the enhanced search query
   */
  public SeriesSearchQuery withManagedAcl(String managedAcl) {
    clearExpectations();
    this.managedAcl = managedAcl;
    return this;
  }

  /**
   * Returns the name of the managed ACL set to the series.
   *
   * @return the name of the managed ACL
   */
  public String getManagedAcl() {
    return managedAcl;
  }

  /**
   * Selects series with the given organizers.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple series.
   *
   * @param organizer
   *          the organizer
   * @return the enhanced search query
   */
  public SeriesSearchQuery withOrganizer(String organizer) {
    if (StringUtils.isBlank(organizer))
      throw new IllegalArgumentException("Organizer cannot be null");
    clearExpectations();
    this.organizers.add(organizer);
    return this;
  }

  /**
   * Returns the list of series organizers or an empty array if no organizers have been specified.
   *
   * @return the organizers
   */
  public String[] getOrganizers() {
    return organizers.toArray(new String[organizers.size()]);
  }

  /**
   * Selects series with the given contributor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple contributors.
   *
   * @param contributor
   *          the contributor
   * @return the enhanced search query
   */
  public SeriesSearchQuery withContributor(String contributor) {
    if (StringUtils.isBlank(contributor)) {
      throw new IllegalArgumentException("Contributor can't be null");
    }
    clearExpectations();
    this.contributors.add(contributor);
    return this;
  }

  /**
   * Returns the list of series contributors or an empty array if no contributor have been specified.
   *
   * @return the contributors
   */
  public String[] getContributors() {
    return contributors.toArray(new String[contributors.size()]);
  }

  /**
   * Select series with the given publishers
   *
   * @param publisher
   *          The publisher to add to the search query.
   * @return This query with the added publisher
   */
  public SeriesSearchQuery withPublisher(String publisher) {
    if (StringUtils.isBlank(publisher)) {
      throw new IllegalArgumentException("Publisher can't be null");
    }
    clearExpectations();
    this.publishers.add(publisher);
    return this;
  }

  /**
   * Returns an array of series publishers or an empty array if no publisher has been specified.
   *
   * @return The publishers
   */
  public String[] getPublishers() {
    return publishers.toArray(new String[publishers.size()]);
  }

  /**
   * Selects series with the given recording status (opted out).
   *
   * @param optedOut
   *          the recording status
   * @return the enhanced search query
   */
  public SeriesSearchQuery withOptedOut(boolean optOut) {
    clearExpectations();
    this.optOut = optOut;
    return this;
  }

  /**
   * Returns the series status (opted out) of the series.
   *
   * @return the series recording status
   */
  public Boolean getOptedOut() {
    return optOut;
  }

  /**
   * The created date to start looking for series.
   *
   * @param createdFrom
   *          The created date to start looking for series
   * @return the enhanced search query
   */
  public SeriesSearchQuery withCreatedFrom(Date createdFrom) {
    this.createdFrom = createdFrom;
    return this;
  }

  /**
   * @return The Date after which all series returned should have been created
   */
  public Date getCreatedFrom() {
    return createdFrom;
  }

  /**
   * The created date to stop looking for series.
   *
   * @param createdTo
   *          The created date to stop looking for series
   * @return the enhanced search query
   */
  public SeriesSearchQuery withCreatedTo(Date createdTo) {
    this.createdTo = createdTo;
    return this;
  }

  /**
   * @return The Date before which all series returned should have been created
   */
  public Date getCreatedTo() {
    return createdTo;
  }

  /**
   * @param edit
   *          True to only get series with edit permissions
   * @return enhanced search query
   */
  public SeriesSearchQuery withEdit(Boolean edit) {
    this.editOnly = edit;
    return this;
  }

  /**
   * @return True to only get series that this user can edit.
   */
  public boolean isEditOnly() {
    return editOnly;
  }

  /**
   * @param rightsHolder
   *          The rights holder to search for
   * @return enhanced query
   */
  public SeriesSearchQuery withRightsHolder(String rightsHolder) {
    this.rightsHolder = rightsHolder;
    return this;
  }

  /**
   * @return The rights holder to search for
   */
  public String getRightsHolder() {
    return rightsHolder;
  }

  /**
   * Defines the sort order for the series by contributors.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public SeriesSearchQuery sortByContributors(Order order) {
    withSortOrder(SeriesIndexSchema.CONTRIBUTORS, order);
    return this;
  }

  /**
   * Returns the sort order for the series created date.
   *
   * @return the sort order
   */
  public Order getSeriesContributorsSortOrder() {
    return getSortOrder(SeriesIndexSchema.CONTRIBUTORS);
  }

  /**
   * @param seriesAbstract
   *          The text to search for in series abstracts
   * @return enhanced search query
   */
  public SeriesSearchQuery withSeriesAbstract(String seriesAbstract) {
    this.seriesAbstract = seriesAbstract;
    return this;
  }

  public String getSeriesAbstract() {
    return seriesAbstract;
  }

  /**
   * Defines the sort order for the managed ACL.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public SeriesSearchQuery sortByManagedAcl(Order order) {
    withSortOrder(SeriesIndexSchema.MANAGED_ACL, order);
    return this;
  }

  /**
   * Returns the sort order for the series managed ACL.
   *
   * @return the sort order
   */
  public Order getSeriesManagedAclSortOrder() {
    return getSortOrder(SeriesIndexSchema.MANAGED_ACL);
  }

  /**
   * Defines the sort order for the series created date & time.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public SeriesSearchQuery sortByCreatedDateTime(Order order) {
    withSortOrder(SeriesIndexSchema.CREATED_DATE_TIME, order);
    return this;
  }

  /**
   * Returns the sort order for the series created date.
   *
   * @return the sort order
   */
  public Order getSeriesDateSortOrder() {
    return getSortOrder(SeriesIndexSchema.CREATED_DATE_TIME);
  }

  /**
   * Defines the sort order for the series by organizers.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public SeriesSearchQuery sortByOrganizers(Order order) {
    withSortOrder(SeriesIndexSchema.ORGANIZERS, order);
    return this;
  }

  /**
   * Returns the sort order for the series organizers.
   *
   * @return the sort order
   */
  public Order getSeriesOrganizersSortOrder() {
    return getSortOrder(SeriesIndexSchema.ORGANIZERS);
  }

  /**
   * Defines the sort order for the series by title.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public SeriesSearchQuery sortByTitle(Order order) {
    withSortOrder(SeriesIndexSchema.TITLE, order);
    return this;
  }

  /**
   * Returns the sort order for the series title.
   *
   * @return the sort order
   */
  public Order getSeriesTitleSortOrder() {
    return getSortOrder(SeriesIndexSchema.TITLE);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(SeriesSearchQuery.class.getSimpleName() + " ");
    if (identifiers.size() > 0) {
      sb.append("ids:'" + identifiers.toString() + "' ");
    }
    if (StringUtils.trimToNull(title) != null) {
      sb.append("title:'" + title + "' ");
    }
    if (StringUtils.trimToNull(description) != null) {
      sb.append("description:'" + description + "' ");
    }
    if (subjects.size() > 0) {
      sb.append("subjects:'" + subjects.toString() + "' ");
    }
    if (StringUtils.trimToNull(organization) != null) {
      sb.append("organization:'" + organization + "' ");
    }
    if (StringUtils.trimToNull(language) != null) {
      sb.append("language:'" + language + "' ");
    }
    if (StringUtils.trimToNull(creator) != null) {
      sb.append("creator:'" + creator + "' ");
    }
    if (StringUtils.trimToNull(license) != null) {
      sb.append("license:'" + license + "' ");
    }
    if (StringUtils.trimToNull(accessPolicy) != null) {
      sb.append("ACL:'" + accessPolicy + "' ");
    }

    if (createdFrom != null) {
      sb.append("Created From:'" + createdFrom + "' ");
    }

    if (createdTo != null) {
      sb.append("Created To:'" + createdTo + "' ");
    }

    if (organizers.size() > 0) {
      sb.append("organizers:'" + organizers.toString() + "' ");
    }

    if (contributors.size() > 0) {
      sb.append("contributors:'" + contributors.toString() + "' ");
    }

    if (publishers.size() > 0) {
      sb.append("publishers:'" + publishers.toString() + "' ");
    }

    sb.append("Opt Out:'" + optOut + "' ");

    if (theme != null) {
      sb.append("Theme:'" + theme + "' ");
    }

    sb.append("Edit:'" + editOnly + "' ");

    if (getTerms().size() > 0) {
      sb.append("Text:");
      for (SearchTerms<String> searchTerm : getTerms()) {
        sb.append("'" + searchTerm.getTerms() + "' ");
      }
    }

    if (StringUtils.trimToNull(rightsHolder) != null) {
      sb.append("Rights Holder:'" + rightsHolder + "' ");
    }

    return sb.toString();

  }

}
