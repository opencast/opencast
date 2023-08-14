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

package org.opencastproject.security.api;

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.EqualsUtil.bothNotNull;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.fns.Booleans;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Provides common functions helpful in dealing with {@link AccessControlList}s.
 */
public final class AccessControlUtil {

  /** Disallow construction of this utility class */
  private AccessControlUtil() {
  }

  /**
   * Determines whether the {@link AccessControlList} permits a user to perform an action.
   *
   * There are three ways a user can be allowed to perform an action:
   * <ol>
   * <li>They have the superuser role</li>
   * <li>They have their local organization's admin role</li>
   * <li>They have a role listed in the series ACL, with write permission</li>
   * </ol>
   *
   * @param acl
   *          the {@link AccessControlList}
   * @param user
   *          the user
   * @param org
   *          the organization
   * @param action
   *          The action to perform. <code>action</code> may be an arbitrary object. The authorization check is done on
   *          the string representation of the object (<code>#toString()</code>). This allows to group actions as enums
   *          and use them without converting them to a string manually. See
   *          {@link org.opencastproject.security.api.Permissions.Action}.
   * @return whether this action should be allowed
   * @throws IllegalArgumentException
   *           if any of the arguments are null
   */
  public static boolean isAuthorized(AccessControlList acl, User user, Organization org, Object action) {
    if (action == null || user == null || acl == null || org == null)
      throw new IllegalArgumentException();

    // Check for the global and local admin role
    if (user.hasRole(GLOBAL_ADMIN_ROLE) || user.hasRole(org.getAdminRole()))
      return true;

    Set<Role> userRoles = user.getRoles();
    for (AccessControlEntry entry : acl.getEntries()) {
      if (!action.toString().equals(entry.getAction()))
        continue;

      String aceRole = entry.getRole();
      for (Role role : userRoles) {
        if (!role.getName().equals(aceRole))
          continue;

        return entry.isAllow();
      }
    }
    return false;
  }

  /**
   * {@link AccessControlUtil#isAuthorized(org.opencastproject.security.api.AccessControlList, org.opencastproject.security.api.User, org.opencastproject.security.api.Organization, Object)}
   * as a predicate function.
   */
  private static Pred<Object> isAuthorizedFn(final AccessControlList acl, final User user, final Organization org) {
    return new Pred<Object>() {
      @Override
      public Boolean apply(Object action) {
        return isAuthorized(acl, user, org, action);
      }
    };
  }

  /**
   * Returns true only if <em>all</em> actions are authorized.
   *
   * @see #isAuthorized(AccessControlList, User, Organization, Object)
   */
  public static boolean isAuthorizedAll(AccessControlList acl, User user, Organization org, Object... actions) {
    return !$(actions).exists(Booleans.not(isAuthorizedFn(acl, user, org)));
  }

  /**
   * Returns true if at least <em>one</em> action is authorized.
   *
   * @see #isAuthorized(AccessControlList, User, Organization, Object)
   */
  public static boolean isAuthorizedOne(AccessControlList acl, User user, Organization org, Object... actions) {
    return $(actions).exists(isAuthorizedFn(acl, user, org));
  }

  /**
   * Returns true if <em>all</em> actions are prohibited.
   *
   * @see #isAuthorized(AccessControlList, User, Organization, Object)
   */
  public static boolean isProhibitedAll(AccessControlList acl, User user, Organization org, Object... actions) {
    return !$(actions).exists(isAuthorizedFn(acl, user, org));
  }

  /**
   * Returns true if at least <em>one</em> action is prohibited.
   *
   * @see #isAuthorized(AccessControlList, User, Organization, Object)
   */
  public static boolean isProhibitedOne(AccessControlList acl, User user, Organization org, Object... actions) {
    return $(actions).exists(Booleans.not(isAuthorizedFn(acl, user, org)));
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
    if (!foundAce)
      newAcl.getEntries().add(new AccessControlEntry(role, action, allow));

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
    final List<AccessControlEntry> seq = mlist(entries)
            .foldl(new ArrayList<AccessControlEntry>(),
                    new Function2<List<AccessControlEntry>, Either<AccessControlEntry, List<AccessControlEntry>>, List<AccessControlEntry>>() {
                      @Override
                      public List<AccessControlEntry> apply(List<AccessControlEntry> sum,
                              Either<AccessControlEntry, List<AccessControlEntry>> current) {
                        if (current.isLeft())
                          sum.add(current.left().value());
                        else
                          sum.addAll(current.right().value());
                        return sum;
                      }
                    });
    return new AccessControlList(seq);
  }

  /** Create a single access control entry. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entry(String role, String action, boolean allow) {
    return left(new AccessControlEntry(role, action, allow));
  }

  /** Create a list of access control entries for a given role. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entries(final String role,
          Tuple<String, Boolean>... actions) {
    final List<AccessControlEntry> entries = mlist(actions).map(
            new Function<Tuple<String, Boolean>, AccessControlEntry>() {
              @Override
              public AccessControlEntry apply(Tuple<String, Boolean> action) {
                return new AccessControlEntry(role, action.getA(), action.getB());
              }
            }).value();
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
    final MessageDigest md = $(acl.getEntries()).sort(sortAcl).bind(new Fn<AccessControlEntry, Stream<String>>() {
      @Override
      public Stream<String> apply(AccessControlEntry entry) {
        return $(entry.getRole(), entry.getAction(), Boolean.toString(entry.isAllow()));
      }
    }).foldl(mkMd5MessageDigest(), new Fn2<MessageDigest, String, MessageDigest>() {
      @Override
      public MessageDigest apply(MessageDigest digest, String s) {
        digest.update(s.getBytes(StandardCharsets.UTF_8));
        // add separator byte (see definition above)
        digest.update(sep);
        return digest;
      }
    });

    try {
      return Checksum.create("md5", Checksum.convertToHex(md.digest()));
    } catch (NoSuchAlgorithmException e) {
      return chuck(e);
    }
  }

  private static MessageDigest mkMd5MessageDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      return chuck(e);
    }
  }

  private static Comparator<AccessControlEntry> sortAcl = new Comparator<AccessControlEntry>() {
    @Override
    public int compare(AccessControlEntry o1, AccessControlEntry o2) {
      // compare role
      int compareTo = StringUtils.trimToEmpty(o1.getRole()).compareTo(StringUtils.trimToEmpty(o2.getRole()));
      if (compareTo != 0)
        return compareTo;

      // compare action
      compareTo = StringUtils.trimToEmpty(o1.getAction()).compareTo(StringUtils.trimToEmpty(o2.getAction()));
      if (compareTo != 0)
        return compareTo;

      // compare allow
      return Boolean.valueOf(o1.isAllow()).compareTo(o2.isAllow());
    }
  };

}
