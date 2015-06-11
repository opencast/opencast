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


package org.opencastproject.oaipmh;

/**
 * OAI specific constants like request parameter names etc.
 */
public interface OaiPmhConstants {

  String VERB_IDENTIFY = "Identify";
  String VERB_LIST_RECORDS = "ListRecords";
  String VERB_LIST_METADATA_FORMATS = "ListMetadataFormats";
  String VERB_LIST_SETS = "ListSets";
  String VERB_LIST_IDENTIFIERS = "ListIdentifiers";
  String VERB_GET_RECORD = "GetRecord";

  String OAI_2_0_XML_NS = "http://www.openarchives.org/OAI/2.0/";
  String OAI_2_0_SCHEMA_LOCATION = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";

  String OAI_DC_XML_NS = "http://www.openarchives.org/OAI/2.0/oai_dc/";
  String OAI_DC_SCHEMA_LOCATION = "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
}
