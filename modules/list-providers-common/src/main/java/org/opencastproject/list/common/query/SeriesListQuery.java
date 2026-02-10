/*
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

package org.opencastproject.list.common.query;

import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListFilter.SourceType;
import org.opencastproject.list.common.provider.AclListProvider;
import org.opencastproject.list.common.provider.ContributorsListProvider;
import org.opencastproject.list.common.provider.SeriesListProvider;
import org.opencastproject.list.util.FiltersUtils;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.Optional;

/**
 * Query for the series list.
 *
 * The following filters can be used:
 * <ul>
 * <li>contributors</li>
 * <li>subject</li>
 * <li>language</li>
 * <li>creator</li>
 * <li>license</li>
 * <li>access policy</li>
 * <li>creation date</li>
 * </ul>
 */
public class SeriesListQuery extends DefaultResourceListQuery {

  public static final String FILTER_ACL_NAME = "managedAcl";
  private static final String FILTER_ACL_LABEL = "FILTERS.SERIES.ACCESS_POLICY.LABEL";

  public static final String FILTER_ACL_PERMISSION_READ_NAME = "acl_permission_read";
  private static final String FILTER_ACL_PERMISSION_READ_LABEL = "FILTERS.SERIES.ACL_PREMISSION_READ.LABEL";

  public static final String FILTER_ACL_PERMISSION_WRITE_NAME = "acl_permission_write";
  private static final String FILTER_ACL_PERMISSION_WRITE_LABEL = "FILTERS.SERIES.ACL_PREMISSION_WRITE.LABEL";

  public static final String FILTER_CONTRIBUTORS_NAME = "contributors";
  private static final String FILTER_CONTRIBUTORS_LABEL = "FILTERS.SERIES.CONTRIBUTORS.LABEL";

  public static final String FILTER_CREATIONDATE_NAME = "CreationDate";
  private static final String FILTER_CREATIONDATE_LABEL = "FILTERS.SERIES.CREATION_DATE.LABEL";

  public static final String FILTER_CREATOR_NAME = "Creator";
  private static final String FILTER_CREATOR_LABEL = "FILTERS.SERIES.CREATOR.LABEL";

  public static final String FILTER_TEXT_NAME = "textFilter";

  public static final String FILTER_LANGUAGE_NAME = "language";
  private static final String FILTER_LANGUAGE_LABEL = "FILTERS.SERIES.LANGUAGE.LABEL";

  public static final String FILTER_LICENSE_NAME = "license";
  private static final String FILTER_LICENSE_LABEL = "FILTERS.SERIES.LICENSE.LABEL";

  public static final String FILTER_ORGANIZERS_NAME = "organizers";
  private static final String FILTER_ORGANIZERS_LABEL = "FILTERS.SERIES.ORGANIZERS.LABEL";

  public static final String FILTER_SUBJECT_NAME = "subject";
  private static final String FILTER_SUBJECT_LABEL = "FILTERS.SERIES.SUBJECT.LABEL";

  public static final String FILTER_TITLE_NAME = "title";
  private static final String FILTER_TITLE_LABEL = "FILTERS.SERIES.TITLE.LABEL";

  public static final String FILTER_READ_ACCESS_NAME = "readAccess";
  public static final String FILTER_READ_ACCESS_LABEL = "FILTERS.SERIES.READ_ACCESS.LABEL";

  public static final String FILTER_WRITE_ACCESS_NAME = "writeAccess";
  public static final String FILTER_WRITE_ACCESS_LABEL = "FILTERS.SERIES.WRITE_ACCESS.LABEL";


