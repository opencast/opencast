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

package org.opencastproject.security.api;

import static org.opencastproject.util.EqualsUtil.bothNotNull;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides common functions helpful in dealing with {@link AccessControlList}s.
 */
public final class AccessControlUtil {

  /** Disallow construction of this utility class */
  private AccessControlUtil() {
  }

  /**
   * Extends an access control list with an access control entry
   *
   * @param acl
   *          the access control list to extend
   * @param role
   *          the access control entry role
   * @param action
   *          the access control entry action
   * @param allow
   *          whether this access control entry role is allowed to take this action
   * @return the extended access control list or the same if already contained
   */
  public static AccessControlList extendAcl(AccessControlList acl, String role, String action, boolean allow) {
    AccessControlList newAcl = new AccessControlList();
    boolean foundAce = false;
    for (AccessControlEntry ace : acl.getEntries()) {
      if (ace.getAction().equalsIgnoreCase(action) && ace.getRole().equalsIgnoreCase(role)) {
        if (ace.isAllow() == allow) {
          // Entry is already the same so just return the acl
          return acl;
        } else {
          // We need to change the allow on the one entry.
          foundAce = true;
          newAcl.getEntries().add(new AccessControlEntry(role, action, allow));
        }
      } else {
        newAcl.getEntries().add(ace);
      }
    }
    if (!foundAce) {
      newAcl.getEntries().add(new AccessControlEntry(role, action, allow));
    }

    return newAcl;
  }

  /**
   * Reduces an access control list by an access control entry
   *
   * @param acl
   *          the access control list to reduce
   * @param role
   *          the role of the access control entry to remove
   * @param action
   *          the action of the access control entry to remove
   * @return the reduced access control list or the same if already contained
   */
  public static AccessControlList reduceAcl(AccessControlList acl, String role, String action) {
    AccessControlList newAcl = new AccessControlList();
    for (AccessControlEntry ace : acl.getEntries()) {
      if (!ace.getAction().equalsIgnoreCase(action) || !ace.getRole().equalsIgnoreCase(role)) {
        newAcl.getEntries().add(ace);
      }
    }
    return newAcl;
  }

  /**
   * Constructor function for ACLs.
   *
   * @see #entry(String, String, boolean)
   * @see #entries(String, org.opencastproject.util.data.Tuple[])
   */
  public static AccessControlList acl(Either<AccessControlEntry, List<AccessControlEntry>>... entries) {
    // sequence entries
    List<AccessControlEntry> seq = new ArrayList<>();
    for (Either<AccessControlEntry, List<AccessControlEntry>> current : entries) {
      if (current.isLeft()) {
        seq.add(current.left().value());
      } else {
        seq.addAll(current.right().value());
      }
    }
    return new AccessControlList(seq);
  }


  /** Create a single access control entry. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entry(String role, String action, boolean allow) {
    return left(new AccessControlEntry(role, action, allow));
  }

  /** Create a list of access control entries for a given role. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entries(final String role,
      Tuple<String, Boolean>... actions) {
    List<AccessControlEntry> entries = Arrays.stream(actions)
        .map(action -> new AccessControlEntry(role, action.getA(), action.getB()))
        .toList();
    return right(entries);
  }

  /**
   * Define equality on AccessControlLists. Two AccessControlLists are considered equal if they contain the exact same
   * entries no matter in which order.
   * <p>
   * This has not been implemented in terms of #equals and #hashCode because the list of entries is not immutable and
   * therefore not suitable to be put in a set.
   */
  public static boolean equals(AccessControlList a, AccessControlList b) {
    return bothNotNull(a, b) && eqListUnsorted(a.getEntries(), b.getEntries());
  }

  /** Calculate an MD5 checksum for an {@link AccessControlList}. */
  public static Checksum calculateChecksum(AccessControlList acl) {
    // Use 0 as a word separator. This is safe since none of the UTF-8 code points
    // except \u0000 contains a null byte when converting to a byte array.
    final byte[] sep = new byte[] { 0 };

    // Sort ACL entries
    List<AccessControlEntry> sortedEntries = acl.getEntries().stream()
        .sorted(sortAcl)
        .collect(Collectors.toList());

    MessageDigest digest = mkMd5MessageDigest();

    for (AccessControlEntry entry : sortedEntries) {
      String[] fields = {
          entry.getRole(),
          entry.getAction(),
          Boolean.toString(entry.isAllow())
      };
      for (String field : fields) {
        digest.update(field.getBytes(StandardCharsets.UTF_8));
        // add separator byte (see definition above)
        digest.update(sep);
      }
    }

    try {
      return Checksum.create("md5", Checksum.convertToHex(digest.digest()));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static MessageDigest mkMd5MessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static Comparator<AccessControlEntry> sortAcl = new Comparator<AccessControlEntry>() {
    @Override
    public int compare(AccessControlEntry o1, AccessControlEntry o2) {
      // compare role
      int compareTo = StringUtils.trimToEmpty(o1.getRole()).compareTo(StringUtils.trimToEmpty(o2.getRole()));
      if (compareTo != 0) {
        return compareTo;
      }

      // compare action
      compareTo = StringUtils.trimToEmpty(o1.getAction()).compareTo(StringUtils.trimToEmpty(o2.getAction()));
      if (compareTo != 0) {
        return compareTo;
      }

      // compare allow
      return Boolean.valueOf(o1.isAllow()).compareTo(o2.isAllow());
    }
  };

}
