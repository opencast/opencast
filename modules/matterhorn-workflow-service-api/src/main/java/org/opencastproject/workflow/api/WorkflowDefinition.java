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
package org.opencastproject.workflow.api;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A workflow definition.
 */
@XmlJavaTypeAdapter(WorkflowDefinitionImpl.Adapter.class)
public interface WorkflowDefinition {

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
   * The operations, listed in order, that this workflow definition includes.
   */
  List<WorkflowOperationDefinition> getOperations();

  /**
   * Whether this definition is published. This information is useful for user interfaces.
   *
   * @return Whether this is a published workflow definition
   */
  boolean isPublished();

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
   * Removes all tags associated with this workflow definition
   */
  void clearTags();

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
