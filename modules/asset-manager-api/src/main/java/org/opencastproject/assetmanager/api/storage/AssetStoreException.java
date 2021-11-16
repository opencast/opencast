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
package org.opencastproject.assetmanager.api.storage;

public class AssetStoreException extends RuntimeException {
  private static final long serialVersionUID = -5519744635333639394L;

  public AssetStoreException() {
  }

  public AssetStoreException(String message) {
    super(message);
  }

  public AssetStoreException(Throwable cause) {
    super(cause);
  }

  public AssetStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
