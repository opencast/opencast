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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation that waits for a caption file to be uploaded.
 */
public class CaptionUploadWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptionUploadWorkflowOperationHandler.class);

  /** The key used to find the configured caption flavor */
  private static final String FLAVOR_PROPERTY = "caption-flavor";
  
  /** The key used to find the configured targets tags */
  private static final String TARGET_TAGS_PROPERTY = "target-tags";
  
  /** The key used to find the configured action in case of mediapackage containing captions */
  private static final String OVERWRITE_CAPTIONS_PROPERTY = "overwriteCaption";

  /** The default caption flavor if none is configured */
  private static final MediaPackageElementFlavor DEFAULT_FLAVOR = MediaPackageElements.CAPTION_GENERAL;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** Path to the caption upload ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/caption/index.html";

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(FLAVOR_PROPERTY, "The configuration key that identifies the required caption flavor.");
  }

  public void activate(ComponentContext cc) {
    super.activate(cc);
    setHoldActionTitle("Caption Upload");
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering caption upload hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.handler.ResumableWorkflowOperationHandlerBase#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {

    boolean overwrite = Boolean.parseBoolean(workflowInstance.getConfiguration(OVERWRITE_CAPTIONS_PROPERTY));
    
    boolean hasCaption = false;
    
    MediaPackageElement[] captionsElements = workflowInstance.getMediaPackage().getElementsByFlavor(DEFAULT_FLAVOR);
    
    if (captionsElements.length > 0 && !overwrite)
      return createResult(Action.CONTINUE);
    else
      return createResult(Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context, Map<String, String> properties)
          throws WorkflowOperationException {
    
     MediaPackageElementFlavor flavor = getFlavor(workflowInstance.getCurrentOperation());
     
     boolean hasCaptions = hasCaptions(workflowInstance.getMediaPackage(), flavor);
     
     if (hasCaptions) {
         String tagsStr = workflowInstance.getCurrentOperation().getConfiguration(TARGET_TAGS_PROPERTY); 
         
          // Get all the targets-tags from the operation configuration
          if (tagsStr != null) {
            String[] tags = tagsStr.split(",");
         
            MediaPackageElement[] mpElements = workflowInstance.getMediaPackage().getElementsByFlavor(DEFAULT_FLAVOR);
         
            for (MediaPackageElement mpElement : mpElements)
              for (String tag : tags)
                mpElement.addTag(tag);
          }        
     } 
    
     
    return createResult(Action.CONTINUE);  
  }

  protected boolean hasCaptions(MediaPackage mp, MediaPackageElementFlavor flavor) {
    return (mp.getElementsByFlavor(flavor).length > 0) || (mp.getElementsByFlavor(DEFAULT_FLAVOR).length > 0);
  }

  protected MediaPackageElementFlavor getFlavor(WorkflowOperationInstance operation) throws WorkflowOperationException {
    // Not properties not use for the moment, but will be when SRT,WebTT,etc will be supported
    String configuredFlavor = operation.getConfiguration(FLAVOR_PROPERTY);
    if (configuredFlavor == null) {
      return DEFAULT_FLAVOR;
    } else {
      try {
        return MediaPackageElementFlavor.parseFlavor(configuredFlavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e.getMessage(), e);
      }
    }
  }
}
