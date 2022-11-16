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
package org.opencastproject.email.template.impl;

import org.opencastproject.email.template.api.EmailTemplateService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.CatalogUIAdapter;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.util.doc.DocUtil;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component(
    immediate = true,
    service = EmailTemplateService.class,
    property = {
        "service.description=Email Template Service"
    }
)
public class EmailTemplateServiceImpl implements EmailTemplateService {
  private static final Logger logger = LoggerFactory.getLogger(EmailTemplateServiceImpl.class);

  public static final String DEFAULT_DELIMITER_FOR_MULTIPLE = ", ";

  /** The workspace (needed to read the catalogs when processing templates) **/
  private Workspace workspace;

  /** Email template scanner is optional and has dynamic policy */
  private final AtomicReference<EmailTemplateScanner> templateScannerRef = new AtomicReference<EmailTemplateScanner>();

  /** The incident service (to list errors in email) */
  private IncidentService incidentService = null;

  private IndexService indexService;

  private SecurityService securityService;

  @Activate
  protected void activate(ComponentContext context) {
    logger.info("EmailTemplateServiceImpl activated");
  }

  @Override
  public String applyTemplate(String templateName, String templateContent, WorkflowInstance workflowInstance) {
    return applyTemplate(templateName, templateContent, workflowInstance, DEFAULT_DELIMITER_FOR_MULTIPLE);
  }

  /**
   * Apply the template to the workflow instance.
   *
   * @param templateName
   *          template name
   * @param templateContent
   *          template content
   * @param workflowInstance
   *          workflow
   * @return text with applied template
   */
  @Override
  public String applyTemplate(String templateName, String templateContent, WorkflowInstance workflowInstance,
      String delimiter) {
    if (templateContent == null && templateScannerRef.get() != null) {
      templateContent = templateScannerRef.get().getTemplate(templateName);
    }

    if (templateContent == null) {
      logger.warn("E-mail template not found: {}", templateName);
      return "TEMPLATE NOT FOUND: " + templateName; // it's probably missing
    }

    // Build email data structure and apply the template
    HashMap<String, HashMap<String, String>> catalogs = initCatalogs(workflowInstance.getMediaPackage(), delimiter);

    WorkflowOperationInstance failed = findFailedOperation(workflowInstance);
    List<Incident> incidentList = null;
    if (failed != null) {
      try {
        IncidentTree incidents = incidentService.getIncidentsOfJob(failed.getId(), true);
        incidentList = generateIncidentList(incidents);
      } catch (Exception e) {
        logger.error("Error when populating template with incidents", e);
        // Incidents in email will be empty
      }
    }

    Map<String, String> orgProperties = null;
    Organization org = securityService.getOrganization();
    if (org != null) {
      orgProperties = org.getProperties();
    }

    return DocUtil.generate(new EmailData(templateName, workflowInstance, catalogs, failed, incidentList,
            orgProperties), templateContent);
  }

  /**
   * Initializes the map with all fields from the dublin core catalogs.
   */
  private HashMap<String, HashMap<String, String>> initCatalogs(MediaPackage mediaPackage, String delimiter) {
    HashMap<String, HashMap<String, String>> catalogs = new HashMap<String, HashMap<String, String>>();

    Set<MediaPackageElementFlavor> catalogFlavors = new HashSet<>();
    catalogFlavors.add(indexService.getCommonEventCatalogUIAdapter().getFlavor());
    catalogFlavors.add(indexService.getCommonSeriesCatalogUIAdapter().getFlavor());
    catalogFlavors.addAll(indexService.getEventCatalogUIAdapters().stream()
        .map(CatalogUIAdapter::getFlavor)
        .collect(Collectors.toSet()));
    catalogFlavors.addAll(indexService.getSeriesCatalogUIAdapters().stream()
        .map(CatalogUIAdapter::getFlavor)
        .collect(Collectors.toSet()));

    Set<Catalog> catalogElements = catalogFlavors.stream()
        .flatMap(f -> Arrays.stream(mediaPackage.getCatalogs(f)))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    for (Catalog c : catalogElements) {
      DublinCoreCatalog dc;
      try (InputStream in = workspace.read(c.getURI())) {
        dc = DublinCores.read(in);
      } catch (Exception e) {
        logger.warn("Error when populating catalog data", e);
        // Don't include the info
        continue;
      }

      HashMap<String, String> catalogHash = new HashMap<>();
      for (EName ename : dc.getProperties()) {
        String name = ename.getLocalName();
        catalogHash.put(name, dc.getAsText(ename, DublinCore.LANGUAGE_ANY, delimiter));
      }

      catalogs.put(c.getFlavor().toString(), catalogHash);
      // Backwards compatibility: use only subtype of the flavor as key
      if (c.getFlavor().getType().equals("dublincore")) {
        catalogs.put(c.getFlavor().getSubtype(), catalogHash);
      }
    }

    return catalogs;
  }

  /**
   * Traverses the workflow until it finds a failed operation that has failOnError=true
   *
   * @param workflow
   * @return the workflow operation that failed
   */
  private WorkflowOperationInstance findFailedOperation(WorkflowInstance workflow) {
    ArrayList<WorkflowOperationInstance> operations
        = new ArrayList<WorkflowOperationInstance>(workflow.getOperations());
    // Current operation is the email operation
    WorkflowOperationInstance emailOp = workflow.getCurrentOperation();
    // Look for the last operation that is in failed state and has failOnError true
    int i = operations.indexOf(emailOp) - 1;
    WorkflowOperationInstance op = null;
    for (; i >= 0; i--) {
      op = operations.get(i);
      if (OperationState.FAILED.equals(op.getState()) && op.isFailOnError()) {
        return op;
      }
    }
    return null;
  }

  /**
   * Generates list of all incidents in the tree
   *
   * @param tree
   *          the incident tree
   * @return a flat list of incidents
   */
  private List<Incident> generateIncidentList(IncidentTree tree) {
    List<Incident> list = new LinkedList<Incident>();
    if (tree != null && tree.getDescendants() != null && tree.getDescendants().size() > 0) {
      for (IncidentTree subtree : tree.getDescendants()) {
        list.addAll(generateIncidentList(subtree));
      }
    }
    list.addAll(tree.getIncidents());
    return list;
  }

  /**
   * Callback for OSGi to set the {@link Workspace}.
   *
   * @param ws
   *          the workspace
   */
  @Reference
  void setWorkspace(Workspace ws) {
    this.workspace = ws;
  }

  /**
   * Callback for OSGi to set the {@link EmailTemplateScanner}.
   *
   * @param templateScanner
   *          the template scanner service
   */
  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "unsetEmailTemplateScanner"
  )
  void setEmailTemplateScanner(EmailTemplateScanner templateScanner) {
    this.templateScannerRef.compareAndSet(null, templateScanner);
  }

  /**
   * Callback for OSGi to unset the {@link EmailTemplateScanner}.
   *
   * @param templateScanner
   *          the template scanner service
   */
  void unsetEmailTemplateScanner(EmailTemplateScanner templateScanner) {
    this.templateScannerRef.compareAndSet(templateScanner, null);
  }

  /**
   * Callback for OSGi to unset the {@link IncidentService}.
   *
   * @param incidentService
   *          the incident service
   */
  @Reference
  public void setIncidentService(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  @Reference
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  @Reference
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
