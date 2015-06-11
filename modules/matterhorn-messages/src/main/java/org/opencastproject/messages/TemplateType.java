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

package org.opencastproject.messages;

import java.util.List;

/** Algebraic type describing the different template (or email) types. */
public abstract class TemplateType {
  private TemplateType() {
  }

  public enum Type {
    INVITATION(TemplateType.INVITATION), REMEMBER(null), THANK_YOU(null), ACKNOWLEDGE(TemplateType.ACKNOWLEDGE);

    private final TemplateType type;

    private Type(TemplateType type) {
      this.type = type;
    }

    /** Get the respective template type implementation. */
    public TemplateType getType() {
      return type;
    }
  }

  /** Return the type identifier. */
  public abstract Type getType();

  // todo provide information about template data suitable to be displayed in the UI

  public static final Invitation INVITATION = new Invitation();
  public static final Acknowledge ACKNOWLEDGE = new Acknowledge();

  /** The invitation email template. */
  public static final class Invitation extends TemplateType {
    private Invitation() {
    }

    @Override
    public Type getType() {
      return Type.INVITATION;
    }

    public static Data data(String staff, String optOutLink, List<Module> modules) {
      return new Data(staff, optOutLink, modules);
    }

    public static Module module(String name, String description) {
      return new Module(name, description);
    }

    /** Template data for an invitation email template. */
    public static final class Data {
      private final String staff;
      private final String optOutLink;
      private final List<Module> modules;

      public Data(String staff, String optOutLink, List<Module> modules) {
        this.staff = staff;
        this.optOutLink = optOutLink;
        this.modules = modules;
      }

      public String getStaff() {
        return staff;
      }

      public String getOptOutLink() {
        return optOutLink;
      }

      public List<Module> getModules() {
        return modules;
      }

    }

    public static final class Module {
      private final String name;
      private final String description;

      public Module(String name, String description) {
        this.name = name;
        this.description = description;
      }

      public String getName() {
        return name;
      }

      public String getDescription() {
        return description;
      }
    }
  }

  public static final class Acknowledge extends TemplateType {
    @Override
    public Type getType() {
      return Type.ACKNOWLEDGE;
    }
  }

  // todo implement other template types
}
