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
package org.opencastproject.security.urlsigning.provider.impl;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.WowzaResourceStrategyImpl;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider;
import org.opencastproject.urlsigning.common.Policy;
import org.opencastproject.urlsigning.common.ResourceStrategy;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component(
    immediate = true,
    service = { ManagedService.class, UrlSigningProvider.class },
    property = {
        "service.description=Wowza Url Signing Provider",
        "service.pid=org.opencastproject.security.urlsigning.provider.impl.WowzaUrlSigningProvider"
    }
)
public class WowzaUrlSigningProvider extends AbstractUrlSigningProvider {

  private static final Logger logger = LoggerFactory.getLogger(WowzaUrlSigningProvider.class);

  /** The Wowza resource strategy to use to convert from the base url to a resource url. */
  private ResourceStrategy resourceStrategy = new WowzaResourceStrategyImpl();

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public ResourceStrategy getResourceStrategy() {
    return resourceStrategy;
  }

  @Override
  public String toString() {
    return "Wowza URL Signing Provider";
  }

  /**
   * @param policy
   *             the policy
   * @return the signed url
   */
  @Override
  public String sign(Policy policy) throws UrlSigningException {
    if (!accepts(policy.getBaseUrl())) {
      throw UrlSigningException.urlNotSupported();
    }

    try {
      URI uri = new URI(policy.getBaseUrl());

      /*
        For backward compatibility, but i can not see how this could work.
        According to the documentation 
        "https://www.wowza.com/docs/how-to-protect-streaming-using-securetoken-in-wowza-streaming-engine" 
        if you using token v1 we need a TEA implimentation.
      */
      if ("rtmp".equals((uri.getScheme()))) {
        return super.sign(policy);
      }

        // Get the key that matches this URI since there must be one that matches as the base url has been accepted.
      Key key = getKey(policy.getBaseUrl());

      policy.setResourceStrategy(getResourceStrategy());

      if (!key.getSecret().contains("@")) {
        getLogger().error("Given key not valid. (prefix@secret)");

        throw new Exception("Given key not valid. (prefix@secret)");
      }
      String[] wowzaKeyPair = key.getSecret().split("@");
      String wowzaPrefix = wowzaKeyPair[0];
      String wowzaSecret = wowzaKeyPair[1];

      String newUri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
          addSignutureToRequest(policy, wowzaPrefix, wowzaSecret), null).toString();
      return newUri;
    } catch (Exception e) {
      getLogger().error("Unable to create signed URL because {}", ExceptionUtils.getStackTrace(e));
      throw new UrlSigningException(e);
    }
  }

  /**
   * @param policy
   *             the policy
   * @param encryptionKeyId
   *             the encription key id
   * @param encryptionKey
   *             the encription key
   * @return true
   *             if the url is excluded, false otherwise
   * @exception Exception
   *             if something goes bad
   */
  private String addSignutureToRequest(Policy policy, String encryptionKeyId, String encryptionKey) throws Exception  {
    final String startTime;
    final String endTime = Long.toString(policy.getValidUntil().getMillis() / 1000);
    final String ip;

    String baseUrl = policy.getBaseUrl();
    URI url = new URI(policy.getBaseUrl());
    String resource = policy.getResource();
    if (policy.getClientIpAddress().isPresent()) {
      // The ip comes with a slash: /192.168.1.2
      String ipAux = policy.getClientIpAddress().get().toString();
      ipAux = ipAux.substring(1, ipAux.length());
      ip = ipAux;
    } else {
      ip = "";
    }

    if (policy.getValidFrom().isPresent()) {
      startTime = Long.toString(policy.getValidFrom().get().getMillis() / 1000);
    } else {
      startTime = "";
    }

    String queryStringParameters = new String();

    queryStringParameters += encryptionKeyId + "endtime=" + endTime;

    if (!"".equals(startTime)) {
      queryStringParameters += "&" + encryptionKeyId + "starttime=" + startTime;
    }

    if (url.getQuery() != null) {
      String query = url.getQuery();
      String[] params = query.split("&");
      for (String param : params) {
        if (param.contains("=")) {
          String[] keyValue = param.split("=");
          queryStringParameters += "&" + encryptionKeyId + keyValue[0] + "=" + keyValue[1];
        }
      }
    }

    queryStringParameters += "&" + encryptionKeyId + "hash=" + generateHash(baseUrl,
        resource, ip, encryptionKeyId, encryptionKey, startTime, endTime);

    return queryStringParameters;
  }

  /**
   * @param baseUrl
   *             the base url
   * @param resource
   *             the resource
   * @param ip
   *             the ip
   * @param encryptionKeyId
   *             the encription key id
   * @param encryptionKey
   *             the encription key
   * @param startTime
   *             start time
   * @param endTime
   *             end time
   * @return the generated hashed
   * @exception Exception
   *             if something goes bad
   */
  private String generateHash(String baseUrl, String resource, String ip,
      String encryptionKeyId, String encryptionKey, String startTime, String endTime) throws Exception {
    String urlToHash = resource + "?";

    if (!"".equals(ip)) {
      urlToHash += ip + "&" + encryptionKey;
    } else {
      urlToHash += encryptionKey;
    }

    SortedMap<String, String> arguments = new TreeMap<>();

    arguments.put(encryptionKeyId + "endtime", endTime);

    if (!"".equals(startTime)) {
      arguments.put(encryptionKeyId + "starttime", startTime);
    }

    String query = new URI(baseUrl).getQuery();
    if (null == query) {
      query = "";
    }

    String[] params = query.split("&");
    for (String param : params) {
      if (param.contains("=")) {
        String[] keyValue = param.split("=");
        arguments.put(encryptionKeyId + keyValue[0], keyValue[1]);
      }
    }

    for (Map.Entry<String,String> entry : arguments.entrySet()) {
      String value = entry.getValue();
      String key = entry.getKey();
      urlToHash += "&" + key + "=" + value;
    }

    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] messageDigest = md.digest(urlToHash.getBytes());
    String base64Hash = Base64.getEncoder().encodeToString(messageDigest);

    base64Hash = base64Hash.replaceAll("\\+", "-");
    base64Hash = base64Hash.replaceAll("/", "_");

    return base64Hash;
  }

  @Reference
  @Override
  public void setSecurityService(SecurityService securityService) {
    super.setSecurityService(securityService);
  }

}
