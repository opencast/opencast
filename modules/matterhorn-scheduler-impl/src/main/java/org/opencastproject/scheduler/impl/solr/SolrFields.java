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
package org.opencastproject.scheduler.impl.solr;

/**
 * Definitions of all the keys (except sort keys) in solr.
 */
public interface SolrFields {

  /** The key in solr documents representing the workflow's ID */
  String ID_KEY = "id";

  /** The key in solr documents representing the event as xml */
  String XML_KEY = "xml";

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
  String IS_PART_OF_KEY = "dc_is_part_of";
  String REPLACES_KEY = "dc_replaces";
  String TYPE_KEY = "dc_type";
  String ACCESS_RIGHTS_KEY = "dc_access_rights";
  String LICENSE_KEY = "dc_license";

  /** Event fields */
  String STARTS_KEY = "event_start";
  String ENDS_KEY = "event_end";
  String LAST_MODIFIED = "event_last_modified";
  String OPT_OUT = "event_opt_out";
  String BLACKLISTED = "event_blacklisted";

  /** Capture agent fields */
  String CA_PROPERTIES = "ca_properties";

  /** Fulltext search field */
  String FULLTEXT_KEY = "fulltext";
}
