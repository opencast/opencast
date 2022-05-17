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

import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;

import org.opencastproject.util.IoSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a mechanism to un/marshall workflow instances and definitions to/from xml.
 */
public final class YamlWorkflowParser {

  private static final ObjectMapper mapper;

  static {
    ObjectMapper om = YAMLMapper.builder().configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true).build();
    om.registerModule(new JaxbAnnotationModule());
    SimpleModule sm = new SimpleModule();
    sm.addDeserializer(JaxbWorkflowConfiguration.class, new YamlWorkflowConfigurationDeserializer());
    sm.addSerializer(JaxbWorkflowConfiguration.class, new YamlWorkflowConfigurationSerializer());
    om.registerModule(sm);
    mapper = om;
  }

  /** Disallow instantiating this class */
  private YamlWorkflowParser() {
  }

  /**
   * Loads workflow definitions from the given input stream.
   *
   * @param in
   * @return the list of workflow definitions
   */
  public static List<WorkflowDefinition> parseWorkflowDefinitions(InputStream in) throws WorkflowParsingException {
    try {
      WorkflowDefinitionImpl[] impls = mapper.readValue(in, WorkflowDefinitionImpl[].class);

      return new ArrayList<>(Arrays.asList(impls));
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Loads a workflow definition from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the workflow definition
   * @throws WorkflowParsingException
   *           if creating the workflow definition fails
   */
  public static WorkflowDefinition parseWorkflowDefinition(InputStream in) throws WorkflowParsingException {
    try {
      return mapper.readValue(in,  WorkflowDefinitionImpl.class);
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Loads a workflow definition from the xml stream.
   *
   * @param in
   *          xml stream of the workflow definition
   * @return the workflow definition
   * @throws WorkflowParsingException
   *           if creating the workflow definition fails
   */
  public static WorkflowDefinition parseWorkflowDefinition(String in) throws WorkflowParsingException {
    try {
      return parseWorkflowDefinition(IOUtils.toInputStream(in, "UTF8"));
    } catch (IOException e) {
      throw new WorkflowParsingException(e);
    }
  }

  /**
   * Loads a workflow instance from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the workflow instance
   * @throws WorkflowParsingException
   *           if creating the workflow instance fails
   */
  public static JaxbWorkflowConfiguration parseWorkflowInstance(InputStream in) throws WorkflowParsingException {
    try {
      JaxbWorkflowConfiguration workflow = mapper.readValue(in, JaxbWorkflowConfiguration.class);
      return workflow;
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Loads a workflow instance from the xml stream.
   *
   * @param in
   *          xml stream of the workflow instance
   * @return the workflow instance
   * @throws WorkflowParsingException
   *           if creating the workflow instance fails
   */
  public static JaxbWorkflowConfiguration parseWorkflowInstance(String in) throws WorkflowParsingException {
    try {
      return parseWorkflowInstance(IOUtils.toInputStream(in, "UTF8"));
    } catch (IOException e) {
      throw new WorkflowParsingException(e);
    }
  }

  /**
   * Loads a set of workflow instances from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the set of workflow instances
   * @throws WorkflowParsingException
   *           if creating the workflow instance set fails
   */
  public static WorkflowSet parseWorkflowSet(InputStream in) throws WorkflowParsingException {
    try {
      return mapper.readValue(in, WorkflowSetImpl.class);
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  public static String toYaml(WorkflowInstance workflowInstance) throws WorkflowParsingException {
    try {
      return mapper.writeValueAsString(workflowInstance);
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }

  public static String toYaml(WorkflowDefinition workflowDefinition) throws WorkflowParsingException {
    try {
      return mapper.writeValueAsString(workflowDefinition);
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }



}
