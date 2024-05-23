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

package org.opencastproject.transcription.microsoft.azure;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;

public class MicrosoftAzureAuthorization {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureStorageClient.class);
  public static final String AZURE_STORAGE_VERSION = "2022-11-02";
  public static final String AZURE_BLOB_STORE_URL_SUFFIX = "blob.core.windows.net";

  private final String azureStorageAccountName;
  private final String azureAccountAccessKey;

  public MicrosoftAzureAuthorization(String azureStorageAccountName, String azureAccountAccessKey)
          throws MicrosoftAzureStorageClientException {
    this.azureStorageAccountName = StringUtils.trimToEmpty(azureStorageAccountName);
    this.azureAccountAccessKey = StringUtils.trimToEmpty(azureAccountAccessKey);
    if (StringUtils.isEmpty(azureStorageAccountName)) {
      throw new MicrosoftAzureStorageClientException("Azure storage account name not set.");
    }
    if (StringUtils.isEmpty(azureAccountAccessKey)) {
      throw new MicrosoftAzureStorageClientException("Azure storage account key not set.");
    }
  }

  public String getAzureStorageAccountName() {
    return azureStorageAccountName;
  }

  String generateAccountSASToken(String signedPermissions, String signedResourceType,
      Date signedStart, Date signedExpiry, String signedIP, String signedEncryptionScope) {
    // documentation: https://learn.microsoft.com/en-us/rest/api/storageservices/create-account-sas
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    List<String> queryArgs = new ArrayList<>();
    StringBuilder stringBuilder = new StringBuilder();
    /*
    StringToSign = accountname + "\n" +
        signedpermissions + "\n" +
        signedservice + "\n" +
        signedresourcetype + "\n" +
        signedstart + "\n" +
        signedexpiry + "\n" +
        signedIP + "\n" +
        signedProtocol + "\n" +
        signedversion + "\n" +
        signedEncryptionScope + "\n"
    */
    stringBuilder.append(azureStorageAccountName + "\n");
    stringBuilder.append(StringUtils.trimToEmpty(signedPermissions) + "\n");
    queryArgs.add("sp=" + StringUtils.trimToEmpty(signedPermissions));
    stringBuilder.append("b" + "\n");
    queryArgs.add("ss=b");
    stringBuilder.append(StringUtils.trimToEmpty(signedResourceType) + "\n");
    queryArgs.add("srt=" + StringUtils.trimToEmpty(signedResourceType));
    Date startDate = signedStart;
    if (startDate == null) {
      startDate = Date.from(Instant.now().minus(15, ChronoUnit.MINUTES));
    }
    stringBuilder.append(df.format(startDate) + "\n");
    queryArgs.add("st=" + df.format(startDate));
    Date endDate = signedExpiry;
    if (endDate == null) {
      endDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    }
    stringBuilder.append(df.format(endDate) + "\n");
    queryArgs.add("se=" + df.format(endDate));
    stringBuilder.append(StringUtils.trimToEmpty(signedIP) + "\n");
    if (StringUtils.isNotBlank(signedIP)) {
      queryArgs.add("sip=" + StringUtils.trimToEmpty(signedIP));
    }
    stringBuilder.append("https" + "\n");
    queryArgs.add("spr=https");
    stringBuilder.append(AZURE_STORAGE_VERSION + "\n");
    queryArgs.add("sv=" + AZURE_STORAGE_VERSION);
    stringBuilder.append(StringUtils.trimToEmpty(signedEncryptionScope) + "\n");
    if (StringUtils.isNotBlank(signedEncryptionScope)) {
      queryArgs.add("ses=" + StringUtils.trimToEmpty(signedEncryptionScope));
    }
    String stringToSign = stringBuilder.toString();

    Mac initializedMac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256,
        Base64.decodeBase64(azureAccountAccessKey));
    byte[] signedString = initializedMac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
    String signature = Base64.encodeBase64String(signedString);
    queryArgs.add("sig=" + URLEncoder.encode(signature, StandardCharsets.UTF_8));
    return StringUtils.joinWith("&", queryArgs.toArray());
  }

  String generateServiceSasToken(String signedPermissions, Date signedStart, Date signedExpiry, String resource,
      String signedResource) {
    return  generateServiceSasToken(signedPermissions, signedStart, signedExpiry, resource, null, null, null,
        signedResource, null, null, null, null, null, null, null, null);
  }

  String generateServiceSasToken(String signedPermissions, Date signedStart, Date signedExpiry, String resource,
      String signedIdentifier, String signedIP, String signedVersion, String signedResource,
      String signedDirectoryDepth, String signedSnapshotTime, String signedEncryptionScope,
      String rscc, String rscd, String rsce, String rscl, String rsct) {
    // documentation:
    // https://learn.microsoft.com/en-us/rest/api/storageservices/create-service-sas
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    List<String> queryArgs = new ArrayList<>();
    StringBuilder stringBuilder = new StringBuilder();
    /*
    StringToSign = signedPermissions + "\n" +
               signedStart + "\n" +
               signedExpiry + "\n" +
               canonicalizedResource + "\n" +
               signedIdentifier + "\n" +
               signedIP + "\n" +
               signedProtocol + "\n" +
               signedVersion + "\n" +
               signedResource + "\n" +
               signedSnapshotTime + "\n" +
               signedEncryptionScope + "\n" +
               rscc + "\n" +
               rscd + "\n" +
               rsce + "\n" +
               rscl + "\n" +
               rsct
     */
    //    signedPermissions (sp)
    stringBuilder.append(StringUtils.trimToEmpty(signedPermissions) + "\n");
    queryArgs.add("sp=" + StringUtils.trimToEmpty(signedPermissions));
    //    signedStart (st)
    Date startDate = signedStart;
    if (startDate == null) {
      startDate = Date.from(Instant.now().minus(15, ChronoUnit.MINUTES));
    }
    stringBuilder.append(df.format(startDate) + "\n");
    queryArgs.add("st=" + df.format(startDate));
    //    signedExpiry (se)
    Date endDate = signedExpiry;
    if (endDate == null) {
      endDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    }
    stringBuilder.append(df.format(endDate) + "\n");
    queryArgs.add("se=" + df.format(endDate));
    //    canonicalizedResource ()
    String canonicalizedResource = Paths.get("/blob", azureStorageAccountName, StringUtils.trimToEmpty(resource))
        .normalize().toString();
    if (StringUtils.endsWith(canonicalizedResource,"/")) {
      canonicalizedResource = StringUtils.substring(canonicalizedResource, 0, canonicalizedResource.length() - 1);
    }
    stringBuilder.append(canonicalizedResource + "\n");
    //    signedIdentifier (si)
    stringBuilder.append(StringUtils.trimToEmpty(signedIdentifier) + "\n");
    if (StringUtils.isNotBlank(signedIdentifier)) {
      queryArgs.add("si=" + StringUtils.trimToEmpty(signedIdentifier));
    }
    //    signedIP (sip)
    stringBuilder.append(StringUtils.trimToEmpty(signedIP) + "\n");
    if (StringUtils.isNotBlank(signedIP)) {
      queryArgs.add("sip=" + StringUtils.trimToEmpty(signedIP));
    }
    //    signedProtocol (spr)
    stringBuilder.append("https" + "\n");
    queryArgs.add("spr=https");
    //    signedVersion (sv)
    if (StringUtils.isNotBlank(signedVersion)) {
      stringBuilder.append(StringUtils.trimToEmpty(signedVersion) + "\n");
      queryArgs.add("sv=" + StringUtils.trimToEmpty(signedVersion));
    } else {
      stringBuilder.append(AZURE_STORAGE_VERSION + "\n");
      queryArgs.add("sv=" + AZURE_STORAGE_VERSION);
    }
    //    signedResource (sr)
    stringBuilder.append(StringUtils.trimToEmpty(signedResource) + "\n");
    if (StringUtils.isNotBlank(signedResource)) {
      queryArgs.add("sr=" + StringUtils.trimToEmpty(signedResource));
    }
    //    sr=d -> signedDirectoryDepth (sdd)
    if (StringUtils.isNotBlank(signedDirectoryDepth)) {
      queryArgs.add("sdd=" + StringUtils.trimToEmpty(signedDirectoryDepth));
    }
    stringBuilder.append(StringUtils.trimToEmpty(signedSnapshotTime) + "\n");
    //    if (StringUtils.isNotBlank(signedSnapshotTime)) {
    //      queryArgs.add("sst???=" + StringUtils.trimToEmpty(signedSnapshotTime));
    //    }
    //    signedEncryptionScope (ses)
    stringBuilder.append(StringUtils.trimToEmpty(signedEncryptionScope) + "\n");
    if (StringUtils.isNotBlank(signedEncryptionScope)) {
      queryArgs.add("ses=" + StringUtils.trimToEmpty(signedEncryptionScope));
    }
    //    rscc = Cache-Control (rscc)
    stringBuilder.append(StringUtils.trimToEmpty(rscc) + "\n");
    if (StringUtils.isNotBlank(rscc)) {
      queryArgs.add("rscc=" + StringUtils.trimToEmpty(rscc));
    }
    //    rscd = Content-Disposition (rscd)
    stringBuilder.append(StringUtils.trimToEmpty(rscd) + "\n");
    if (StringUtils.isNotBlank(rscd)) {
      queryArgs.add("rscd=" + StringUtils.trimToEmpty(rscd));
    }
    //    rsce = Content-Encoding (rsce)
    stringBuilder.append(StringUtils.trimToEmpty(rsce) + "\n");
    if (StringUtils.isNotBlank(rsce)) {
      queryArgs.add("rsce=" + StringUtils.trimToEmpty(rsce));
    }
    //    rscl = Content-Language (rscl)
    stringBuilder.append(StringUtils.trimToEmpty(rscl) + "\n");
    if (StringUtils.isNotBlank(rscl)) {
      queryArgs.add("rscl=" + StringUtils.trimToEmpty(rscl));
    }
    //    rsct = Content-Type (rsct)
    stringBuilder.append(StringUtils.trimToEmpty(rsct));
    if (StringUtils.isNotBlank(rsct)) {
      queryArgs.add("rsct=" + StringUtils.trimToEmpty(rsct));
    }
    String stringToSign = stringBuilder.toString();

    Mac initializedMac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256,
        Base64.decodeBase64(azureAccountAccessKey));
    byte[] signedString = initializedMac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
    String signature = Base64.encodeBase64String(signedString);
    queryArgs.add("sig=" + URLEncoder.encode(signature, StandardCharsets.UTF_8));
    return StringUtils.joinWith("&", queryArgs.toArray());
  }
}
