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

package org.opencastproject.kernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This implements a zero-configuration version Spring Security's token based remember-me service. While the key can
 * still be augmented by configuration, it is generally generated based on seldom changing but unique system
 * properties like hostname, IP address, file system information and Linux kernel.
 */
public class SystemTokenBasedRememberMeService extends TokenBasedRememberMeServices {
  private Logger logger = LoggerFactory.getLogger(SystemTokenBasedRememberMeService.class);

  @Deprecated
  public SystemTokenBasedRememberMeService() {
    super();
  }

  public SystemTokenBasedRememberMeService(String key, UserDetailsService userDetailsService) {
    super(SystemTokenRememberMeUtils.augmentKey(key), userDetailsService);
  }

  /**
   * Set a new key to be used when generating remember-me tokens.
   *
   * Note that the key passed to this method will be augmented by seldom changing but generally unique system
   * properties like hostname, IP address, file system information and Linux kernel. Hence, even setting no custom
   * key should be save.
   */
  @Override
  public void setKey(String key) {
    super.setKey(SystemTokenRememberMeUtils.augmentKey(key));
  }

  /**
   * Calculates the digital signature to be put in the cookie. Default value is
   * SHA-512 ("username:tokenExpiryTime:password:key")
   */
  @Override
  protected String makeTokenSignature(long tokenExpiryTime, String username, String password) {
    String data = username + ":" + tokenExpiryTime + ":" + password + ":" + getKey();
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No SHA-512 algorithm available!");
    }

    return new String(Hex.encode(digest.digest(data.getBytes())));
  }
}
