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
package org.opencastproject.annotation.api;

import org.opencastproject.util.NotFoundException;

/**
 * Manages user annotations within media.
 */
public interface AnnotationService {
  /**
   * Adds a new annotation to the database and returns the event with an updated annotationId, to make sure the
   * annotationId stays unique
   * 
   * @param a
   *          The Annotation that will be added to the database
   * @return the updated annotation, with a new ID. NULL if there are errors while adding the annotation.
   */
  Annotation addAnnotation(Annotation a);

  /**
   * Remove a given annotation from database
   * 
   * @param a
   *          The Annotation that will be removed from the database
   * @return true if successfull removed, false else.
   */
  boolean removeAnnotation(Annotation a);

  /**  
   * Gets an annotation by its identifier.
   * 
   * @param id
   *          the annotation identifier
   * @return the annotation
   * @throws NotFoundException if there is no annotation with this identifier
   */
  Annotation getAnnotation(long id) throws NotFoundException;

  /**
   * Returns annotations
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotations(int offset, int limit);


  /**
   * Returns annotations of a given mediapackage ID
   *
   * @param mediapackageId
   *          The mediapackage ID
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByMediapackageId(String mediapackageId, int offset, int limit);

  /**
   * Returns annotations of a given type
   * 
   * @param type
   *          The annotation type
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByType(String type, int offset, int limit);

  /**
   * Returns annotations of a given day (YYYYMMDD)
   * 
   * @param day
   *          The day in the format of YYYYMMDD
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByDay(String day, int offset, int limit);

  /**
   * Returns annotations of a given type and day
   * 
   * @param type
   *          the annotation type
   * @param day
   *          the day
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByTypeAndDay(String type, String day, int offset, int limit);

  /**
   * Returns annotations of a given type and mediapackage id
   * 
   * @param type
   *          the annotation type
   * @param mediapackageId
   *          the mediapackage id
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the annotation list
   */
  AnnotationList getAnnotationsByTypeAndMediapackageId(String type, String mediapackageId, int offset, int limit);

}
