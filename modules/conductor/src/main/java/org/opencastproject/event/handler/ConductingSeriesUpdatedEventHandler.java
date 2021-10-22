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

package org.opencastproject.event.handler;

import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.message.broker.api.update.ISeriesUpdateHandler;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simple approach to serialize the work of all three dependend update handlers. Todo: Merge all handlers into one
 * to avoid unnecessary distribution updates etc.
 */
public class ConductingSeriesUpdatedEventHandler implements ISeriesUpdateHandler {

  private static final Logger logger = LoggerFactory.getLogger(ConductingSeriesUpdatedEventHandler.class);

  private AssetManagerUpdatedEventHandler assetManagerUpdatedEventHandler;
  private SeriesUpdatedEventHandler seriesUpdatedEventHandler;
  private WorkflowPermissionsUpdatedEventHandler workflowPermissionsUpdatedEventHandler;


  public void activate(ComponentContext cc) {
    logger.info("Activating {}", ConductingSeriesUpdatedEventHandler.class.getName());
  }

  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", ConductingSeriesUpdatedEventHandler.class.getName());
  }

  @Override
  public void execute(SeriesItem seriesItem) {
    if (SeriesItem.Type.UpdateElement.equals(seriesItem.getType())) {
      assetManagerUpdatedEventHandler.handleEvent(seriesItem);
    } else if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())
                 || SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())
                 || SeriesItem.Type.Delete.equals(seriesItem.getType())) {
      seriesUpdatedEventHandler.handleEvent(seriesItem);
      assetManagerUpdatedEventHandler.handleEvent(seriesItem);
      workflowPermissionsUpdatedEventHandler.handleEvent(seriesItem);
    }
  }

  /** OSGi DI callback. */
  public void setAssetManagerUpdatedEventHandler(AssetManagerUpdatedEventHandler h) {
    this.assetManagerUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setSeriesUpdatedEventHandler(SeriesUpdatedEventHandler h) {
    this.seriesUpdatedEventHandler = h;
  }

  /** OSGi DI callback. */
  public void setWorkflowPermissionsUpdatedEventHandler(WorkflowPermissionsUpdatedEventHandler h) {
    this.workflowPermissionsUpdatedEventHandler = h;
  }

}
