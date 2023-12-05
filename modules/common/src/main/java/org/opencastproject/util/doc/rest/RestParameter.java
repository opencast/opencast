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

package org.opencastproject.util.doc.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation type is used for annotating parameters for RESTful query. This annotation type needs to be kept until
 * runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestParameter {

  enum Type {
    NO_PARAMETER, // This is a special type to represent that there is no parameter. We need this because java
                  // annotation cannot be set to null.
    BOOLEAN, FILE, STRING, TEXT, INTEGER, FLOAT
  };

  /**
   * @return a name of the parameter.
   */
  String name();

  /**
   * @return a description of the parameter.
   */
  String description();

  /**
   * @return a default value of the parameter.
   */
  String defaultValue() default "";

  /**
   * @return a RestParameter.Type enum specifying the type of the parameter.
   */
  Type type();

  /**
   * @return the {@link javax.xml.bind.annotation.XmlType} or {@link javax.xml.bind.annotation.XmlRootElement} annotated
   *         class that models this parameter.
   */
  Class<?> jaxbClass() default Object.class;

  /**
   * @return a boolean indicating whether this parameter is required.
   */
  boolean isRequired();
}
