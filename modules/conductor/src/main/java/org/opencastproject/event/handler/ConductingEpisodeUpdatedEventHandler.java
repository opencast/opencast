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

import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler listens for changes to episodes. Whenever a change is done, this is propagated to OAI-PMH.
 */
@Component(
    immediate = true,
    service = {
        AssetManagerUpdateHandler.class
    },
    property = {
        "service.description=Conducting event handler for recording events",
    }
)
public class ConductingEpisodeUpdatedEventHandler implements AssetManagerUpdateHandler {

  private static final Logger logger = LoggerFactory.getLogger(ConductingEpisodeUpdatedEventHandler.class);

  private OaiPmhUpdatedEventHandler oaiPmhUpdatedEventHandler;

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating {}", ConductingEpisodeUpdatedEventHandler.class.getName());
  }

  @Deactivate
  public void deactivate(ComponentContext cc) {
    logger.info("Deactivating {}", ConductingEpisodeUpdatedEventHandler.class.getName());
  }

  @Override
  public void execute(AssetManagerItem messageItem) {
    if (! (messageItem instanceof AssetManagerItem.TakeSnapshot)) {
      // We don't want to handle anything but TakeSnapshot messages.
      return;
    }
    AssetManagerItem.TakeSnapshot snapshotItem = (AssetManagerItem.TakeSnapshot) messageItem;
    if (AssetManagerItem.Type.Update.equals(snapshotItem.getType())) {
      // the OAI-PMH handler is a dynamic dependency
      if (oaiPmhUpdatedEventHandler != null) {
        oaiPmhUpdatedEventHandler.handleEvent(snapshotItem);
      }
    }
  }

  /**
   * OSGi DI callback.
   */
  @Reference
  public void setOaiPmhUpdatedEventHandler(OaiPmhUpdatedEventHandler h) {
    this.oaiPmhUpdatedEventHandler = h;
  }
}
