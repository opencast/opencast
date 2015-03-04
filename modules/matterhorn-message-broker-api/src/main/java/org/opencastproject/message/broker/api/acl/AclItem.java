/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.message.broker.api.acl;

import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through an Acl queue.
 */
public class AclItem implements Serializable {

  private static final long serialVersionUID = -8329403993622629220L;

  public static final String ACL_QUEUE_PREFIX = "ACL.";

  public static final String ACL_QUEUE = ACL_QUEUE_PREFIX + "QUEUE";

  private String currentAclName;
  private String newAclName;
  private final Type type;

  public enum Type {
    Create, Update, Delete
  };

  /**
   * @return Builds {@link AclItem} for deleting an acl.
   */
  public static AclItem create(String currentAclName) {
    return new AclItem(currentAclName, Type.Create);
  }

  /**
   * @param mediapackage
   *          The mediapackage to update.
   * @param acl
   *          The access control list of the mediapackage to update.
   * @param version
   *          The version of the mediapackage.
   * @param date
   *          The modification date.
   * @return Builds a {@link AclItem} for updating a mediapackage.
   */
  public static AclItem update(String currentAclName, String newAclName) {
    return new AclItem(currentAclName, newAclName);
  }

  /**
   * @return Builds {@link AclItem} for deleting an acl.
   */
  public static AclItem delete(String currentAclName) {
    return new AclItem(currentAclName, Type.Delete);
  }

  /**
   * Constructor to build an Update {@link AclItem}
   */
  public AclItem(String currentAclName, String newAclName) {
    this.currentAclName = currentAclName;
    this.newAclName = newAclName;
    this.type = Type.Update;
  }

  /**
   * Constructor to build a create or delete {@link AclItem}.
   */
  public AclItem(String currentAclName, Type type) {
    this.currentAclName = currentAclName;
    this.type = type;
  }

  public String getCurrentAclName() {
    return currentAclName;
  }

  public String getNewAclName() {
    return newAclName;
  }

  public Type getType() {
    return type;
  }

}
