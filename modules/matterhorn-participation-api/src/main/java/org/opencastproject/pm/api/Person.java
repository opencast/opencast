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
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.security.api.User;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

/** Business object for a person. */
public class Person implements Blacklistable {

  public static final String TYPE = "person";

  /** The person's identifier */
  private Long id;

  /** The person's name */
  private String name;

  /** The person's email */
  private String email;

  /** The person's types */
  private List<PersonType> personTypes;

  /**
   * Creates a person
   * 
   * @param id
   *          the id
   * @param name
   *          the name
   * @param email
   *          the email
   * @param personTypes
   *          the person's types
   */
  public Person(Long id, String name, String email, List<PersonType> personTypes) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.personTypes = new ArrayList<PersonType>(notNull(personTypes, "personTypes"));
  }

  public static Person person(String name, String email, List<PersonType> personTypes) {
    return new Person(null, name, email, personTypes);
  }

  public static Person person(String name, String email) {
    return new Person(null, name, email, nil(PersonType.class));
  }

  public static Person fromUser(User user) {
    return new Person(null, user.getName(), user.getEmail(), nil(PersonType.class));
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the person id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the person id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  public static final Function<Person, Option<Long>> getId = new Function<Person, Option<Long>>() {
    @Override
    public Option<Long> apply(Person person) {
      return option(person.getId());
    }
  };

  /**
   * Sets the name
   * 
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name
   * 
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the email
   * 
   * @param email
   *          the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Returns the email
   * 
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  public EmailAddress getEmailAddress() {
    return new EmailAddress(email, name);
  }

  /**
   * Sets the person types
   * 
   * @param personTypes
   *          the person types
   */
  public void setPersonTypes(List<PersonType> personTypes) {
    this.personTypes = personTypes;
  }

  /**
   * Returns the person types
   * 
   * @return the person types
   */
  public List<PersonType> getPersonTypes() {
    return personTypes;
  }

  /**
   * Add a type to the person
   * 
   * @param type
   *          the type to add to this person
   * @return true if this collection changed as a result of the call
   */
  public boolean addType(PersonType type) {
    return personTypes.add(notNull(type, "type"));
  }

  /**
   * Remove a type from the person
   * 
   * @param type
   *          the type to remove from this person
   * @return true if this collection changed as a result of the call
   */
  public boolean removeType(PersonType type) {
    return personTypes.remove(notNull(type, "type"));
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Person person = (Person) o;
    return name.equals(person.getName()) && email.equals(person.getEmail())
            && personTypes.equals(person.getPersonTypes());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, email, personTypes);
  }

  @Override
  public String toString() {
    return "Person:" + name;
  }

  public Obj toJson() {
    List<Jsons.Val> pArr = new ArrayList<Jsons.Val>();
    for (PersonType t : personTypes) {
      pArr.add(t.toJson());
    }

    return Jsons.obj(Jsons.p("id", id), Jsons.p("name", name), Jsons.p("email", email),
            Jsons.p("types", Jsons.arr(pArr)));
  }

}
