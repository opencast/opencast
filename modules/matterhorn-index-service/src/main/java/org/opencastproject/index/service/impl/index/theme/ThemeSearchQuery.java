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

package org.opencastproject.index.service.impl.index.theme;

import org.opencastproject.matterhorn.search.SearchTerms;
import org.opencastproject.matterhorn.search.impl.AbstractSearchQuery;
import org.opencastproject.security.api.User;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This interface defines a fluent api for a query object used to lookup themes in the search index.
 */
public class ThemeSearchQuery extends AbstractSearchQuery {

  protected List<Long> identifiers = new ArrayList<Long>();
  private User user = null;
  private String organization = null;
  private String creator = null;
  private Date createdFrom = null;
  private Date createdTo = null;
  private Boolean isDefault = null;
  private String description = null;
  private String name = null;
  private Boolean bumperActive = null;
  private String bumperFile = null;
  private Boolean licenseSlideActive = null;
  private String licenseSlideBackground = null;
  private String licenseSlideDescription = null;
  private Boolean trailerActive = null;
  private String trailerFile = null;
  private Boolean titleSlideActive = null;
  private String titleSlideBackground = null;
  private String titleSlideMetadata = null;
  private Boolean watermarkActive = null;
  private String watermarkFile = null;
  private String watermarkPosition = null;

  @SuppressWarnings("unused")
  private ThemeSearchQuery() {
  }

  /**
   * Creates a query that will return theme documents. The user's organization must match the query's organization.
   *
   * @param organization
   *          The organization to run this query with. Cannot be null.
   * @param user
   *          The user to run this query as. Cannot be null.
   * @throws IllegalStateException
   *           Thrown if the current user's organization doesn't match the search organization, the organization is
   *           null, or user is null.
   */
  public ThemeSearchQuery(String organization, User user) {
    super(Theme.DOCUMENT_TYPE);

    if (organization == null)
      throw new IllegalStateException("The organization for this query was null.");

    if (user == null)
      throw new IllegalStateException("The user for this query was null.");

    this.organization = organization;
    this.user = user;
    if (!user.getOrganization().getId().equals(organization))
      throw new IllegalStateException("User's organization must match search organization");
  }

  /**
   * Selects themes with the given identifier.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple themes.
   *
   * @param id
   *          the theme identifier
   * @return the enhanced search query
   */
  public ThemeSearchQuery withIdentifier(long id) {
    this.identifiers.add(id);
    return this;
  }

  /**
   * Returns the list of theme identifiers or an empty array if no identifiers have been specified.
   *
   * @return the identifiers
   */
  public Long[] getIdentifiers() {
    return identifiers.toArray(new Long[identifiers.size()]);
  }

  /**
   * Selects themes with the given description.
   *
   * @param description
   *          the description
   * @return the enhanced search query
   */
  public ThemeSearchQuery withDescription(String description) {
    clearExpectations();
    this.description = description;
    return this;
  }

  /**
   * Returns the description of the theme query.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the organization of the theme query.
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
   * Selects themes with the given creator.
   *
   * @param creator
   *          the creator
   * @return the enhanced search query
   */
  public ThemeSearchQuery withCreator(String creator) {
    clearExpectations();
    this.creator = creator;
    return this;
  }

