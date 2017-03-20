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

import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.UrlSupport.url;

import org.opencastproject.oaipmh.server.MetadataFormat;

import java.net.URI;
import java.net.URL;

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

  String ERROR_ID_DOES_NOT_EXIST = "idDoesNotExist";
  String ERROR_BAD_ARGUMENT = "badArgument";
  String ERROR_BAD_RESUMPTION_TOKEN = "badResumptionToken";
  String ERROR_CANNOT_DISSEMINATE_FORMAT = "cannotDisseminateFormat";
  String ERROR_NO_RECORDS_MATCH = "noRecordsMatch";
  String ERROR_NO_SET_HIERARCHY = "noSetHierarchy";
  String ERROR_NO_METADATA_FORMATS = "noMetadataFormats";

  String OAI_2_0_XML_NS = "http://www.openarchives.org/OAI/2.0/";
  String OAI_2_0_SCHEMA_LOCATION = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";

  String OAI_DC_XML_NS = "http://www.openarchives.org/OAI/2.0/oai_dc/";
  URI OAI_DC_XML_NS_URI = uri(OAI_DC_XML_NS);
  String OAI_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
  URL OAI_DC_SCHEMA_URL = url(OAI_DC_SCHEMA);
  String OAI_DC_SCHEMA_LOCATION = OAI_DC_XML_NS + " " + OAI_DC_SCHEMA;

  /** ACL read permission (action). */
  String READ_PERMISSION = "read";

  MetadataFormat OAI_DC_METADATA_FORMAT = new MetadataFormat() {
    @Override
    public String getPrefix() {
      return "oai_dc";
    }

    @Override
    public URL getSchema() {
      return OAI_DC_SCHEMA_URL;
    }

    @Override
    public URI getNamespace() {
      return OAI_DC_XML_NS_URI;
    }
  };
}
