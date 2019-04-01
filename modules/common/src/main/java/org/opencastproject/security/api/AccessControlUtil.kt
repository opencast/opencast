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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.security.api

import com.entwinemedia.fn.Prelude.chuck
import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE
import org.opencastproject.util.EqualsUtil.bothNotNull
import org.opencastproject.util.EqualsUtil.eqListUnsorted
import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.Checksum
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function2
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.fns.Booleans

import org.apache.commons.lang3.StringUtils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Comparator

/**
 * Provides common functions helpful in dealing with [AccessControlList]s.
 */
object AccessControlUtil {

    val toAclScope: Function<String, Option<AclScope>> = object : Function<String, Option<AclScope>>() {
        override fun apply(s: String): Option<AclScope> {
            try {
                return some(AclScope.valueOf(s))
            } catch (e: IllegalArgumentException) {
                return none()
            }

        }
    }

    private val sortAcl = Comparator<AccessControlEntry> { o1, o2 ->
        // compare role
        var compareTo = StringUtils.trimToEmpty(o1.role).compareTo(StringUtils.trimToEmpty(o2.role))
        if (compareTo != 0)
            return@Comparator compareTo

        // compare action
        compareTo = StringUtils.trimToEmpty(o1.action).compareTo(StringUtils.trimToEmpty(o2.action))
        if (compareTo != 0) compareTo else java.lang.Boolean.valueOf(o1.isAllow).compareTo(o2.isAllow)

        // compare allow
    }

    /**
     * Determines whether the [AccessControlList] permits a user to perform an action.
     *
     * There are three ways a user can be allowed to perform an action:
     *
     *  1. They have the superuser role
     *  1. They have their local organization's admin role
     *  1. They have a role listed in the series ACL, with write permission
     *
     *
     * @param acl
     * the [AccessControlList]
     * @param user
     * the user
     * @param org
     * the organization
     * @param action
     * The action to perform. `action` may be an arbitrary object. The authorization check is done on
     * the string representation of the object (`#toString()`). This allows to group actions as enums
     * and use them without converting them to a string manually. See
     * [org.opencastproject.security.api.Permissions.Action].
     * @return whether this action should be allowed
     * @throws IllegalArgumentException
     * if any of the arguments are null
     */
    fun isAuthorized(acl: AccessControlList?, user: User?, org: Organization?, action: Any?): Boolean {
        if (action == null || user == null || acl == null || org == null)
            throw IllegalArgumentException()

        // Check for the global and local admin role
        if (user.hasRole(GLOBAL_ADMIN_ROLE) || user.hasRole(org.adminRole))
            return true

        val userRoles = user.roles
        for (entry in acl.entries!!) {
            if (action.toString() != entry.action)
                continue

            val aceRole = entry.role
            for (role in userRoles) {
                if (role.name != aceRole)
                    continue

                return entry.isAllow
            }
        }
        return false
    }

    /**
     * [AccessControlUtil.isAuthorized]
     * as a predicate function.
     */
    private fun isAuthorizedFn(acl: AccessControlList, user: User, org: Organization): Pred<Any> {
        return object : Pred<Any>() {
            override fun apply(action: Any): Boolean? {
                return isAuthorized(acl, user, org, action)
            }
        }
    }

    /**
     * Returns true only if *all* actions are authorized.
     *
     * @see .isAuthorized
     */
    fun isAuthorizedAll(acl: AccessControlList, user: User, org: Organization, vararg actions: Any): Boolean {
        return !`$`(*actions).exists(Booleans.not(isAuthorizedFn(acl, user, org)))
    }

    /**
     * Returns true if at least *one* action is authorized.
     *
     * @see .isAuthorized
     */
    fun isAuthorizedOne(acl: AccessControlList, user: User, org: Organization, vararg actions: Any): Boolean {
        return `$`(*actions).exists(isAuthorizedFn(acl, user, org))
    }

    /**
     * Returns true if *all* actions are prohibited.
     *
     * @see .isAuthorized
     */
    fun isProhibitedAll(acl: AccessControlList, user: User, org: Organization, vararg actions: Any): Boolean {
        return !`$`(*actions).exists(isAuthorizedFn(acl, user, org))
    }

    /**
     * Returns true if at least *one* action is prohibited.
     *
     * @see .isAuthorized
     */
    fun isProhibitedOne(acl: AccessControlList, user: User, org: Organization, vararg actions: Any): Boolean {
        return `$`(*actions).exists(Booleans.not(isAuthorizedFn(acl, user, org)))
    }

