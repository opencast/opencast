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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.mediapackage.selector

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementSelector

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.LinkedHashSet

/**
 * This selector will return any `MediaPackageElement`s from a `MediaPackage` that matches the tag
 * and flavors.
 */
abstract class AbstractMediaPackageElementSelector<T : MediaPackageElement> : MediaPackageElementSelector<T> {

    /** The tags  */
    protected var tags: MutableSet<String> = HashSet()

    /** The tags to exclude  */
    protected var excludeTags: MutableSet<String> = HashSet()

    /** The flavors  */
    protected var flavors: MutableList<MediaPackageElementFlavor> = ArrayList()

    /**
     * This base implementation will return those media package elements that match the type specified as the type
     * parameter to the class and that flavor (if specified) AND at least one of the tags (if specified) match.
     *
     * @see org.opencastproject.mediapackage.MediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<T> {
        return select(Arrays.asList(*mediaPackage.elements), withTagsAndFlavors)
    }

    override fun select(elements: List<MediaPackageElement>, withTagsAndFlavors: Boolean): Collection<T> {
        val result = LinkedHashSet<T>()

        // If no flavors and tags are set, return empty list
        if (flavors.isEmpty() && tags.isEmpty())
            return result

        val type = getParametrizedType(result)
        elementLoop@ for (e in elements) {

            // Does the type match?
            if (type.isAssignableFrom(e.javaClass)) {

                for (tag in e.tags) {
                    if (excludeTags.contains(tag))
                        continue@elementLoop
                }

                // Any of the flavors?
                var matchesFlavor = false
                for (flavor in flavors) {
                    if (flavor.matches(e.flavor)) {
                        matchesFlavor = true
                        break
                    }
                }

                if (flavors.isEmpty())
                    matchesFlavor = true

                // If the elements selection is done by tags AND flavors
                if (withTagsAndFlavors && matchesFlavor && e.containsTag(tags))
                    result.add(e as T)
                // Otherwise if only one of these parameters is necessary to select an element
                if (!withTagsAndFlavors && (!flavors.isEmpty() && matchesFlavor || !tags.isEmpty() && e.containsTag(tags)))
                    result.add(e as T)
            }
        }

        return result
    }

    /**
     * This constructor tries to determine the entity type from the type argument used by a concrete implementation of
     * `GenericHibernateDao`.
     *
     *
     * Note: This code will only work for immediate specialization, and especially not for subclasses.
     */
    private fun getParametrizedType(`object`: Any): Class<*> {
        var current: Class<*> = javaClass
        var superclass: Type
        var entityClass: Class<out T>? = null
        while ((superclass = current.genericSuperclass) != null) {
            if (superclass is ParameterizedType) {
                entityClass = superclass.actualTypeArguments[0] as Class<T>
                break
            } else if (superclass is Class<*>) {
                current = superclass
            } else {
                break
            }
        }
        if (entityClass == null) {
            throw IllegalStateException("Cannot determine entity type because " + javaClass.getName()
                    + " does not specify any type parameter.")
        }
        return entityClass
    }

    /**
     * Sets the flavors.
     *
     *
     * Note that the order is relevant to the selection of the track returned by this selector.
     *
     * @param flavors
     * the list of flavors
     * @throws IllegalArgumentException
     * if the flavors list is `null`
     */
    fun setFlavors(flavors: MutableList<MediaPackageElementFlavor>?) {
        if (flavors == null)
            throw IllegalArgumentException("List of flavors must not be null")
        this.flavors = flavors
    }

    /**
     * Adds the given flavor to the list of flavors.
     *
     *
     * Note that the order is relevant to the selection of the track returned by this selector.
     *
     * @param flavor
     */
    fun addFlavor(flavor: MediaPackageElementFlavor?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        if (!flavors.contains(flavor))
            flavors.add(flavor)
    }

    /**
     * Adds the given flavor to the list of flavors.
     *
     *
     * Note that the order is relevant to the selection of the track returned by this selector.
     *
     * @param flavor
     */
    fun addFlavor(flavor: String?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        val f = MediaPackageElementFlavor.parseFlavor(flavor)
        if (!flavors.contains(f))
            flavors.add(f)
    }

    /**
     * Adds the given flavor to the list of flavors.
     *
     *
     * Note that the order is relevant to the selection of the track returned by this selector.
     *
     * @param index
     * the position in the list
     * @param flavor
     * the flavor to add
     */
    fun addFlavorAt(index: Int, flavor: MediaPackageElementFlavor?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        flavors.add(index, flavor)
        for (i in index + 1 until flavors.size) {
            if (flavors[i] == flavor)
                flavors.removeAt(i)
        }
    }

    /**
     * Adds the given flavor to the list of flavors.
     *
     *
     * Note that the order is relevant to the selection of the track returned by this selector.
     *
     * @param index
     * the position in the list
     * @param flavor
     * the flavor to add
     */
    fun addFlavorAt(index: Int, flavor: String?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        val f = MediaPackageElementFlavor.parseFlavor(flavor)
        flavors.add(index, f)
        for (i in index + 1 until flavors.size) {
            if (flavors[i] == f)
                flavors.removeAt(i)
        }
    }

    /**
     * Removes all occurences of the given flavor from the list of flavors.
     *
     * @param flavor
     * the flavor to remove
     */
    fun removeFlavor(flavor: MediaPackageElementFlavor?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        flavors.remove(flavor)
    }

    /**
     * Removes all occurences of the given flavor from the list of flavors.
     *
     * @param flavor
     * the flavor to remove
     */
    fun removeFlavor(flavor: String?) {
        if (flavor == null)
            throw IllegalArgumentException("Flavor must not be null")
        flavors.remove(MediaPackageElementFlavor.parseFlavor(flavor))
    }

    /**
     * Removes all occurences of the given flavor from the list of flavors.
     *
     * @param index
     * the position in the list
     */
    fun removeFlavorAt(index: Int) {
        flavors.removeAt(index)
    }

    /**
     * Returns the list of flavors.
     *
     * @return the flavors
     */
    fun getFlavors(): Array<MediaPackageElementFlavor> {
        return flavors.toTypedArray<MediaPackageElementFlavor>()
    }

    /**
     * Adds `tag` to the list of tags that are used to select the media.
     *
     * @param tag
     * the tag to include
     */
    fun addTag(tag: String) {
        if (tag.startsWith(NEGATE_TAG_PREFIX)) {
            excludeTags.add(tag.substring(NEGATE_TAG_PREFIX.length))
        } else {
            tags.add(tag)
        }
    }

    /**
     * Adds `tag` to the list of tags that are used to select the media.
     *
     * @param tag
     * the tag to include
     */
    fun removeTag(tag: String) {
        tags.remove(tag)
    }

    /**
     * Returns the tags.
     *
     * @return the tags
     */
    fun getTags(): Array<String> {
        return tags.toTypedArray<String>()
    }

    /**
     * Removes all of the tags from this selector.
     */
    fun clearTags() {
        tags.clear()
    }

    companion object {

        /**
         * The prefix indicating that a tag should be excluded from a search for elements using
         * [MediaPackage.getElementsByTags]
         */
        val NEGATE_TAG_PREFIX = "-"
    }

}