  /**
   * Returns the creator of the themes.
   *
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * The created date to start looking for themes.
   *
   * @param createdFrom
   *          The created date to start looking for themes
   * @return the enhanced search query
   */
  public ThemeSearchQuery withCreatedFrom(Date createdFrom) {
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
  public ThemeSearchQuery withCreatedTo(Date createdTo) {
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
   * Selects themes that are default.
   *
   * @param isDefault
   *          Whether to search for themes that are default or not.
   * @return the enhanced search query
   */
  public ThemeSearchQuery withIsDefault(Boolean isDefault) {
    clearExpectations();
    this.isDefault = isDefault;
    return this;
  }

  /**
   * Returns whether the theme query is searching for default themes.
   *
   * @return whether the search is looking for default themes.
   */
  public Boolean getIsDefault() {
    return isDefault;
  }

  /**
   * Selects themes with the given name.
   *
   * @param name
   *          the name
   * @return the enhanced search query
   */
  public ThemeSearchQuery withName(String name) {
    clearExpectations();
    this.name = name;
    return this;
  }

  /**
   * Returns the name looked for by the theme query.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Selects themes with where the bumper is active.
   *
   * @param bumperActive
   *          Whether the bumperActive
   * @return the enhanced search query
   */
  public ThemeSearchQuery withBumperActive(Boolean bumperActive) {
    clearExpectations();
    this.bumperActive = bumperActive;
    return this;
  }

  /**
   * Returns whether the theme query is searching for themes where the bumper is active.
   *
   * @return the bumperActive
   */
  public Boolean getBumperActive() {
    return bumperActive;
  }

  /**
   * Selects themes with the given bumper file id.
   *
   * @param bumperFile
   *          the bumper file id
   * @return the enhanced search query
   */
  public ThemeSearchQuery withBumperFile(String bumperFile) {
    clearExpectations();
    this.bumperFile = bumperFile;
    return this;
  }

  /**
   * Returns the bumperFile of the theme query.
   *
   * @return the bumperFile
   */
  public String getBumperFile() {
    return bumperFile;
  }

  /**
   * Selects themes where the license slide is active.
   *
   * @param licenseSlideActive
   *          If the license slide is active or not.
   * @return the enhanced search query
   */
  public ThemeSearchQuery withLicenseSlideActive(Boolean licenseSlideActive) {
    clearExpectations();
    this.licenseSlideActive = licenseSlideActive;
    return this;
  }

  /**
   * Returns whether the theme query is looking for themes where the license slide is active.
   *
   * @return the license
   */
  public Boolean getLicenseSlideActive() {
    return licenseSlideActive;
  }

  /**
   * Selects themes with the given license slide background.
   *
   * @param licenseSlideBackground
   *          the license slide background file
   * @return the enhanced search query
   */
  public ThemeSearchQuery withLicenseSlideBackground(String licenseSlideBackground) {
    clearExpectations();
    this.licenseSlideBackground = licenseSlideBackground;
    return this;
  }

  /**
   * Returns the license slide background this query is looking for.
   *
   * @return the licenseSlideBackground
   */
  public String getLicenseSlideBackground() {
    return licenseSlideBackground;
  }

  /**
   * Selects themes with the given license slide description.
   *
   * @param licenseSlideDescription
   *          the license slide description
   * @return the enhanced search query
   */
  public ThemeSearchQuery withLicenseSlideDescription(String licenseSlideDescription) {
    clearExpectations();
    this.licenseSlideDescription = licenseSlideDescription;
    return this;
  }

  /**
   * Returns the license slide description this query is looking for.
   *
   * @return the license slide description
   */
  public String getLicenseSlideDescription() {
    return licenseSlideDescription;
  }

  /**
   * Selects themes that have the trailer active
   *
   * @param trailerActive
   *          whether the trailer is active or not
   * @return the enhanced search query
   */
  public ThemeSearchQuery withTrailerActive(Boolean trailerActive) {
    clearExpectations();
    this.trailerActive = trailerActive;
    return this;
  }

  /**
   * Returns whether the query is looking for themes where the trailer is active
   *
   * @return Whether the query is looking for themes where the trailer is active
   */
  public Boolean getTrailerActive() {
    return trailerActive;
  }

  /**
   * Selects themes with the given trailer file
   *
   * @param trailerFile
   *          the trailer file that should be in the themes
   * @return the enhanced search query
   */
  public ThemeSearchQuery withTrailerFile(String trailerFile) {
    clearExpectations();
    this.trailerFile = trailerFile;
    return this;
  }

  /**
   * Returns the trailer file id that is being matched in this query
   *
   * @return the trailer file id
   */
  public String getTrailerFile() {
    return trailerFile;
  }

  /**
   * Selects themes where the title slide is active.
   *
   * @param titleSlideActive
   *          Whether to search for themes where the title slide is active
   * @return the enhanced search query
   */
  public ThemeSearchQuery withTitleSlideActive(Boolean titleSlideActive) {
    clearExpectations();
    this.titleSlideActive = titleSlideActive;
    return this;
  }

  /**
   * Returns whether this query is searching for themes where the title slide is active.
   *
   * @return Whether the query is looking for themes where the title slide is active
   */
  public Boolean getTitleSlideActive() {
    return titleSlideActive;
  }

  /**
   * Selects themes with matching title slide metadata
   *
   * @param titleSlideMetadata
   *          Search themes for this title slide metadata
   * @return the enhanced search query
   */
  public ThemeSearchQuery withTitleSlideMetadata(String titleSlideMetadata) {
    clearExpectations();
    this.titleSlideMetadata = titleSlideMetadata;
    return this;
  }

  /**
   * Returns the title slide metadata this query is searching for
   *
   * @return the title slide metadata being searched for
   */
  public String getTitleSlideMetadata() {
    return titleSlideMetadata;
  }

  /**
   * Selects themes with the given title slide background id.
   *
   * @param titleSlideBackground
   *          the id for the title slide background file.
   * @return the enhanced search query
   */
  public ThemeSearchQuery withTitleSlideBackground(String titleSlideBackground) {
    clearExpectations();
    this.titleSlideBackground = titleSlideBackground;
    return this;
  }

  /**
   * @return Returns the title slide background id this query is searching for.
   */
  public String getTitleSlideBackground() {
    return titleSlideBackground;
  }

  /**
   * Selects themes where a watermark is active
   *
   * @param watermarkActive
   *          Whether to search for themes where the watermark is active
   * @return the enhanced search query
   */
  public ThemeSearchQuery withWatermarkActive(Boolean watermarkActive) {
    clearExpectations();
    this.watermarkActive = watermarkActive;
    return this;
  }

  /**
   * @return Returns whether this query is searching for themes where the watermark is active.
   */
  public Boolean getWatermarkActive() {
    return watermarkActive;
  }

  /**
   * Selects themes with the given watermark file
   *
   * @param watermarkFile
   *          the id of the watermark file
   * @return the enhanced search query
   */
  public ThemeSearchQuery withWatermarkFile(String watermarkFile) {
    clearExpectations();
    this.watermarkFile = watermarkFile;
    return this;
  }

  /**
   * @return Returns the watermark file id this query is searching for.
   */
  public String getWatermarkFile() {
    return watermarkFile;
  }

  /**
   * Selects themes with the given watermark position.
   *
   * @param watermarkPosition
   *          the watermark position to select
   * @return the enhanced search query
   */
  public ThemeSearchQuery withLicense(String watermarkPosition) {
    clearExpectations();
    this.watermarkPosition = watermarkPosition;
    return this;
  }

  /**
   * @return Returns the watermark position this query is searching for.
   */
  public String getWatermarkPosition() {
    return watermarkPosition;
  }

  /**
   * Defines the sort order for the theme by creator username.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public ThemeSearchQuery sortByCreator(Order order) {
    withSortOrder(ThemeIndexSchema.CREATOR, order);
    return this;
  }

  /**
   * Returns the sort order for the theme creator username.
   *
   * @return the sort order
   */
  public Order getThemeCreatorSortOrder() {
    return getSortOrder(ThemeIndexSchema.CREATOR);
  }

  /**
   * Defines the sort order for the theme created date & time.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public ThemeSearchQuery sortByCreatedDateTime(Order order) {
    withSortOrder(ThemeIndexSchema.CREATION_DATE, order);
    return this;
  }

  /**
   * Defines the sort order for the theme default property
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public ThemeSearchQuery sortByDefault(Order order) {
    withSortOrder(ThemeIndexSchema.DEFAULT, order);
    return this;
  }

  /**
   * Returns the sort order for the theme created date.
   *
   * @return the sort order
   */
  public Order getSeriesDateSortOrder() {
    return getSortOrder(ThemeIndexSchema.CREATION_DATE);
  }

  /**
   * Defines the sort order for the theme by names.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public ThemeSearchQuery sortByName(Order order) {
    withSortOrder(ThemeIndexSchema.NAME, order);
    return this;
  }

  /**
   * Returns the sort order for the theme name.
   *
   * @return the sort order
   */
  public Order getThemeNameSortOrder() {
    return getSortOrder(ThemeIndexSchema.NAME);
  }

  /**
   * Defines the sort order for the theme by description.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public ThemeSearchQuery sortByDescription(Order order) {
    withSortOrder(ThemeIndexSchema.DESCRIPTION, order);
    return this;
  }

  /**
   * Returns the sort order for the theme name.
   *
   * @return the sort order
   */
  public Order getThemeDescriptionSortOrder() {
    return getSortOrder(ThemeIndexSchema.DESCRIPTION);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(ThemeSearchQuery.class.getSimpleName() + " ");

    if (identifiers.size() > 0) {
      sb.append("ids:'" + identifiers.toString() + "' ");
    }

    if (StringUtils.trimToNull(organization) != null) {
      sb.append("organization:'" + organization + "' ");
    }

    if (StringUtils.trimToNull(creator) != null) {
      sb.append("creator:'" + creator + "' ");
    }

    if (createdFrom != null) {
      sb.append("Created From:'" + createdFrom + "' ");
    }

    if (createdTo != null) {
      sb.append("Created To:'" + createdTo + "' ");
    }

    sb.append("Is Default:'" + isDefault + "' ");

    if (StringUtils.trimToNull(description) != null) {
      sb.append("description:'" + description + "' ");
    }

    if (StringUtils.trimToNull(name) != null) {
      sb.append("name:'" + name + "' ");
    }

    sb.append("bumper active:'" + bumperActive + "' ");

    if (StringUtils.trimToNull(bumperFile) != null) {
      sb.append("bumper file:'" + bumperFile + "' ");
    }

    sb.append("license slide active:'" + licenseSlideActive + "' ");

    if (StringUtils.trimToNull(licenseSlideBackground) != null) {
      sb.append("license slide background:'" + licenseSlideBackground + "' ");
    }

    if (StringUtils.trimToNull(licenseSlideDescription) != null) {
      sb.append("license slide description:'" + licenseSlideDescription + "' ");
    }

    sb.append("trailer active:'" + trailerActive + "' ");

    if (StringUtils.trimToNull(trailerFile) != null) {
      sb.append("trailer file:'" + trailerFile + "' ");
    }

    sb.append("title slide active:'" + titleSlideActive + "' ");

    if (StringUtils.trimToNull(titleSlideBackground) != null) {
      sb.append("title slide background:'" + titleSlideBackground + "' ");
    }

    if (StringUtils.trimToNull(titleSlideMetadata) != null) {
      sb.append("title slide metadata:'" + titleSlideMetadata + "' ");
    }

    sb.append("watermark active:'" + watermarkActive + "' ");

    if (StringUtils.trimToNull(watermarkFile) != null) {
      sb.append("watermark file:'" + watermarkFile + "' ");
    }

    if (StringUtils.trimToNull(watermarkPosition) != null) {
      sb.append("watermark position:'" + watermarkPosition + "' ");
    }

    if (getTerms().size() > 0) {
      sb.append("Text:");
      for (SearchTerms<String> searchTerm : getTerms()) {
        sb.append("'" + searchTerm.getTerms() + "' ");
      }
    }
    return sb.toString();
  }
}
