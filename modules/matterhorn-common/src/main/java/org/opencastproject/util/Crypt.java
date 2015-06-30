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

package org.opencastproject.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

/** AES encryption. */
public final class Crypt {
  private Crypt() {
  }

  public static String encrypt(Key key, String text) {
    final ByteArrayOutputStream encrypted;
    try {
      final Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      encrypted = new ByteArrayOutputStream();
      final CipherOutputStream cipherStream = new CipherOutputStream(encrypted, cipher);
      try {
        cipherStream.write(text.getBytes());
      } finally {
        try {
          cipherStream.close();
        } catch (Exception e) {
        }
        try {
          encrypted.close();
        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error encrypting text", e);
    }
    return new String(encodeToHex(encrypted.toByteArray()));
  }

  public static String decrypt(Key key, String encryptedText) {
    final ByteArrayOutputStream decrypted;
    try {
      final Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, key);
      final byte[] decodeBuffer = new byte[1024];
      final ByteArrayInputStream encrypted = new ByteArrayInputStream(decodeFromHex(encryptedText));
      decrypted = new ByteArrayOutputStream();
      final CipherInputStream cipherStream = new CipherInputStream(encrypted, cipher);
      try {
        int n;
        while ((n = cipherStream.read(decodeBuffer)) > 0) {
          decrypted.write(decodeBuffer, 0, n);
        }
      } finally {
        IoSupport.closeQuietly(decrypted);
        IoSupport.closeQuietly(cipherStream);
        IoSupport.closeQuietly(encrypted);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error decrypting text", e);
    }
    return decrypted.toString();
  }

  public static Key createKey(String password) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(password.getBytes("ISO-8859-1"));
      return new SecretKeySpec(md.digest(), "AES");
    } catch (Exception e) {
      throw new RuntimeException("Error creating key", e);
    }
  }

  // Copied here so that the Security methods can be run standalone without dependencies

  private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  public static char[] encodeToHex(byte[] bytes) {
    final int nBytes = bytes.length;
    char[] result = new char[2 * nBytes];

    int j = 0;
    for (int i = 0; i < nBytes; i++) {
      // Char for top 4 bits
      result[j++] = HEX[(0xF0 & bytes[i]) >>> 4];
      // Bottom 4
      result[j++] = HEX[(0x0F & bytes[i])];
    }

    return result;
  }

  public static byte[] decodeFromHex(CharSequence s) {
    int nChars = s.length();

    if (nChars % 2 != 0) {
      throw new IllegalArgumentException("Hex-encoded string must have an even number of characters");
    }

    byte[] result = new byte[nChars / 2];

    for (int i = 0; i < nChars; i += 2) {
      int msb = Character.digit(s.charAt(i), 16);
      int lsb = Character.digit(s.charAt(i + 1), 16);

      if (msb < 0 || lsb < 0) {
        throw new IllegalArgumentException("Non-hex character in input: " + s);
      }
      result[i / 2] = (byte) ((msb << 4) | lsb);
    }
    return result;
  }
}
