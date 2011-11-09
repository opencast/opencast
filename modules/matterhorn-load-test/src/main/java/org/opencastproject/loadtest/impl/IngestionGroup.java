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
package org.opencastproject.loadtest.impl;

import org.opencastproject.security.api.TrustedHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

/** Keeps track of a group of media packages that will be ingested together at the same time. **/
public class IngestionGroup {
  // The logger.
  private static final Logger logger = LoggerFactory.getLogger(IngestionGroup.class);
  // The collection of ingestion jobs we will be executing.
  private LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();

  /**
   * This is a collection of ingests that will all be run at the same time.
   * 
   * @param numberOfIngests
   *          The number of ingests that we will be executing at the same time.
   * @param delayToIngest
   *          The amount of time in minutes to wait until starting these ingests.
   * @param newLoadTesting
   *          The load testing context these ingests will be done in.
   * @param client
   *          The TrustedHttpClient we will use to ingest to the core.
   */
  public IngestionGroup(long numberOfIngests, long delayToIngest, LoadTest loadTest, TrustedHttpClient client) {
    IngestJob newJob;
    SecureRandom random = new SecureRandom();
    Thread thread;
    logger.info("This ingestion group has " + numberOfIngests + " ingests in it.");
    for (long i = 0; i < numberOfIngests; i++) {
      newJob = new IngestJob(new BigInteger(130, random).toString(32), delayToIngest, loadTest, client);
      logger.info("Creating job " + newJob);
      ingestJobs.add(newJob);
      thread = new Thread(newJob);
      thread.start();
    }
  }
  
  
  /**
   * 
   * @return The ingest jobs that will be executed at the same time.  
   */
  public Collection<IngestJob> getJobs() {
    return ingestJobs;
  }
}
