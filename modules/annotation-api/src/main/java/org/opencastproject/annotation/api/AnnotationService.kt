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

package org.opencastproject.annotation.api

import org.opencastproject.util.NotFoundException

/**
 * Manages user annotations within media.
 */
interface AnnotationService {
    /**
     * Adds a new annotation to the database and returns the event with an updated annotationId, to make sure the
     * annotationId stays unique
     *
     * @param a
     * The Annotation that will be added to the database
     * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
     */
    fun addAnnotation(a: Annotation): Annotation

    /**
     * Changes an annotation in the database it uses the annotationId to overwrite the value. It returns the event with an updated annotation.
     *
     * @param a
     * The Annotation that will be changed in the database
     * @return the updated annotation. NULL if there are errors while changing the annotation.
     */
    @Throws(NotFoundException::class)
    fun changeAnnotation(a: Annotation): Annotation

    /**
     * Remove a given annotation from database
     *
     * @param a
     * The Annotation that will be removed from the database
     * @return true if successfull removed, false else.
     */
    fun removeAnnotation(a: Annotation): Boolean

    /**
     * Gets an annotation by its identifier.
     *
     * @param id
     * the annotation identifier
     * @return the annotation
     * @throws NotFoundException if there is no annotation with this identifier
     */
    @Throws(NotFoundException::class)
    fun getAnnotation(id: Long): Annotation

    /**
     * Returns annotations
     *
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotations(offset: Int, limit: Int): AnnotationList


    /**
     * Returns annotations of a given mediapackage ID
     *
     * @param mediapackageId
     * The mediapackage ID
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotationsByMediapackageId(mediapackageId: String, offset: Int, limit: Int): AnnotationList

    /**
     * Returns annotations of a given type
     *
     * @param type
     * The annotation type
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotationsByType(type: String, offset: Int, limit: Int): AnnotationList

    /**
     * Returns annotations of a given day (YYYYMMDD)
     *
     * @param day
     * The day in the format of YYYYMMDD
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotationsByDay(day: String, offset: Int, limit: Int): AnnotationList

    /**
     * Returns annotations of a given type and day
     *
     * @param type
     * the annotation type
     * @param day
     * the day
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotationsByTypeAndDay(type: String, day: String, offset: Int, limit: Int): AnnotationList

    /**
     * Returns annotations of a given type and mediapackage id
     *
     * @param type
     * the annotation type
     * @param mediapackageId
     * the mediapackage id
     * @param offset
     * the offset
     * @param limit
     * the limit
     * @return the annotation list
     */
    fun getAnnotationsByTypeAndMediapackageId(type: String, mediapackageId: String, offset: Int, limit: Int): AnnotationList

}