  public SeriesListQuery() {
    super();
    this.availableFilters.add(createCreationDateFilter(Optional.<Tuple<Date, Date>> empty()));
    this.availableFilters.add(createOrganizersFilter(Optional.empty()));
    this.availableFilters.add(createContributorsFilter(Optional.empty()));
    this.availableFilters.add(createReadAccessFilter(Optional.empty()));
    this.availableFilters.add(createWriteAccessFilter(Optional.empty()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given acl
   *
   * @param acl
   *          the acl to filter for
   */
  public void withAccessPolicy(String acl) {
    this.addFilter(createAccessPolicyFilter(Optional.ofNullable(acl)));
  }

  /**
   * Returns an {@link Optional} containing the acl used to filter if set
   *
   * @return an {@link Optional} containing the acl or none.
   */
  public Optional<String> getAccessPolicy() {
    return this.getFilterValue(FILTER_ACL_NAME);
  }

  public void withoutPermissions() {
    ResourceListFilter filter = this.getFilter(FILTER_ACL_PERMISSION_READ_NAME);
    if (filter != null) {
      this.removeFilter(filter);
    }
    filter = this.getFilter(FILTER_ACL_PERMISSION_WRITE_NAME);
    if (filter != null) {
      this.removeFilter(filter);
    }
  }

  public void withReadPermission(boolean value) {
    this.addFilter(createReadPermissionFilter(Optional.ofNullable(value)));
  }

  public void withWritePermission(boolean value) {
    this.addFilter(createWritePermissionFilter(Optional.ofNullable(value)));
  }

  public Optional<Boolean> getReadPermission() {
    return this.getFilterValue(FILTER_ACL_PERMISSION_READ_NAME);
  }

  public Optional<Boolean> getWritePermission() {
    return this.getFilterValue(FILTER_ACL_PERMISSION_WRITE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given contributor
   *
   * @param contributor
   *          the contributor to filter for
   */
  public void withContributor(String contributor) {
    this.addFilter(createContributorsFilter(Optional.ofNullable(contributor)));
  }

  /**
   * Returns an {@link Optional} containing the contributor used to filter if set
   *
   * @return an {@link Optional} containing the contributor or none.
   */
  public Optional<String> getContributor() {
    return this.getFilterValue(FILTER_CONTRIBUTORS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given creation date period
   *
   * @param creationDate
   *          the creation date period as {@link Tuple} with two {@link Date}.
   */
  public void withCreationDate(Tuple<Date, Date> creationDate) {
    this.addFilter(createCreationDateFilter(Optional.ofNullable(creationDate)));
  }

  /**
   * Returns an {@link Optional} containing the creation date period used to filter if set
   *
   * @return an {@link Optional} containing the creation date period or none.
   */
  public Optional<Tuple<Date, Date>> getCreationDate() {
    return this.getFilterValue(FILTER_CREATIONDATE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given creator
   *
   * @param creator
   *          the creator to filter for
   */
  public void withCreator(String creator) {
    this.addFilter(createCreatorFilter(Optional.ofNullable(creator)));
  }

  /**
   * Returns an {@link Optional} containing the creator used to filter if set
   *
   * @return an {@link Optional} containing the creator or none.
   */
  public Optional<String> getCreator() {
    return this.getFilterValue(FILTER_CREATOR_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given language
   *
   * @param language
   *          the language to filter for
   */
  public void withLanguage(String language) {
    this.addFilter(createLanguageFilter(Optional.ofNullable(language)));
  }

  /**
   * Returns an {@link Optional} containing the language used to filter if set
   *
   * @return an {@link Optional} containing the language or none.
   */
  public Optional<String> getLanguage() {
    return this.getFilterValue(FILTER_LANGUAGE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given license
   *
   * @param license
   *          the license to filter for
   */
  public void withLicense(String license) {
    this.addFilter(createLicenseFilter(Optional.ofNullable(license)));
  }

  /**
   * Returns an {@link Optional} containing the license used to filter if set
   *
   * @return an {@link Optional} containing the license or none.
   */
  public Optional<String> getLicense() {
    return this.getFilterValue(FILTER_LICENSE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given organizer
   *
   * @param organizer
   *          the organizer to filter for
   */
  public void withOrganizer(String organizer) {
    this.addFilter(createOrganizersFilter(Optional.ofNullable(organizer)));
  }

  /**
   * Returns an {@link Optional} containing the organizer used to filter if set
   *
   * @return an {@link Optional} containing the organizer or none.
   */
  public Optional<String> getOrganizer() {
    return this.getFilterValue(FILTER_ORGANIZERS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param subject
   *          the subject to filter for
   */
  public void withSubject(String subject) {
    this.addFilter(createSubjectFilter(Optional.ofNullable(subject)));
  }

  /**
   * Returns an {@link Optional} containing the subject used to filter if set
   *
   * @return an {@link Optional} containing the subject or none.
   */
  public Optional<String> getSubject() {
    return this.getFilterValue(FILTER_SUBJECT_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given title
   *
   * @param title
   *          the subject to filter for
   */
  public void withTitle(String title) {
    this.addFilter(createTitleFilter(Optional.ofNullable(title)));
  }

  /**
   * Returns an {@link Optional} containing the title used to filter if set
   *
   * @return an {@link Optional} containing the title or none.
   */
  public Optional<String> getTitle() {
    return this.getFilterValue(FILTER_TITLE_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on access policy
   *
   * @param acl
   *          the acl to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for access policy based query
   */
  public static ResourceListFilter<String> createAccessPolicyFilter(Optional<String> acl) {
    return FiltersUtils.generateFilter(acl, FILTER_ACL_NAME, FILTER_ACL_LABEL, SourceType.SELECT,
            Optional.of(AclListProvider.NAME));
  }

  public static ResourceListFilter<Boolean> createReadPermissionFilter(Optional<Boolean> value) {
    return FiltersUtils.generateFilter(value, FILTER_ACL_PERMISSION_READ_NAME, FILTER_ACL_PERMISSION_READ_LABEL,
        SourceType.SELECT, Optional.empty());
  }

  public static ResourceListFilter<Boolean> createWritePermissionFilter(Optional<Boolean> value) {
    return FiltersUtils.generateFilter(value, FILTER_ACL_PERMISSION_WRITE_NAME, FILTER_ACL_PERMISSION_WRITE_LABEL,
        SourceType.SELECT, Optional.empty());
  }

  /**
   * Create a new {@link ResourceListFilter} based on a contributor
   *
   * @param contributor
   *          the contributor's name to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a contributor based query
   */
  public static ResourceListFilter<String> createContributorsFilter(Optional<String> contributor) {
    return FiltersUtils.generateFilter(contributor, FILTER_CONTRIBUTORS_NAME, FILTER_CONTRIBUTORS_LABEL,
            SourceType.SELECT, Optional.of(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a creator
   *
   * @param creator
   *          the creator to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a creator based query
   */
  public static ResourceListFilter<String> createCreatorFilter(Optional<String> creator) {
    return FiltersUtils.generateFilter(creator, FILTER_CREATOR_NAME, FILTER_CREATOR_LABEL, SourceType.SELECT,
            Optional.of(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on creation date period
   *
   * @param period
   *          the period to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for the given period
   */
  public static ResourceListFilter<Tuple<Date, Date>> createCreationDateFilter(Optional<Tuple<Date, Date>> period) {
    return FiltersUtils.generateFilter(period, FILTER_CREATIONDATE_NAME, FILTER_CREATIONDATE_LABEL, SourceType.PERIOD,
            Optional.empty());
  }

  /**
   * Create a new {@link ResourceListFilter} based on a language
   *
   * @param language
   *          the language to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a language based query
   */
  public static ResourceListFilter<String> createLanguageFilter(Optional<String> language) {
    return FiltersUtils.generateFilter(language, FILTER_LANGUAGE_NAME, FILTER_LANGUAGE_LABEL, SourceType.SELECT,
            Optional.of("LANGUAGES"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a license
   *
   * @param license
   *          the license to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a license based query
   */
  public static ResourceListFilter<String> createLicenseFilter(Optional<String> license) {
    return FiltersUtils.generateFilter(license, FILTER_LICENSE_NAME, FILTER_LICENSE_LABEL, SourceType.SELECT,
            Optional.of("LICENSES"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a organizer
   *
   * @param organizer
   *          the organizer to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a organizer based query
   */
  public static ResourceListFilter<String> createOrganizersFilter(Optional<String> organizer) {
    return FiltersUtils.generateFilter(organizer, FILTER_ORGANIZERS_NAME, FILTER_ORGANIZERS_LABEL, SourceType.SELECT,
            Optional.of(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a subject
   *
   * @param subject
   *          the subject to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a subject based query
   */
  public static ResourceListFilter<String> createSubjectFilter(Optional<String> subject) {
    return FiltersUtils.generateFilter(subject, FILTER_SUBJECT_NAME, FILTER_SUBJECT_LABEL, SourceType.SELECT,
            Optional.of(SeriesListProvider.SUBJECT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a title
   *
   * @param title
   *          the title to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a title based query
   */
  public static ResourceListFilter<String> createTitleFilter(Optional<String> title) {
    return FiltersUtils.generateFilter(title, FILTER_TITLE_NAME, FILTER_TITLE_LABEL, SourceType.SELECT,
            Optional.of(SeriesListProvider.TITLE));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a read role
   *
   * @param readAccess
   *          the read role to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a read role based query
   */
  public static ResourceListFilter<String> createReadAccessFilter(Optional<String> readAccess) {
    return FiltersUtils.generateFilter(readAccess, FILTER_READ_ACCESS_NAME, FILTER_READ_ACCESS_LABEL,
        SourceType.FREETEXT, Optional.of("ROLES.TARGET.ACL"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a write role
   *
   * @param writeAccess
   *          the write role to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a write role based query
   */
  public static ResourceListFilter<String> createWriteAccessFilter(Optional<String> writeAccess) {
    return FiltersUtils.generateFilter(writeAccess, FILTER_WRITE_ACCESS_NAME, FILTER_WRITE_ACCESS_LABEL,
        SourceType.FREETEXT, Optional.of("ROLES.TARGET.ACL"));
  }

}
