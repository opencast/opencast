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
package org.opencastproject.oaipmh.harvester;

import static org.opencastproject.oaipmh.OaiPmhUtil.toUtc;

import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.util.XmlSafeParser;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The repository client provides low level methods to talk to an OAI-PMH repository.
 */
public final class OaiPmhRepositoryClient {
  private final HttpClient httpclient;

  private final DocumentBuilderFactory builderFactory;

  private final String baseUrl;

  private Granularity supportedGranularity;

  /**
   * Create a new harvester to talk to the repository at <code>baseUrl</code>.
   */
  private OaiPmhRepositoryClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.builderFactory = XmlSafeParser.newDocumentBuilderFactory();

    this.builderFactory.setNamespaceAware(true);
    this.httpclient = new DefaultHttpClient();
  }

  /**
   * Create and setup a new harvester for the given repository <code>baseUrl</code>.
   */
  public static OaiPmhRepositoryClient newHarvester(String baseUrl) {
    OaiPmhRepositoryClient repositoryClient = new OaiPmhRepositoryClient(baseUrl);
    // identify the repository
    Granularity g = repositoryClient.identify().getGranularity();
    repositoryClient.setSupportedGranularity(g);
    return repositoryClient;
  }

  private void setSupportedGranularity(Granularity supportedGranularity) {
    this.supportedGranularity = supportedGranularity;
  }

  /**
   * Run an "Identify" request.
   */
  public IdentifyResponse identify() {
    return new IdentifyResponse(doRequest(baseUrl + "?verb=" + OaiPmhConstants.VERB_IDENTIFY));
  }

  /**
   * Run a "ListRecords" request.
   */
  public ListRecordsResponse listRecords(String metadataPrefix, Option<Date> from, Option<Date> until, Option<String> set) {
    String queryParams = join(
        "verb=" + OaiPmhConstants.VERB_LIST_RECORDS,
        "metadataPrefix=" + metadataPrefix,
        from.map(mkQueryParamDate("from")).getOrElse(""),
        until.map(mkQueryParamDate("until")).getOrElse(""),
        set.map(mkQueryParam("set")).getOrElse(""));
    return new ListRecordsResponse(doRequest(baseUrl + "?" + queryParams));
  }

  /**
   * Resume a "ListRecords" request.
   */
  public ListRecordsResponse resumeListRecords(String resumptionToken) {
    String queryParams = "verb=" + OaiPmhConstants.VERB_LIST_RECORDS + "&resumptionToken=" + resumptionToken;
    return new ListRecordsResponse(doRequest(baseUrl + "?" + queryParams));
  }

  private Function<Date, String> mkQueryParamDate(final String key) {
    return new Function<Date, String>() {
      @Override
      public String apply(Date date) {
        return key + "=" + toUtc(date, supportedGranularity);
      }
    };
  }

  private Function<String, String> mkQueryParam(final String key) {
    return new Function<String, String>() {
      @Override
      public String apply(String a) {
        return key + "=" + a;
      }
    };
  }

  /**
   * Join the query parameters.
   */
  private String join(String... as) {
    StringBuffer buf = new StringBuffer();
    for (String a : as) {
      if (a.length() > 0)
        buf.append(a).append("&");
    }
    return buf.substring(0, Math.max(buf.length() - "&".length(), 0));
  }

  /**
   * Execute the request and return the document.
   */
  public Document doRequest(String url) {
    final HttpGet httpget = new HttpGet(url);
    try {
      final HttpResponse response = httpclient.execute(httpget);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
          final InputStream content = entity.getContent();
          final Document doc = builderFactory.newDocumentBuilder().parse(content);
          if (doc.getChildNodes().getLength() == 0) {
            throw new RequestException("Empty response");
          }
          return doc;
        } else {
          throw new RequestException("Empty response");
        }
      } else {
        throw new RequestException("Response code not OK");
      }
    } catch (Exception e) {
      throw new RequestException("Error running request", e);
    }
  }
}
