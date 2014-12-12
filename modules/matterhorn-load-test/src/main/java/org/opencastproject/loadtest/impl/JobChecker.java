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
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

/** Checks every interval whether the ingest jobs have finished executing. **/
public class JobChecker implements Runnable {
  //The logger.
  private static final Logger logger = LoggerFactory.getLogger(JobChecker.class);
  // The collection of ingest jobs that we need to check.
  private LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();
  // The load testing context that this will be executing in.
  private LoadTest loadTest = null;
  // The client to use to connect to the core for the ingest.
  private TrustedHttpClient client = null;

  /**
   * Checks each of the ingest jobs to find out if they are finished yet or not.
   *
   * @param ingestJobs
   *          The ingest jobs that we are meant to check.
   * @param newLoadTesting
   *          The load testing context we will be checking these jobs in.
   */
  public JobChecker(LinkedList<IngestJob> ingestJobs, LoadTest loadTesting, TrustedHttpClient newClient) {
    this.ingestJobs = ingestJobs;
    this.loadTest = loadTesting;
    client = newClient;
  }

  /**
   * Use the TrustedHttpClient from matterhorn to check the status of jobs.
   *
   * @param id
   *          The media package id to check.
   * @param mediaPackageLocation
   *          The location of the mediapackage we want to ingest.
   */
  private void checkJobWithJava(IngestJob job) {
    String id = job.getID();
    logger.info("Checking recording: {}", id);
    try {
      URL url = new URL(loadTest.getCoreAddress() + "/workflow/instance/" + id);
      logger.debug("Check Job URL is " + url.toString());
      HttpGet getMethod = new HttpGet(url.toString());

      // Send the request
      HttpResponse response = null;
      int retValue = -1;
      try {
        response = client.execute(getMethod);
      } catch (TrustedHttpClientException e) {
        logger.error("Unable to check ingest {}, message reads: {}.", id, e.getMessage());
      } catch (NullPointerException e) {
        logger.error("Unable to check ingest {}, null pointer exception!", id);
      } finally {
        if (response != null) {
          retValue = response.getStatusLine().getStatusCode();
          client.close(response);
        } else {
          retValue = -1;
        }
      }

      if (retValue == HttpURLConnection.HTTP_OK) {
        logger.info(id + " successfully ingested, is now processing and will be marked as done.");
        ingestJobs.remove(job);
        ThreadCounter.subtract();
      }
      else {
        logger.info(id + " has not ingested yet.");
      }

    } catch (MalformedURLException e) {
      logger.error("Malformed URL for ingest target \"" + loadTest.getCoreAddress()
              + "/ingest/addZippedMediaPackage\"");
    }
  }

  /**
   * Check the ingest jobs to see if they have started on the core yet at a set interval in the configuration file until
   * they have all started.
   *
   * @see java.lang.Runnable#run()
   **/
  @Override
  public void run() {
    while (!ThreadCounter.allDone()) {
      try {
        Thread.sleep(loadTest.getJobCheckInterval() * LoadTest.MILLISECONDS_IN_SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      for (IngestJob ingestJob : ingestJobs) {
        checkJobWithJava(ingestJob);
      }

    }
  }
}
