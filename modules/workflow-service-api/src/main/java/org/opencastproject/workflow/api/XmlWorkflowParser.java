/*
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
import org.opencastproject.util.XmlSafeParser;

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

/**
 * Provides a mechanism to un/marshall workflow instances and definitions to/from xml.
 */
public final class XmlWorkflowParser {

  private static final JAXBContext jaxbContext;

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.mediapackage");
    sb.append(":org.opencastproject.workflow.api");
    try {
      jaxbContext = JAXBContext.newInstance(sb.toString(), XmlWorkflowParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow instantiating this class */
  private XmlWorkflowParser() {
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
      WorkflowDefinitionImpl[] impls = unmarshaller.unmarshal(XmlSafeParser.parse(in), WorkflowDefinitionImpl[].class)
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
      return unmarshaller.unmarshal(XmlSafeParser.parse(in), WorkflowDefinitionImpl.class).getValue();
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
  public static WorkflowInstance parseWorkflowInstance(InputStream in) throws WorkflowParsingException {
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller
          .unmarshal(XmlSafeParser.parse(in), JaxbWorkflowInstance.class)
          .getValue()
          .toWorkflowInstance();
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
  public static WorkflowInstance parseWorkflowInstance(String in) throws WorkflowParsingException {
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
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(XmlSafeParser.parse(in), WorkflowSetImpl.class).getValue();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Converts a workflowInstance to an xml string
   *
   * @param workflowInstance
   *          the workflowInstance
   * @return workflowInstance as xml string
   * @throws WorkflowParsingException
   */
  public static String toXml(WorkflowInstance workflowInstance) throws WorkflowParsingException {
    return toXml(new JaxbWorkflowInstance(workflowInstance));
  }

  /**
   * Converts a xml annotated workflowInstance to an xml string
   *
   * @param workflowInstance
   *          the xml annotated workflowInstance
   * @return workflowInstance as xml string
   * @throws WorkflowParsingException
   */
  public static String toXml(JaxbWorkflowInstance workflowInstance) throws WorkflowParsingException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(workflowInstance, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new WorkflowParsingException(e);
    }

  }

  /**
   * Converts a workflowDefinition to an xml string
   *
   * @param workflowDefinition
   *          the workflowDefinition
   * @return workflowDefinition as xml string
   * @throws WorkflowParsingException
   */
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
}
