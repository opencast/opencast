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
package org.opencastproject.series.impl.solr;

/**
 * Definitions of all the keys in (except sort keys) in solr.
 */
public interface SolrFields {

  /** The key in solr documents representing the composite series and organization IDs */
  String COMPOSITE_ID_KEY = "composite_id";

  /** The key in solr documents representing series DC as xml */
  String XML_KEY = "xml";

  /** The key representing Access Control List */
  String ACCESS_CONTROL_KEY = "access_control";

  /** The key representing the roles that can contribute to the series */
  String ACCESS_CONTROL_CONTRIBUTE = "acl_contribute";

  /** The key representing the roles that can edit the series */
  String ACCESS_CONTROL_EDIT = "acl_edit";

  /** The key representing the roles that can read the series */
  String ACCESS_CONTROL_READ = "acl_read";

  /** The key representing the opt out status */
  String OPT_OUT = "opt_out";

  /** Dublin core fields */
  String TITLE_KEY = "dc_title";
  String SUBJECT_KEY = "dc_subject";
  String CREATOR_KEY = "dc_creator";
  String PUBLISHER_KEY = "dc_publisher";
  String CONTRIBUTOR_KEY = "dc_contributor";
  String ABSTRACT_KEY = "dc_abstract";
  String DESCRIPTION_KEY = "dc_description";
  String CREATED_KEY = "dc_created";
  String AVAILABLE_FROM_KEY = "dc_available_from";
  String AVAILABLE_TO_KEY = "dc_avaliable_to";
  String LANGUAGE_KEY = "dc_language";
  String RIGHTS_HOLDER_KEY = "dc_rights_holder";
  String SPATIAL_KEY = "dc_spatial";
  String TEMPORAL_KEY = "dc_temporal";
  String IS_PART_OF_KEY = "dc_is_part_of";
  String REPLACES_KEY = "dc_replaces";
  String TYPE_KEY = "dc_type";
  String ACCESS_RIGHTS_KEY = "dc_access_rights";
  String LICENSE_KEY = "dc_license";
  String ORGANIZATION = "organization";

  /** Fulltext search field */
  String FULLTEXT_KEY = "fulltext";
}
