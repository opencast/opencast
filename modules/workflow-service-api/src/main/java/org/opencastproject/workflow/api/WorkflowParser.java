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

import org.opencastproject.util.IoSupport;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

/**
 * Provides a mechanism to un/marshall workflow instances and definitions to/from xml.
 */
public final class WorkflowParser {

  private static final JAXBContext jaxbContext;

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.mediapackage");
    sb.append(":org.opencastproject.workflow.api");
    try {
      jaxbContext = JAXBContext.newInstance(sb.toString(), WorkflowParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow instantiating this class */
  private WorkflowParser() {
  }

  /**
   * Loads workflow definitions from the given input stream.
   *
   * @param in
   * @return the list of workflow definitions
   */
  public static List<WorkflowDefinition> parseWorkflowDefinitions(InputStream in) throws WorkflowParsingException {
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      WorkflowDefinitionImpl[] impls = unmarshaller.unmarshal(new StreamSource(in), WorkflowDefinitionImpl[].class)
              .getValue();
      List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
      for (WorkflowDefinitionImpl impl : impls) {
        list.add(impl);
      }
      return list;
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
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), WorkflowDefinitionImpl.class).getValue();
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
  public static WorkflowInstanceImpl parseWorkflowInstance(InputStream in) throws WorkflowParsingException {
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      WorkflowInstanceImpl workflow = unmarshaller.unmarshal(new StreamSource(in), WorkflowInstanceImpl.class)
              .getValue();
      workflow.init();
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
  public static WorkflowInstanceImpl parseWorkflowInstance(String in) throws WorkflowParsingException {
    try {
      return parseWorkflowInstance(IOUtils.toInputStream(in, "UTF8"));
    } catch (IOException e) {
      throw new WorkflowParsingException(e);
    }
  }

  /**
   * Loads workflow statistics from the given input stream.
   *
   * @param in
   *          the input stream
   * @return the workflow statistics
   * @throws WorkflowParsingException
   *           if creating the workflow statistics fails
   */
  public static WorkflowStatistics parseWorkflowStatistics(InputStream in) throws WorkflowParsingException {
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), WorkflowStatistics.class).getValue();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Loads workflow statistics from the given xml string.
   *
   * @param xml
   *          the xml serialized representation of the workflow statistics
   * @return the workflow statistics
   * @throws WorkflowParsingException
   *           if creating the workflow statistics fails
   */
  public static WorkflowStatistics parseWorkflowStatistics(String xml) throws WorkflowParsingException {
    try {
      return parseWorkflowStatistics(IOUtils.toInputStream(xml, "UTF8"));
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
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), WorkflowSetImpl.class).getValue();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Loads a set of workflow instances from the xml string.
   *
   * @param in
   *          xml string of the workflow instance set
   * @return the workflow set
   * @throws WorkflowParsingException
   *           if creating the workflow instance set fails
   */
  public static WorkflowSet parseWorkflowSet(String in) throws WorkflowParsingException {
    try {
      return parseWorkflowSet(IOUtils.toInputStream(in, "UTF8"));
    } catch (IOException e) {
      throw new WorkflowParsingException(e);
    }
  }

  public static String toXml(WorkflowInstance workflowInstance) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(workflowInstance, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }

  }

  public static String toXml(WorkflowDefinition workflowDefinition) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(workflowDefinition, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }

  public static String toXml(List<WorkflowDefinition> list) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(new WorkflowDefinitionSet(list), writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }

  public static String toXml(WorkflowSet set) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(set, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }

  public static String toXml(WorkflowStatistics stats) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(stats, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }
  }

}
