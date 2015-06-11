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
package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.provider.AclListProvider;
import org.opencastproject.index.service.resources.list.provider.ContributorsListProvider;
import org.opencastproject.index.service.resources.list.provider.LanguagesListProvider;
import org.opencastproject.index.service.resources.list.provider.SeriesListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import java.util.Date;

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
public class SeriesListQuery extends ResourceListQueryImpl {

  public static final String FILTER_ACL_NAME = "managedAcl";
  private static final String FILTER_ACL_LABEL = "FILTERS.SERIES.ACCESS_POLICY.LABEL";

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

  public SeriesListQuery() {
    super();
    this.availableFilters.add(createContributorsFilter(Option.<String> none()));
    this.availableFilters.add(createCreationDateFilter(Option.<Tuple<Date, Date>> none()));
    this.availableFilters.add(createOrganizersFilter(Option.<String> none()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given acl
   *
   * @param acl
   *          the acl to filter for
   */
  public void withAccessPolicy(String acl) {
    this.addFilter(createAccessPolicyFilter(Option.option(acl)));
  }

  /**
   * Returns an {@link Option} containing the acl used to filter if set
   *
   * @return an {@link Option} containing the acl or none.
   */
  public Option<String> getAccessPolicy() {
    return this.getFilterValue(FILTER_ACL_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given contributor
   *
   * @param presenter
   *          the presenter to filter for
   */
  public void withContributor(String contributor) {
    this.addFilter(createContributorsFilter(Option.option(contributor)));
  }

  /**
   * Returns an {@link Option} containing the contributor used to filter if set
   *
   * @return an {@link Option} containing the contributor or none.
   */
  public Option<String> getContributor() {
    return this.getFilterValue(FILTER_CONTRIBUTORS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given creation date period
   *
   * @param creationDate
   *          the creation date period as {@link Tuple} with two {@link Date}.
   */
  public void withCreationDate(Tuple<Date, Date> creationDate) {
    this.addFilter(createCreationDateFilter(Option.option(creationDate)));
  }

  /**
   * Returns an {@link Option} containing the creation date period used to filter if set
   *
   * @return an {@link Option} containing the creation date period or none.
   */
  public Option<Tuple<Date, Date>> getCreationDate() {
    return this.getFilterValue(FILTER_CREATIONDATE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given creator
   *
   * @param creator
   *          the creator to filter for
   */
  public void withCreator(String creator) {
    this.addFilter(createCreatorFilter(Option.option(creator)));
  }

  /**
   * Returns an {@link Option} containing the creator used to filter if set
   *
   * @return an {@link Option} containing the creator or none.
   */
  public Option<String> getCreator() {
    return this.getFilterValue(FILTER_CREATOR_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given language
   *
   * @param language
   *          the language to filter for
   */
  public void withLanguage(String language) {
    this.addFilter(createLanguageFilter(Option.option(language)));
  }

  /**
   * Returns an {@link Option} containing the language used to filter if set
   *
   * @return an {@link Option} containing the language or none.
   */
  public Option<String> getLanguage() {
    return this.getFilterValue(FILTER_LANGUAGE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given license
   *
   * @param license
   *          the license to filter for
   */
  public void withLicense(String license) {
    this.addFilter(createLicenseFilter(Option.option(license)));
  }

  /**
   * Returns an {@link Option} containing the license used to filter if set
   *
   * @return an {@link Option} containing the license or none.
   */
  public Option<String> getLicense() {
    return this.getFilterValue(FILTER_LICENSE_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given organizer
   *
   * @param organizer
   *          the organizer to filter for
   */
  public void withOrganizer(String organizer) {
    this.addFilter(createOrganizersFilter(Option.option(organizer)));
  }

  /**
   * Returns an {@link Option} containing the organizer used to filter if set
   *
   * @return an {@link Option} containing the organizer or none.
   */
  public Option<String> getOrganizer() {
    return this.getFilterValue(FILTER_ORGANIZERS_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given subject
   *
   * @param subject
   *          the subject to filter for
   */
  public void withSubject(String subject) {
    this.addFilter(createSubjectFilter(Option.option(subject)));
  }

  /**
   * Returns an {@link Option} containing the subject used to filter if set
   *
   * @return an {@link Option} containing the subject or none.
   */
  public Option<String> getSubject() {
    return this.getFilterValue(FILTER_SUBJECT_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given title
   *
   * @param title
   *          the subject to filter for
   */
  public void withTitle(String title) {
    this.addFilter(createTitleFilter(Option.option(title)));
  }

  /**
   * Returns an {@link Option} containing the title used to filter if set
   *
   * @return an {@link Option} containing the title or none.
   */
  public Option<String> getTitle() {
    return this.getFilterValue(FILTER_TITLE_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on access policy
   *
   * @param acl
   *          the acl to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for access policy based query
   */
  public static ResourceListFilter<String> createAccessPolicyFilter(Option<String> acl) {
    return FiltersUtils.generateFilter(acl, FILTER_ACL_NAME, FILTER_ACL_LABEL, SourceType.SELECT,
            Option.some(AclListProvider.NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a contributor
   *
   * @param contributor
   *          the contributor's name to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a contributor based query
   */
  public static ResourceListFilter<String> createContributorsFilter(Option<String> contributor) {
    return FiltersUtils.generateFilter(contributor, FILTER_CONTRIBUTORS_NAME, FILTER_CONTRIBUTORS_LABEL,
            SourceType.SELECT, Option.some(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a creator
   *
   * @param creator
   *          the creator to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a creator based query
   */
  public static ResourceListFilter<String> createCreatorFilter(Option<String> creator) {
    return FiltersUtils.generateFilter(creator, FILTER_CREATOR_NAME, FILTER_CREATOR_LABEL, SourceType.SELECT,
            Option.some(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on creation date period
   *
   * @param period
   *          the period to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for the given period
   */
  public static ResourceListFilter<Tuple<Date, Date>> createCreationDateFilter(Option<Tuple<Date, Date>> period) {
    return FiltersUtils.generateFilter(period, FILTER_CREATIONDATE_NAME, FILTER_CREATIONDATE_LABEL, SourceType.PERIOD,
            Option.some(SeriesListProvider.CREATION_DATE));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a language
   *
   * @param language
   *          the language to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a language based query
   */
  public static ResourceListFilter<String> createLanguageFilter(Option<String> language) {
    return FiltersUtils.generateFilter(language, FILTER_LANGUAGE_NAME, FILTER_LANGUAGE_LABEL, SourceType.SELECT,
            Option.some(LanguagesListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a license
   *
   * @param license
   *          the license to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a license based query
   */
  public static ResourceListFilter<String> createLicenseFilter(Option<String> license) {
    return FiltersUtils.generateFilter(license, FILTER_LICENSE_NAME, FILTER_LICENSE_LABEL, SourceType.SELECT,
            Option.some("LICENSES"));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a organizer
   *
   * @param organizer
   *          the organizer to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a organizer based query
   */
  public static ResourceListFilter<String> createOrganizersFilter(Option<String> organizer) {
    return FiltersUtils.generateFilter(organizer, FILTER_ORGANIZERS_NAME, FILTER_ORGANIZERS_LABEL, SourceType.SELECT,
            Option.some(ContributorsListProvider.DEFAULT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a subject
   *
   * @param subject
   *          the subject to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a subject based query
   */
  public static ResourceListFilter<String> createSubjectFilter(Option<String> subject) {
    return FiltersUtils.generateFilter(subject, FILTER_SUBJECT_NAME, FILTER_SUBJECT_LABEL, SourceType.SELECT,
            Option.some(SeriesListProvider.SUBJECT));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a title
   *
   * @param title
   *          the title to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a title based query
   */
  public static ResourceListFilter<String> createTitleFilter(Option<String> title) {
    return FiltersUtils.generateFilter(title, FILTER_TITLE_NAME, FILTER_TITLE_LABEL, SourceType.SELECT,
            Option.some(SeriesListProvider.TITLE));
  }

}
