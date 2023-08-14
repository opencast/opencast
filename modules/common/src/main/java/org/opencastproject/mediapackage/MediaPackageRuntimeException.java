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


package org.opencastproject.mediapackage;

/**
 * This exception is used to wrap the checked {@link MediaPackageException} into a RuntimeException. This is useful to
 * create unchecked versions of some methods to use when in modern streams as in java8 or rxjava, where checked
 * exceptions should be avoided.
 */
public class MediaPackageRuntimeException extends RuntimeException {

  /** Serial version uid */
  private static final long serialVersionUID = -1545569836535459336L;

  private MediaPackageException wrappedException;

  /**
   * Creates a new media package runtime exception with the specified wrapped exception.
   *
   * @param wrappedException
   *          the wrapped {@link MediaPackageException}
   */
  public MediaPackageRuntimeException(MediaPackageException wrappedException) {
    this.wrappedException = wrappedException;
  }

  public MediaPackageException getWrappedException() {
    return wrappedException;
  }
}
