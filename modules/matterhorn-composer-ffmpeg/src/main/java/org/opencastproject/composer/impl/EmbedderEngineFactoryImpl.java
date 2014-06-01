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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EmbedderEngine;
import org.opencastproject.composer.api.EmbedderEngineFactory;
import org.opencastproject.composer.impl.qtembedder.QTSbtlEmbedderEngine;

import org.osgi.service.component.ComponentContext;

/**
 * Implementation of {@link EmbedderEngineFactory} that creates new {@link EmbedderEngine} instance.
 *
 */
public class EmbedderEngineFactoryImpl implements EmbedderEngineFactory {

  /** Component context from where configurations are retrieved */
  private ComponentContext context;

  /** Activates component via OSGi */
  public void activate(ComponentContext context) {
    this.context = context;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.EmbedderEngineFactory#newEmbedderEngine()
   */
  @Override
  public EmbedderEngine newEmbedderEngine() {
    QTSbtlEmbedderEngine engine = new QTSbtlEmbedderEngine();
    engine.activate(context);
    return engine;
  }

}
