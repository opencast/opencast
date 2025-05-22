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

package org.opencastproject.graphql.command;

import graphql.schema.DataFetchingEnvironment;

/**
 * AbstractCommand is an abstract class that implements the Command interface.
 * It provides a basic structure based on Joshua Block's builder design pattern.
 *
 * @param <T> the type of result that this command will produce
 */
public abstract class AbstractCommand<T> implements Command<T> {

  protected final DataFetchingEnvironment environment;

  public AbstractCommand(final Builder<T> builder) {
    this.environment = builder.environment;
  }

  public abstract static class Builder<B> {

    private DataFetchingEnvironment environment;

    public Builder<B> environment(DataFetchingEnvironment environment) {
      this.environment = environment;
      return this;
    }

    public void validate() {
      if (environment == null) {
        throw new IllegalStateException("DataFetchingEnvironment cannot be null");
      }
    }

    public abstract AbstractCommand<B> build();


  }

}
