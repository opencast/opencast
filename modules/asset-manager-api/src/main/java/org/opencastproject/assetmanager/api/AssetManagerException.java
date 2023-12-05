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
package org.opencastproject.assetmanager.api;

import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

/**
 * A common exception indicating various issues.
 */
public class AssetManagerException extends RuntimeException {
  public AssetManagerException() {
  }

  public AssetManagerException(String message) {
    super(message);
  }

  public AssetManagerException(String message, Throwable cause) {
    super(message, cause);
  }

  public AssetManagerException(Throwable cause) {
    super(cause);
  }

  /** Returns true if the exception is caused by a {@link org.opencastproject.security.api.UnauthorizedException}. */
  // todo is an authorization failure really unrecoverable?
  public boolean isCauseNotAuthorized() {
    return getCause() instanceof UnauthorizedException;
  }

  /** Returns true if the exception is caused by a {@link org.opencastproject.util.NotFoundException}. */
  public boolean isCauseNotFound() {
    return getCause() instanceof NotFoundException;
  }

}
