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

package org.opencastproject.caption.remote;

import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
public class CaptionServiceRemoteImpl extends RemoteBase implements CaptionService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceRemoteImpl.class);

  public CaptionServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * @see org.opencastproject.caption.api.CaptionService#convert(Catalog, String, String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat) throws UnsupportedCaptionFormatException,
          CaptionConverterException, MediaPackageException {
    return convert(input, inputFormat, outputFormat, null);
  }

  /**
   * @see org.opencastproject.caption.api.CaptionService#convert(Catalog, String, String, String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException, MediaPackageException {
    HttpPost post = new HttpPost("/convert");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("captions", MediaPackageElementParser.getAsXml(input)));
      params.add(new BasicNameValuePair("input", inputFormat));
      params.add(new BasicNameValuePair("output", outputFormat));
      if (StringUtils.isNotBlank(language))
        params.add(new BasicNameValuePair("language", language));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new CaptionConverterException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("Converting job {} started on a remote caption service", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new CaptionConverterException("Unable to convert catalog " + input + " using a remote caption service", e);
    } finally {
      closeConnection(response);
    }
    throw new CaptionConverterException("Unable to convert catalog " + input + " using a remote caption service");
  }

  /**
   * @see org.opencastproject.caption.api.CaptionService#getLanguageList(Catalog, String)
   */
  @Override
  public String[] getLanguageList(Catalog input, String format) throws UnsupportedCaptionFormatException,
          CaptionConverterException {
    HttpPost post = new HttpPost("/languages");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("captions", MediaPackageElementParser.getAsXml(input)));
      params.add(new BasicNameValuePair("input", format));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new CaptionConverterException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        List<String> langauges = new ArrayList<String>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(EntityUtils.toString(response.getEntity(), "UTF-8"));
        NodeList languages = doc.getElementsByTagName("languages");
        for (int i = 0; i < languages.getLength(); i++) {
          Node item = languages.item(i);
          langauges.add(item.getTextContent());
        }
        logger.info("Catalog languages received from remote caption service");
        return langauges.toArray(new String[langauges.size()]);
      }
    } catch (Exception e) {
      throw new CaptionConverterException("Unable to get catalog languages " + input
              + " using a remote caption service", e);
    } finally {
      closeConnection(response);
    }
    throw new CaptionConverterException("Unable to get catalog languages" + input + " using a remote caption service");
  }

}
