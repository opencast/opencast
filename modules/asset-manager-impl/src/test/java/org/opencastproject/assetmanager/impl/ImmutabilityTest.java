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
package org.opencastproject.assetmanager.impl;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.Test;

import javax.annotation.concurrent.Immutable;

public class ImmutabilityTest {

  @Test
  public void testImmutability() {
    final JavaClasses valueObjects = new ClassFileImporter().importPackages("org.opencastproject.assetmanager.api");

    final ArchRule fieldsAreImmutable = fields()
        .that()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(Immutable.class)
        .should()
        .beFinal();

    fieldsAreImmutable.check(valueObjects);

  }
}
