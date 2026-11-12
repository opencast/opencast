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

package org.opencastproject.serviceregistry.impl;

import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.api.SystemLoad.NodeLoad;
import org.opencastproject.serviceregistry.impl.jpa.HostRegistrationJpaImpl;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

/**
 * Tests for {@link ServiceRegistryJpaImpl.LoadComparator} and {@link ServiceRegistryJpaImpl.LoadComparatorEncoding},
 * which are used to sort service registrations by load when dispatching jobs. Both need to be consistent, i.e. obey
 * {@link Comparator}'s general contract, or {@code Collections.sort}/{@code List.sort} can throw
 * "Comparison method violates its general contract!" once enough services are being compared to make TimSort notice
 * the inconsistency (see <a href="https://github.com/opencast/opencast/issues/2745">#2745</a>).
 */
public class LoadComparatorTest {

  private static final String JOB_TYPE = "org.opencastproject.testservice";
  private static final String COMPOSER_JOB_TYPE = "org.opencastproject.composer";

  private final ServiceRegistryJpaImpl serviceRegistry = new ServiceRegistryJpaImpl();

  @After
  public void tearDown() {
    // encodingWorkers/encodingThreshold are static, i.e. shared across every instance and test in this JVM
    ServiceRegistryJpaImpl.encodingWorkers = ServiceRegistryJpaImpl.DEFAULT_ENCODING_WORKERS;
    ServiceRegistryJpaImpl.encodingThreshold = ServiceRegistryJpaImpl.DEFAULT_ENCODING_THRESHOLD;
  }

  private HostRegistration host(String baseUrl) {
    return new HostRegistrationJpaImpl(baseUrl, "127.0.0.1", baseUrl, 1024, 1, 1.0f, true, false);
  }

  private ServiceRegistration service(HostRegistration host, String jobType) {
    return new ServiceRegistrationJpaImpl((HostRegistrationJpaImpl) host, jobType, "/path", true);
  }

  /**
   * Asserts that a comparator satisfies its general contract (reflexivity, antisymmetry and transitivity) for every
   * pair and triple in {@code elements}. This is a stronger check than just calling {@code Collections.sort} and
   * hoping it doesn't throw, since whether TimSort notices an inconsistency depends on the exact merge pattern it
   * happens to use for a given input.
   */
  private void assertObeysGeneralContract(Comparator<ServiceRegistration> comparator,
          List<ServiceRegistration> elements) {
    for (ServiceRegistration a : elements) {
      Assert.assertEquals("not reflexive for " + a.getHost(), 0, comparator.compare(a, a));
      for (ServiceRegistration b : elements) {
        int ab = Integer.signum(comparator.compare(a, b));
        int ba = Integer.signum(comparator.compare(b, a));
        Assert.assertEquals("not antisymmetric for " + a.getHost() + " and " + b.getHost(), -ab, ba);
      }
    }
    for (ServiceRegistration a : elements) {
      for (ServiceRegistration b : elements) {
        if (comparator.compare(a, b) > 0) {
          continue;
        }
        for (ServiceRegistration c : elements) {
          if (comparator.compare(b, c) > 0) {
            continue;
          }
          Assert.assertTrue(
                  String.format("not transitive: %s <= %s <= %s, but %s > %s", a.getHost(), b.getHost(),
                          c.getHost(), a.getHost(), c.getHost()),
                  comparator.compare(a, c) <= 0);
        }
      }
    }
  }

