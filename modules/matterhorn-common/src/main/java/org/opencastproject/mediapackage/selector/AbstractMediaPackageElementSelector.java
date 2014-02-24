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
package org.opencastproject.mediapackage.selector;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementSelector;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This selector will return any <code>MediaPackageElement</code>s from a <code>MediaPackage</code> that matches the tag
 * and flavors.
 */
public abstract class AbstractMediaPackageElementSelector<T extends MediaPackageElement> implements
        MediaPackageElementSelector<T> {

  /** The tags */
  protected Set<String> tags = new HashSet<String>();

  /** The tags to exclude */
  protected Set<String> excludeTags = new HashSet<String>();

  /** The flavors */
  protected List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();

  /**
   * The prefix indicating that a tag should be excluded from a search for elements using
   * {@link MediaPackage#getElementsByTags(Collection)}
   */
  public static final String NEGATE_TAG_PREFIX = "-";

  /**
   * This base implementation will return those media package elements that match the type specified as the type
   * parameter to the class and that flavor (if specified) AND at least one of the tags (if specified) match.
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage,
   *      boolean)
   */
  @SuppressWarnings("unchecked")
  public Collection<T> select(MediaPackage mediaPackage, boolean withTagsAndFlavors) {
    Set<T> result = new HashSet<T>();
    Class type = getParametrizedType(result);
    elementLoop: for (MediaPackageElement e : mediaPackage.getElements()) {

      // Does the type match?
      if (type.isAssignableFrom(e.getClass())) {

        for (String tag : e.getTags()) {
          if (excludeTags.contains(tag))
            continue elementLoop;
        }

        // If no flavors and tags are set, add all elements
        if (flavors.isEmpty() && tags.isEmpty()) {
          result.add((T) e);
          continue;
        }

        // Any of the flavors?
        boolean matchesFlavor = false;
        for (MediaPackageElementFlavor flavor : flavors) {
          if (flavor.matches(e.getFlavor())) {
            matchesFlavor = true;
            break;
          }
        }

        if (flavors.isEmpty())
          matchesFlavor = true;

        // If the elements selection is done by tags AND flavors
        if (withTagsAndFlavors && matchesFlavor && e.containsTag(tags))
          result.add((T) e);
        // Otherwise if only one of these parameters is necessary to select an element
        if (!withTagsAndFlavors && ((!flavors.isEmpty() && matchesFlavor) || (!tags.isEmpty() && e.containsTag(tags))))
          result.add((T) e);
      }
    }

    return result;
  }

  /**
   * This constructor tries to determine the entity type from the type argument used by a concrete implementation of
   * <code>GenericHibernateDao</code>.
   * <p>
   * Note: This code will only work for immediate specialization, and especially not for subclasses.
   */
  @SuppressWarnings("unchecked")
  private Class getParametrizedType(Object object) {
    Class current = getClass();
    Type superclass;
    Class<? extends T> entityClass = null;
    while ((superclass = current.getGenericSuperclass()) != null) {
      if (superclass instanceof ParameterizedType) {
        entityClass = (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
        break;
      } else if (superclass instanceof Class) {
        current = (Class) superclass;
      } else {
        break;
      }
    }
    if (entityClass == null) {
      throw new IllegalStateException("Cannot determine entity type because " + getClass().getName()
              + " does not specify any type parameter.");
    }
    return entityClass;
  }

  /**
   * Sets the flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
   * 
   * @param flavors
   *          the list of flavors
   * @throws IllegalArgumentException
   *           if the flavors list is <code>null</code>
   */
  public void setFlavors(List<MediaPackageElementFlavor> flavors) {
    if (flavors == null)
      throw new IllegalArgumentException("List of flavors must not be null");
    this.flavors = flavors;
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
   * 
   * @param flavor
   */
  public void addFlavor(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    if (!flavors.contains(flavor))
      flavors.add(flavor);
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
   * 
   * @param flavor
   */
  public void addFlavor(String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    MediaPackageElementFlavor f = MediaPackageElementFlavor.parseFlavor(flavor);
    if (!flavors.contains(f))
      flavors.add(f);
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
   * 
   * @param index
   *          the position in the list
   * @param flavor
   *          the flavor to add
   */
  public void addFlavorAt(int index, MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.add(index, flavor);
    for (int i = index + 1; i < flavors.size(); i++) {
      if (flavors.get(i).equals(flavor))
        flavors.remove(i);
    }
  }

  /**
   * Adds the given flavor to the list of flavors.
   * <p>
   * Note that the order is relevant to the selection of the track returned by this selector.
   * 
   * @param index
   *          the position in the list
   * @param flavor
   *          the flavor to add
   */
  public void addFlavorAt(int index, String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    MediaPackageElementFlavor f = MediaPackageElementFlavor.parseFlavor(flavor);
    flavors.add(index, f);
    for (int i = index + 1; i < flavors.size(); i++) {
      if (flavors.get(i).equals(f))
        flavors.remove(i);
    }
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param flavor
   *          the flavor to remove
   */
  public void removeFlavor(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.remove(flavor);
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param flavor
   *          the flavor to remove
   */
  public void removeFlavor(String flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null");
    flavors.remove(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Removes all occurences of the given flavor from the list of flavors.
   * 
   * @param index
   *          the position in the list
   */
  public void removeFlavorAt(int index) {
    flavors.remove(index);
  }

  /**
   * Returns the list of flavors.
   * 
   * @return the flavors
   */
  public MediaPackageElementFlavor[] getFlavors() {
    return flavors.toArray(new MediaPackageElementFlavor[flavors.size()]);
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the media.
   * 
   * @param tag
   *          the tag to include
   */
  public void addTag(String tag) {
    if (tag.startsWith(NEGATE_TAG_PREFIX)) {
      excludeTags.add(tag.substring(NEGATE_TAG_PREFIX.length()));
    } else {
      tags.add(tag);
    }
  }

  /**
   * Adds <code>tag</code> to the list of tags that are used to select the media.
   * 
   * @param tag
   *          the tag to include
   */
  public void removeTag(String tag) {
    tags.remove(tag);
  }

  /**
   * Returns the tags.
   * 
   * @return the tags
   */
  public String[] getTags() {
    return tags.toArray(new String[tags.size()]);
  }

  /**
   * Removes all of the tags from this selector.
   */
  public void clearTags() {
    tags.clear();
  }

}