    /**
     * Extends an access control list with an access control entry
     *
     * @param acl
     * the access control list to extend
     * @param role
     * the access control entry role
     * @param action
     * the access control entry action
     * @param allow
     * whether this access control entry role is allowed to take this action
     * @return the extended access control list or the same if already contained
     */
    fun extendAcl(acl: AccessControlList, role: String, action: String, allow: Boolean): AccessControlList {
        val newAcl = AccessControlList()
        var foundAce = false
        for (ace in acl.entries!!) {
            if (ace.action!!.equals(action, ignoreCase = true) && ace.role!!.equals(role, ignoreCase = true)) {
                if (ace.isAllow == allow) {
                    // Entry is already the same so just return the acl
                    return acl
                } else {
                    // We need to change the allow on the one entry.
                    foundAce = true
                    newAcl.entries!!.add(AccessControlEntry(role, action, allow))
                }
            } else {
                newAcl.entries!!.add(ace)
            }
        }
        if (!foundAce)
            newAcl.entries!!.add(AccessControlEntry(role, action, allow))

        return newAcl
    }

    /**
     * Reduces an access control list by an access control entry
     *
     * @param acl
     * the access control list to reduce
     * @param role
     * the role of the access control entry to remove
     * @param action
     * the action of the access control entry to remove
     * @return the reduced access control list or the same if already contained
     */
    fun reduceAcl(acl: AccessControlList, role: String, action: String): AccessControlList {
        val newAcl = AccessControlList()
        for (ace in acl.entries!!) {
            if (!ace.action!!.equals(action, ignoreCase = true) || !ace.role!!.equals(role, ignoreCase = true)) {
                newAcl.entries!!.add(ace)
            }
        }
        return newAcl
    }

    /**
     * Constructor function for ACLs.
     *
     * @see .entry
     * @see .entries
     */
    fun acl(vararg entries: Either<AccessControlEntry, List<AccessControlEntry>>): AccessControlList {
        // sequence entries
        val seq = mlist(*entries)
                .foldl(ArrayList(),
                        object : Function2<List<AccessControlEntry>, Either<AccessControlEntry, List<AccessControlEntry>>, List<AccessControlEntry>>() {
                            override fun apply(sum: List<AccessControlEntry>,
                                               current: Either<AccessControlEntry, List<AccessControlEntry>>): List<AccessControlEntry> {
                                if (current.isLeft)
                                    sum.add(current.left().value())
                                else
                                    sum.addAll(current.right().value())
                                return sum
                            }
                        })
        return AccessControlList(seq)
    }

    /** Create a single access control entry.  */
    fun entry(role: String, action: String, allow: Boolean): Either<AccessControlEntry, List<AccessControlEntry>> {
        return left(AccessControlEntry(role, action, allow))
    }

    /** Create a list of access control entries for a given role.  */
    fun entries(role: String,
                vararg actions: Tuple<String, Boolean>): Either<AccessControlEntry, List<AccessControlEntry>> {
        val entries = mlist(*actions).map(
                object : Function<Tuple<String, Boolean>, AccessControlEntry>() {
                    override fun apply(action: Tuple<String, Boolean>): AccessControlEntry {
                        return AccessControlEntry(role, action.a, action.b)
                    }
                }).value()
        return right(entries)
    }

    /**
     * Define equality on AccessControlLists. Two AccessControlLists are considered equal if they contain the exact same
     * entries no matter in which order.
     *
     *
     * This has not been implemented in terms of #equals and #hashCode because the list of entries is not immutable and
     * therefore not suitable to be put in a set.
     */
    fun equals(a: AccessControlList, b: AccessControlList): Boolean {
        return bothNotNull(a, b) && eqListUnsorted(a.entries, b.entries)
    }

    /** Calculate an MD5 checksum for an [AccessControlList].  */
    fun calculateChecksum(acl: AccessControlList): Checksum {
        // Use 0 as a word separator. This is safe since none of the UTF-8 code points
        // except \u0000 contains a null byte when converting to a byte array.
        val sep = byteArrayOf(0)
        val md = `$`(acl.entries).sort(sortAcl).bind(object : Fn<AccessControlEntry, Stream<String>>() {
            override fun apply(entry: AccessControlEntry): Stream<String> {
                return `$`(entry.role, entry.action, java.lang.Boolean.toString(entry.isAllow))
            }
        }).foldl(mkMd5MessageDigest(), object : Fn2<MessageDigest, String, MessageDigest>() {
            override fun apply(digest: MessageDigest, s: String): MessageDigest {
                digest.update(s.toByteArray(StandardCharsets.UTF_8))
                // add separator byte (see definition above)
                digest.update(sep)
                return digest
            }
        })

        try {
            return Checksum.create("md5", Checksum.convertToHex(md.digest()))
        } catch (e: NoSuchAlgorithmException) {
            return chuck(e)
        }

    }

    private fun mkMd5MessageDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return chuck(e)
        }

    }

}
/** Disallow construction of this utility class  */
