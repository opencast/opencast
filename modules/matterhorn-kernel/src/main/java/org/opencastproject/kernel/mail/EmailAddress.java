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
package org.opencastproject.kernel.mail;

import static org.opencastproject.util.EqualsUtil.eqObj;
import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;

/** An email address. */
public final class EmailAddress {
  private final String address;
  private final String name;

  public EmailAddress(String address, String name) {
    this.address = address;
    this.name = name;
  }

  public static EmailAddress emailAddress(String address, String name) {
    return new EmailAddress(address, name);
  }

  public String getAddress() {
    return address;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return hash(address, name);
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof EmailAddress && eqFields((EmailAddress) that));
  }

  private boolean eqFields(EmailAddress that) {
    return eqObj(this.address, that.address) && eqObj(this.name, that.name);
  }

  @Override
  public String toString() {
    return name + " <" + address + ">";
  }

  public static final Function<EmailAddress, String> getAddress = new Function<EmailAddress, String>() {
    @Override
    public String apply(EmailAddress a) {
      return a.getAddress();
    }
  };

  public static final Function<EmailAddress, String> getName = new Function<EmailAddress, String>() {
    @Override
    public String apply(EmailAddress a) {
      return a.getName();
    }
  };

  public Obj toJson() {
    return Jsons.obj(Jsons.p("address", address), Jsons.p("name", name));
  }
}
