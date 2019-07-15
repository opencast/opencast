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

package org.opencastproject.workflow.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A workflow definition.
 */
@XmlJavaTypeAdapter(WorkflowDefinitionImpl.Adapter.class)
public interface WorkflowDefinition extends Comparable<WorkflowDefinition> {

  /**
   * The variable in a workflow definition that is to be replaced by the reason for an operation's failure.
   */
  String FAILURE_KEY = "failure.message";

  /**
   * The short title of this workflow definition
   */
  String getId();

  /**
   * Sets the identifier
   *
   * @param id
   *          the workflow definition identifier
   */
  void setId(String id);

  /**
   * The title for this workflow definition
   */
  String getTitle();

  /**
   * Sets the title
   *
   * @param title
   *          the workflow definition title
   */
  void setTitle(String title);

  /**
   * A longer description of this workflow definition
   */
  String getDescription();

  /**
   * Sets the description
   *
   * @param description
   *          the workflow definition description
   */
  void setDescription(String description);

  /**
   * An XML String describing the configuration parameter/panel for this WorkflowDefinition.
   */
  String getConfigurationPanel();

  /**
   * An integer describing the display order for this workflow definition. The display order is supposed to define the
   * order workflow lists as displayed to users.
   * Default is 0.
   */
  int getDisplayOrder();

  /**
   * Set the display order
   *
   * @param displayOrder
   *          the workflow definition display order
   */
  void setDisplayOrder(int displayOrder);

  /**
   * The operations, listed in order, that this workflow definition includes.
   */
  List<WorkflowOperationDefinition> getOperations();

  /**
   * The custom state mappings to use for this workflow.
   */
  Set<WorkflowStateMapping> getStateMappings();

  /**
   * Tags the workflow definition with the given tag.
   *
   * @param tag
   *          the tag
   */
  void addTag(String tag);

  /**
   * Removes the tag from the workflow definition.
   *
   * @param tag
   *          the tag
   */
  void removeTag(String tag);

  /**
   * Returns <code>true</code> if the workflow definition contains the given tag.
   *
   * @param tag
   *          the tag
   * @return <code>true</code> if the element is tagged
   */
  boolean containsTag(String tag);

  /**
   * Returns <code>true</code> if the workflow definition contains at least one of the given tags. If there are no tags
   * contained in the set, then the definition is considered to match as well.
   *
   * @param tags
   *          the set of tag
   * @return <code>true</code> if the element is tagged accordingly
   */
  boolean containsTag(Collection<String> tags);

  /**
   * Returns the tags for this workflow definition or an empty array if there are no tags.
   *
   * @return the tags
   */
  String[] getTags();

  /**
   * Returns the roles for this workflow definition
   *
   * @return the tags
   */
  Collection<String> getRoles();

  /**
   * Removes all tags associated with this workflow definition
   */
  void clearTags();

  /**
   * Gets the organization associated with this workflow (or <code>null</code>, if it's global)
   * @return the organization
   */
  String getOrganization();

  /**
   * Appends the operation to the workflow definition.
   *
   * @param operation
   *          the operation
   */
  void add(WorkflowOperationDefinition operation);

  /**
   * Inserts the operation at the given position into the workflow definition.
   *
   * @param operation
   *          the operation
   * @param position
   *          the position to add the workflow operation to
   * @throws IndexOutOfBoundsException
   *           if <code>position</code> is larger than the number of operations + 1
   */
  void add(WorkflowOperationDefinition operation, int position);

  /**
   * Returns the operation at the given position.
   *
   * @param position
   *          the operation's position
   * @throws IndexOutOfBoundsException
   *           if <code>position</code> is larger than the number of operations
   */
  WorkflowOperationDefinition get(int position);

  /**
   * Removes the workflow operation at the indicated position and returns it.
   *
   * @param position
   *          the operation's position
   * @return the removed workflow operation
   * @throws IndexOutOfBoundsException
   *           if <code>position</code> is larger than the number of operations
   */
  WorkflowOperationDefinition remove(int position) throws IndexOutOfBoundsException;

}
