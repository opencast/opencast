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

package org.opencastproject.mediapackage;

import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A <code>MediaPackageElementRef</code> provides means of pointing to other elements in the media package.
 * <p>
 * A metadata catalog could for example contain a reference to the track that was used to extract the data contained in
 * it.
 * </p>
 */
@XmlJavaTypeAdapter(MediaPackageReferenceImpl.Adapter.class)
public interface MediaPackageReference extends Cloneable {

  String TYPE_MEDIAPACKAGE = "mediapackage";
  String TYPE_TRACK = "track";
  String TYPE_CATALOG = "catalog";
  String TYPE_ATTACHMENT = "attachment";
  String TYPE_SERIES = "series";
  String SELF = "self";
  String ANY = "*";

  /**
   * Returns the reference type.
   * <p>
   * There is a list of well known types describing media package elements:
   * <ul>
   * <li><code>mediapackage</code> a reference to the parent media package</li>
   * <li><code>track</code> referes to a track inside the media package</li>
   * <li><code>catalog</code> referes to a catalog inside the media package</li>
   * <li><code>attachment</code> referes to an attachment inside the media package</li>
   * <li><code>series</code> referes to a series</li>
   * </ul>
   *
   * @return the reference type
   */
  String getType();

  /**
   * Returns the reference identifier.
   * <p>
   * The identifier will usually refer to the id of the media package element, should the reference point to an element
   * inside the media package (see {@link MediaPackageElement#getIdentifier()}).
   * <p>
   * In case of a reference to another media package, this will reflect the media package id (see
   * {@link MediaPackage#getIdentifier()}) or <code>self</code> if it refers to the parent media package.
   *
   * @return the reference identifier
   */
  String getIdentifier();

  /**
   * Returns <code>true</code> if this reference matches <code>reference</code> by means of type and identifier.
   *
   * @param reference
   *          the media package reference
   * @return <code>true</code> if the reference matches
   */
  boolean matches(MediaPackageReference reference);

  /**
   * Returns additional properties that further define what the object is referencing.
   * <p>
   * An example would be the point in time for a slide preview:
   *
   * <pre>
   *  &lt;attachment ref="track:track-7;time=8764"&gt;
   *  &lt;/attachment&gt;
   * </pre>
   *
   * @return the properties of this reference
   */
  Map<String, String> getProperties();

  /**
   * Returns the property with name <code>key</code> or <code>null</code> if no such property exists.
   *
   * @param key
   *          the property name
   * @return the property value
   */
  String getProperty(String key);

  /**
   * Adds an additional property to further define the object reference. Set the value to null in order to remove a
   * property.
   *
   * @param key
   *          The unique key
   * @param value
   *          The value of the property
   */
  void setProperty(String key, String value);

  /**
   * Returns a deep copy of this reference.
   *
   * @return the clone
   */
  Object clone();

}
