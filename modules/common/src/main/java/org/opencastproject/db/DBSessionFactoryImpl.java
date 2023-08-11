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

package org.opencastproject.db;

import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;

@Component(
    property = {
        "service.description=DB Session Factory"
    },
    immediate = true,
    service = { DBSessionFactory.class }
)
public class DBSessionFactoryImpl implements DBSessionFactory {
  private static final Logger logger = LoggerFactory.getLogger(DBSessionFactoryImpl.class);

  private int maxTransactionRetries = DEFAULT_MAX_TRANSACTION_RETRIES;
  public static final String MAX_TRANSACTION_RETRIES_PROPERTY = "transaction.retries.max";
  public static final int DEFAULT_MAX_TRANSACTION_RETRIES = 5;

  @Activate
  public void activate(ComponentContext cc) throws ComponentException {
    logger.info("Activate DB session factory");
    modified(cc);
  }

  @Modified
  public void modified(ComponentContext cc) {
    var properties = cc.getProperties();

    maxTransactionRetries = NumberUtils.toInt((String) properties.get(MAX_TRANSACTION_RETRIES_PROPERTY),
        DEFAULT_MAX_TRANSACTION_RETRIES);
  }

  @Override
  public DBSession createSession(EntityManagerFactory emf) {
    return createSession(emf, maxTransactionRetries);
  }

  public DBSession createSession(EntityManagerFactory emf, int maxTransactionRetries) {
    var db = new DBSessionImpl(emf);
    db.setMaxTransactionRetries(maxTransactionRetries);
    return db;
  }

  public int getMaxTransactionRetries() {
    return maxTransactionRetries;
  }

  public void setMaxTransactionRetries(int maxTransactionRetries) {
    this.maxTransactionRetries = maxTransactionRetries;
  }
}