  /**
   * Builds a system load with many services whose load factors are close enough together to create lots of
   * candidates for the "are these two loads about the same" tie-break, which is exactly the situation the
   * comparators need to handle consistently for every pair.
   */
  private List<ServiceRegistration> manyServicesWithCloseLoadFactors(String jobType, SystemLoad systemLoad,
          long seed, int count) {
    Random random = new Random(seed);
    List<ServiceRegistration> services = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String hostUrl = "http://worker" + i + ".example.org";
      HostRegistration host = host(hostUrl);
      services.add(service(host, jobType));
      // Keeping current and maximum load in modest, overlapping ranges means many nodes end up with a load factor
      // within 0.01 of each other, so the tie-break is exercised a lot.
      float maxLoad = 4 + random.nextInt(12);
      float currentLoad = random.nextInt(100) / 10f;
      systemLoad.addNodeLoad(new NodeLoad(hostUrl, currentLoad, maxLoad));
    }
    return services;
  }

  @Test
  public void testLoadComparatorObeysGeneralContract() {
    for (long seed = 0; seed < 10; seed++) {
      SystemLoad systemLoad = new SystemLoad();
      List<ServiceRegistration> services = manyServicesWithCloseLoadFactors(JOB_TYPE, systemLoad, seed, 50);
      assertObeysGeneralContract(serviceRegistry.new LoadComparator(systemLoad), services);
    }
  }

  @Test
  public void testLoadComparatorEncodingObeysGeneralContractWithoutEncodingWorkers() {
    for (long seed = 0; seed < 10; seed++) {
      SystemLoad systemLoad = new SystemLoad();
      List<ServiceRegistration> services = manyServicesWithCloseLoadFactors(COMPOSER_JOB_TYPE, systemLoad, seed, 50);
      assertObeysGeneralContract(serviceRegistry.new LoadComparatorEncoding(systemLoad), services);
    }
  }

  @Test
  public void testLoadComparatorEncodingObeysGeneralContractWithEncodingWorkers() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(ServiceRegistryJpaImpl.OPT_ENCODING_THRESHOLD, "0.5");
    for (long seed = 0; seed < 10; seed++) {
      SystemLoad systemLoad = new SystemLoad();
      List<ServiceRegistration> services = manyServicesWithCloseLoadFactors(COMPOSER_JOB_TYPE, systemLoad, seed, 50);
      // Declare every other host an encoding worker, so that comparisons between an encoding and a non-encoding
      // worker, as well as between two of the same kind, both happen a lot.
      StringBuilder encodingWorkers = new StringBuilder();
      for (int i = 0; i < services.size(); i += 2) {
        if (encodingWorkers.length() > 0) {
          encodingWorkers.append(',');
        }
        encodingWorkers.append(services.get(i).getHost());
      }
      properties.put(ServiceRegistryJpaImpl.OPT_ENCODING_WORKERS, encodingWorkers.toString());
      serviceRegistry.updated(properties);

      assertObeysGeneralContract(serviceRegistry.new LoadComparatorEncoding(systemLoad), services);
    }
  }

  /**
   * Reproduces the exact kind of inconsistency reported in #2745 by hand: nodes A and B have load factors that are
   * "tied" (within 0.01 of each other), and so are B and C, but A and C are not, and the tie-break by maximum load
   * disagrees with what the direct comparison of A and C would say.
   */
  @Test
  public void testLoadComparatorDoesNotCreateACycleOnATieChain() {
    SystemLoad systemLoad = new SystemLoad();
    // A and B are "tied" (0.008 apart), and B's higher maxLoad makes B sort before A.
    systemLoad.addNodeLoad(new NodeLoad("http://a.example.org", 0.0f, 10f));    // load factor 0.000, maxLoad 10
    systemLoad.addNodeLoad(new NodeLoad("http://b.example.org", 0.4f, 50f));    // load factor 0.008, maxLoad 50
    // B and C are also "tied" (0.008 apart), and C's even higher maxLoad makes C sort before B.
    systemLoad.addNodeLoad(new NodeLoad("http://c.example.org", 1.6f, 100f));   // load factor 0.016, maxLoad 100
    // A and C are not tied (0.016 apart), so they are sorted by their load factor alone: A before C. A consistent
    // comparator must agree with that here too, i.e. it must not also say C < B < A, since that would contradict
    // A < C.

    ServiceRegistration serviceA = service(host("http://a.example.org"), JOB_TYPE);
    ServiceRegistration serviceB = service(host("http://b.example.org"), JOB_TYPE);
    ServiceRegistration serviceC = service(host("http://c.example.org"), JOB_TYPE);

    assertObeysGeneralContract(serviceRegistry.new LoadComparator(systemLoad),
            List.of(serviceA, serviceB, serviceC));
  }

  /**
   * Calls the real job dispatch method for a cluster of many worker nodes with similar loads, the same code path
   * #2745's stack trace goes through. Whether {@code Collections.sort} notices an inconsistent comparator depends
   * on the exact merge pattern for the given input, so this only demonstrates the fix for these particular inputs;
   * {@link #testLoadComparatorObeysGeneralContract} is what actually proves the comparator is consistent for every
   * input.
   */
  @Test
  public void testGetServiceRegistrationsByLoadDoesNotThrowOnALargeCluster() {
    for (long seed = 0; seed < 20; seed++) {
      SystemLoad systemLoad = new SystemLoad();
      List<ServiceRegistration> services = manyServicesWithCloseLoadFactors(JOB_TYPE, systemLoad, seed, 40);
      List<HostRegistration> hosts = new ArrayList<>();
      for (ServiceRegistration service : services) {
        hosts.add(host(service.getHost()));
      }

      List<ServiceRegistration> sorted = serviceRegistry
              .getServiceRegistrationsByLoad(JOB_TYPE, services, hosts, systemLoad);

      Assert.assertEquals(services.size(), sorted.size());
    }
  }
}
