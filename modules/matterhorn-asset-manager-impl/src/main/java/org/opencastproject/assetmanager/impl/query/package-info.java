/**
 * Copyright 2009, 2010 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 *
 * Default query implementation.
 *
 * <h1>General implementation notes</h1>
 * Do not alias entities when creating Querydsl fragments.
 * <code>new QPropertyDto("alias")</code>
 * The code generator in {@link org.opencastproject.assetmanager.impl.query.AbstractASelectQuery#run()}
 * will get into serious trouble because it relies on equality checks on {@link org.opencastproject.assetmanager.impl.persistence.EntityPaths}
 * and other Querydsl types.
 */

/**
 * Default query implementation.
 *
 * <h1>General implementation notes</h1>
 * Do not alias entities when creating Querydsl fragments like this
 * <code>new QPropertyDto("alias")</code>.
 * The code generator in {@link org.opencastproject.assetmanager.impl.query.AbstractASelectQuery#run()}
 * will get into serious trouble because it relies on equality checks on {@link org.opencastproject.assetmanager.impl.persistence.EntityPaths}
 * and other Querydsl types.
 */
package org.opencastproject.assetmanager.impl.query;
