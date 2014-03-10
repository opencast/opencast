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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.ScheduledEvent;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public class SchedulerImplTest {
  private SchedulerImpl schedulerImpl = null;
  private ConfigurationManager configurationManager = null;
  private Properties schedulerProperties = null;
  private CaptureAgentImpl captureAgentImpl = null;
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
  private String directory = "scheduler-restart-test";
  private File testDir = new File("./target", directory);
  private static final Logger logger = LoggerFactory.getLogger(SchedulerImplTest.class);
  private WaitForState waiter;

  @Before
  public void setUp() {
    removeTestDirectory();
    testDir.mkdirs();
    Properties properties = setupCaptureProperties();
    setupConfigurationManager(properties);
    setupCaptureAgentImpl();
    setupSchedulerProperties();
    setupSchedulerImpl();
  }

  private void removeTestDirectory() {
    if (testDir.exists()) {
      FileUtils.deleteQuietly(testDir);
      logger.info("Removing  " + testDir.getAbsolutePath());
    } else {
      logger.info("Didn't Delete " + testDir.getAbsolutePath());
    }
  }

  private Properties setupCaptureProperties() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (inputStream == null) {
      throw new RuntimeException("Unable to load configuration file for capture!");
    }

    Properties properties = new Properties();
    try {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for capture!");
    }
    return properties;
  }

  private void setupConfigurationManager(Properties properties) {
    configurationManager = new ConfigurationManager();
    configurationManager.merge(properties, true);
    // We need this line so we can load the schedules correctly
    // If we don't have it then the locations get turned into /tmp/valid-whatever/demo_capture_agent
    configurationManager.setItem(CaptureParameters.AGENT_NAME, "");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "60");
    configurationManager.setItem("org.opencastproject.storage.dir", testDir.getAbsolutePath());
    configurationManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configurationManager.setItem("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
  }

  private void setupCaptureAgentImpl() {
    captureAgentImpl = new CaptureAgentImpl();
  }

  private void setupSchedulerProperties() {
    InputStream inputStream;
    schedulerProperties = new Properties();

    inputStream = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (inputStream == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }

    try {
      schedulerProperties.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for scheduler!");
    }
  }

  private void setupSchedulerImpl() {
    try {
      schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    } catch (ConfigurationException e) {
      Assert.fail(e.getMessage());
    }
  }

  @After
  public void tearDown() {
    schedulerImpl.deactivate();
    schedulerImpl = null;
    configurationManager = null;
    schedulerProperties = null;
  }

  private String formatDate(Date d, long offset) {
    return sdf.format(new Date(d.getTime() + offset));
  }

  /**
   * Sets up a set of valid calendar times. Assumes edge case calendar is being used, but values will still be usable by
   * 'normal' calendars
   *
   * @throws IOException
   * @throws ConfigurationException
   */
  private String[] setupValidTimes(Date d) {
    String[] times = new String[7];
    // Note: When adding time to this offset, remember to add $desired_time + 1*MINUTES
    // The scheduler is enforcing a 1 minute buffer between events
    long offset = 0;
    // Event with no end and a hardcoded 5-minute duration
    times[0] = formatDate(d, offset);
    offset += 6 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // Event with a longer-than-max capture time
    times[1] = formatDate(d, offset);
    offset += SchedulerImpl.getMaxScheduledCaptureLength() + 2 * CaptureParameters.MINUTES
            * CaptureParameters.MILLISECONDS;
    // Event with a longer-than-max capture time
    times[2] = formatDate(d, offset);
    offset += SchedulerImpl.getMaxScheduledCaptureLength() + 2 * CaptureParameters.MINUTES
            * CaptureParameters.MILLISECONDS;
    // Another 5 minute event
    times[3] = formatDate(d, offset);
    offset += 6 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // A time which can be used as an erroneous end
    times[4] = formatDate(d, offset);
    offset += 6 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // A time which can be used as an erroneous start
    times[5] = formatDate(d, offset);
    // A start 5 minutes in the past
    times[6] = formatDate(d, -1 * (d.getTime() + 5 * CaptureParameters.MINUTES + CaptureParameters.MILLISECONDS));
    return times;
  }

  private String[] setupDurationBoundCheckTimes(Date d) {
    String[] times = new String[8];
    long offset = 0;
    // Start time #1
    times[0] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // End time #1
    times[1] = formatDate(d, offset);
    offset += 2 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // End time #2
    times[2] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // End time #3
    times[3] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS - 1;
    // Start time #2, too close to end time #3 to schedule
    times[4] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // End time #4
    times[5] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;
    // Start time #3, event will be too short
    times[6] = formatDate(d, offset);
    offset += 1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS - 1 /* second */
            * CaptureParameters.MILLISECONDS;
    // End time # 5
    times[7] = formatDate(d, offset);
    return times;
  }

  private String readFile(URL target) {
    StringBuilder sb = new StringBuilder();
    DataInputStream in = null;
    try {
      in = new DataInputStream(target.openStream());
      int c = 0;
      while ((c = in.read()) != -1) {
        sb.append((char) c);
      }
    } catch (IOException e) {
      System.out.println("IOException reading file " + target);
    } finally {
      if (in != null) {
        IOUtils.closeQuietly(in);
      }
    }
    return sb.toString();
  }

  private File setupTestCalendar(String calLocation, String[] times) throws IOException {
    String source = readFile(this.getClass().getClassLoader().getResource(calLocation));

    source = source.replace("@START1@", times[0]);
    source = source.replace("@START2@", times[1]);
    source = source.replace("@START3@", times[2]);
    source = source.replace("@START4@", times[3]);
    source = source.replace("@ERROR_END@", times[4]);
    source = source.replace("@ERROR_START@", times[5]);
    source = source.replace("@PAST_START@", times[6]);

    File output = File.createTempFile("scheduler-test-", ".ics", testDir);
    FileWriter out = null;
    out = new FileWriter(output);
    out.write(source);
    out.close();

    return output;
  }

  private File setupDurationBoundCheckCalendar(String calLocation, String[] times) throws IOException {
    String source = readFile(this.getClass().getClassLoader().getResource(calLocation));

    source = source.replace("@START1@", times[0]);
    source = source.replace("@END1@", times[1]);
    source = source.replace("@END2@", times[2]);
    source = source.replace("@END3@", times[3]);
    source = source.replace("@START2@", times[4]);
    source = source.replace("@END4@", times[5]);
    source = source.replace("@START3@", times[6]);
    source = source.replace("@END5@", times[7]);

    File output = File.createTempFile("scheduler-test-", ".ics", testDir);
    FileWriter out = null;
    out = new FileWriter(output);
    out.write(source);
    out.close();

    return output;
  }

  @Test
  public void testValidRemoteUTF8Calendar() throws IOException, ConfigurationException {
    System.out.println("Starting testValidRemoteUTF8Calendar");
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar(new File("calendars/Opencast.ics").getPath(), times);
    // Yes, I know this isn't actually remote. The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL()
            .toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    schedulerImpl.updateCalendar();

    String[] schedule = null;
    schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    FileUtils.deleteQuietly(testfile);
    System.out.println("Ending testValidRemoteUTF8Calendar");
  }

  @Test
  public void testValidLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("c3a1c747-5501-44ff-b57a-67a4854a39b0", schedule[0]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testBrokenCalendarURLs() throws IOException, ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "foobar:?8785346");
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void test0LengthRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    // Yes, I know this isn't actually remote. The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL()
            .toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    // TODO: Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void test0LengthLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testNegativeLengthRemoteUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    // Yes, I know this isn't actually remote. The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL()
            .toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    // TODO: Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testNegativeLengthLocalUTF8Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    String temp = times[0];
    times[0] = times[1];
    times[1] = temp;
    File testfile = setupTestCalendar("calendars/Opencast.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  @Ignore
  public void testValidRemoteUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    times[1] = times[0];
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    // Yes, I know this isn't actually remote. The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL()
            .toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    // TODO: Figure out why this fails 1/3 times on some machines without the sleep() here.
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  @Ignore
  public void testValidLocalUTF16Calendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar("calendars/Opencast-UTF16.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals(times[0], schedule[0]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testBlankRemoteCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, cachedBlank);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testBlankLocalCalendar() throws ConfigurationException {
    String cachedBlank = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, cachedBlank);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedRemoteURLCalendar() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedLocalURLCalendar() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testMalformedCalendars() throws ConfigurationException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "blah!");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "blah!");
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantRemoteCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile()
            + "nonExistantTest";
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, nonExistant);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testNonExistantLocalCalendar() throws ConfigurationException {
    String nonExistant = this.getClass().getClassLoader().getResource("calendars/Blank.ics").getFile()
            + "nonExistantTest";
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, nonExistant);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageRemoteCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, garbage);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testGarbageLocalCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Garbage.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testIncompleteLocalCalendar() throws ConfigurationException {
    String garbage = this.getClass().getClassLoader().getResource("calendars/Incomplete.ics").getFile();
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, garbage);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(0, schedule.length);
  }

  @Test
  public void testValidRemoteDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    // Yes, I know this isn't actually remote. The point is to test the two different paths for loading calendar data
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, testfile.toURI().toURL()
            .toString());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, null);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    schedulerImpl.updateCalendar();
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("one", schedule[0]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testValidLocalDuplicateCalendar() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar("calendars/Opencast-with-dups.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(1, schedule.length);
    Assert.assertEquals("one", schedule[0]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testEdgecases() throws IOException, ConfigurationException {
    String[] times = setupValidTimes(new Date(System.currentTimeMillis() + 2 * CaptureParameters.HOURS
            * CaptureParameters.MILLISECONDS));
    File testfile = setupTestCalendar("calendars/Edge-Cases.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(5, schedule.length);
    Arrays.sort(schedule);
    Assert.assertEquals("Longer-than-max-capture-time-using-DTEND", schedule[0]);
    Assert.assertEquals("Longer-than-max-capture-time-using-duration", schedule[1]);
    Assert.assertEquals("No-attachments", schedule[2]);
    Assert.assertEquals("No-end-but-duration", schedule[3]);
    Assert.assertEquals("No-end-no-duration", schedule[4]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testOverlappingEdgeCases() throws IOException, ConfigurationException {
    File testfile = setupDurationBoundCheckCalendar("calendars/Overlapping-Cases.ics",
            setupDurationBoundCheckTimes(new Date(System.currentTimeMillis() + 12000L)));
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    String[] schedule = schedulerImpl.getCaptureSchedule();
    Assert.assertEquals(3, schedule.length);
    Arrays.sort(schedule);
    Assert.assertEquals("Scheduled-Event-1", schedule[0]);
    Assert.assertEquals("Started-at-end-of-Scheduled-Event-1-and-should-shorten", schedule[1]);
    Assert.assertEquals("Too-close-to-other-capture", schedule[2]);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testEndpoint() throws MalformedURLException {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "http://www.example.com");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, "");
    URL test = new URL("http://www.example.com");
    schedulerImpl.setScheduleEndpoint(test);
    Assert.assertEquals(test, schedulerImpl.getScheduleEndpoint());
  }

  @Test
  public void testBrokenScheduler() {
    // TODO: Fix this
    boolean expectedException = false;

    // Try it
    try {
      schedulerImpl = new SchedulerImpl(null, configurationManager, captureAgentImpl);
    } catch (ConfigurationException e) {
      expectedException = true;
    }

    // Check it
    if (!expectedException) {
      Assert.fail();
    } else {
      expectedException = false;
    }

    // Try it with a different config
    try {
      schedulerImpl = new SchedulerImpl(new Properties(), configurationManager, captureAgentImpl);
    } catch (ConfigurationException e) {
      expectedException = true;
    }

    // Check it again
    if (!expectedException) {
      Assert.fail();
    } else {
      expectedException = false;
    }
  }

  @Test
  public void testBadCalendarPolling() throws Exception {
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "60");
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Thread.sleep(10);
    Assert.assertTrue(schedulerImpl.isCalendarPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertFalse(schedulerImpl.isCalendarPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, "?asewrtk5fw5");
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "60");
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertFalse(schedulerImpl.isCalendarPollingEnabled());
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, "0");
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertFalse(schedulerImpl.isCalendarPollingEnabled());
  }

  @Test
  public void scheduleRendersCaptureTimesCorrectly() throws IOException, ConfigurationException, InterruptedException {
    Calendar start = Calendar.getInstance();
    int firstMinuteOffset = 20;
    int secondMinuteOffset = -5;
    int thirdMinuteOffset = 30;
    File testfile = setupThreeCaptureCalendar(firstMinuteOffset, secondMinuteOffset, thirdMinuteOffset);
    Thread.sleep(100);
    List<ScheduledEvent> events = schedulerImpl.getSchedule();
    Assert.assertTrue("There should be some events in the schedule.", events.size() > 0);
    for (ScheduledEvent scheduleEvent : events) {
      if (scheduleEvent.getTitle().equalsIgnoreCase("1st-Capture")) {
        checkTime(start, firstMinuteOffset, scheduleEvent);
      } else if (scheduleEvent.getTitle().equalsIgnoreCase("2nd-Capture")) {
        checkTime(start, secondMinuteOffset, scheduleEvent);
      } else if (scheduleEvent.getTitle().equalsIgnoreCase("3rd-Capture")) {
        checkTime(start, thirdMinuteOffset, scheduleEvent);
      }
    }
    FileUtils.deleteQuietly(testfile);
  }

  private void checkTime(Calendar start, int offset, ScheduledEvent scheduleEvent) {
    Date before = new Date();
    Date after = new Date();
    // Create a calendar event a second before the time we expect.
    before = new Date(before.getTime() + (CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS * offset) - 3000);
    // Create a calendar event a second after the time we expect.
    after = new Date(after.getTime() + (CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS * offset) + 3000);
    Date time = new Date(scheduleEvent.getStartTime());
    Assert.assertTrue("getSchedule() returned " + new Date(scheduleEvent.getStartTime()).toString()
            + " as a start time for the event when it should have been " + offset
            + " minutes after right now and 1 second after " + before, before.before(time));
    Assert.assertTrue("getSchedule() returned " + new Date(scheduleEvent.getStartTime()).toString()
            + " as a start time for the event when it should have been " + offset
            + " minutes before right now and 1 second before " + after.toString(), after.after(time));
  }

  private File setupThreeCaptureCalendar(int firstMinuteOffset, int secondMinuteOffset, int thirdMinuteOffset)
          throws IOException, ConfigurationException {
    String[] times = createThreeCaptures(firstMinuteOffset, secondMinuteOffset, thirdMinuteOffset);
    File testfile = setupCaptureAgentTestCalendar("calendars/ThreeCaptures.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    captureAgentImpl.setConfigService(configurationManager);
    schedulerImpl.setConfigService(configurationManager);
    schedulerImpl.updated(schedulerProperties);
    schedulerImpl.updateCalendar();
    return testfile;
  }

  private String[] createThreeCaptures(int firstMinuteOffset, int secondMinuteOffset, int thirdMinuteOffset) {
    Date startTime;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    String[] times = new String[3];
    /* Setup first start time value */
    startTime = new Date();
    startTime = new Date(startTime.getTime() + CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS
            * firstMinuteOffset);
    times[0] = sdf.format(startTime);
    /* Setup second start time value. */
    startTime = new Date();
    startTime = new Date(startTime.getTime() + CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS
            * secondMinuteOffset);
    times[1] = sdf.format(startTime);
    /* Setup third calendar value. */
    startTime = new Date();
    startTime = new Date(startTime.getTime() + CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS
            * thirdMinuteOffset);
    times[2] = sdf.format(startTime);
    return times;
  }

  private File setupCaptureAgentTestCalendar(String calLocation, String[] times) throws IOException {
    String source = readFile(this.getClass().getClassLoader().getResource(calLocation));

    source = source.replace("@START1@", times[0]);
    source = source.replace("@START2@", times[1]);
    source = source.replace("@START3@", times[2]);

    File output = File.createTempFile("capture-scheduler-test-", ".ics", testDir);
    FileWriter out = null;
    out = new FileWriter(output);
    out.write(source);
    out.close();

    return output;
  }

  @Test
  public void testCaptureAgentStatusPollingDoesNotFireIfCaptureIsInFuture() throws IOException, ConfigurationException,
          InterruptedException {
    File testfile = setupThreeCaptureCalendar(10, 20, 30);
    captureAgentImpl.activate(null);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    Thread.sleep(100);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testCaptureAgentStatusPollingDoesNotFireIfCaptureWasInPast() throws IOException, ConfigurationException,
          InterruptedException {
    File testfile = setupThreeCaptureCalendar(-1000, -100, -10);
    captureAgentImpl.activate(null);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    Thread.sleep(100);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testCaptureAgentStatusPollingDoesNotFireIfCapturesAreInPastAndFuture() throws IOException,
          ConfigurationException, InterruptedException {
    File testfile = setupThreeCaptureCalendar(-20, 10, 20);
    captureAgentImpl.activate(null);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    Thread.sleep(100);
    Assert.assertEquals(AgentState.IDLE, captureAgentImpl.getAgentState());
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testLateCaptures() throws IOException, ConfigurationException,
          InterruptedException {
    File testfile = setupThreeCaptureCalendar(-10, -1, 10);
    captureAgentImpl.activate(null);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertEquals(2, schedulerImpl.getCaptureSchedule().length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testRestartingCaptureDoesStartWithNoMediaFiles() throws IOException, ConfigurationException,
          InterruptedException {
    // Unused test that should test the ability of the capture agent to skip starting a capture late if there has
    // already been captured media.
    setupFakeMediaPackageWithoutMediaFiles();
    File testfile = setupThreeCaptureCalendar(-10, -1, 10);
    captureAgentImpl.activate(null);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertEquals(2, schedulerImpl.getCaptureSchedule().length);
    FileUtils.deleteQuietly(testfile);
  }

  @Test
  public void testRestartingCaptureDoesntOverwriteExistingCapture() throws IOException, ConfigurationException,
          InterruptedException {
    // Unused test that should test the ability of the capture agent to skip starting a capture late if there has
    // already been captured media.
    setupFakeMediaPackageWithMediaFiles();
    File testfile = setupThreeCaptureCalendar(-10, -1, 10);
    captureAgentImpl.activate(null);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    Assert.assertEquals(1, schedulerImpl.getCaptureSchedule().length);
    FileUtils.deleteQuietly(testfile);
  }

  private XProperties loadProperties(String location) throws IOException {
    XProperties props = new XProperties();
    InputStream s = getClass().getClassLoader().getResourceAsStream(location);
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file from " + location);
    }
    props.load(s);
    return props;
  }

  public void setupFakeMediaPackageWithoutMediaFiles() {
    // Create the configuration manager
    configurationManager = new ConfigurationManager();
    // Setup the configuration manager with a tmp storage directory.
    File recordingDir = new File("./target", directory);
    Properties p;
    try {
      p = loadProperties("config/capture.properties");
      p.put(CaptureParameters.CAPTURE_FILESYSTEM_CACHE_URL, new File(testDir, "cache").getAbsolutePath());
      p.put(CaptureParameters.CAPTURE_FILESYSTEM_VOLATILE_URL, new File(testDir, "volatile").getAbsolutePath());
      p.put(CaptureParameters.RECORDING_ROOT_URL, recordingDir.getAbsolutePath());
      p.put(CaptureParameters.RECORDING_ID, "2nd-Capture");
      p.put("org.opencastproject.server.url", "http://localhost:8080");
      p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
      p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
      p.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "capture.device.names=MOCK_SCREEN,MOCK_PRESENTER,MOCK_MICROPHONE");
      configurationManager.updated(p);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    } catch (ConfigurationException e) {
      e.printStackTrace();
      Assert.fail();
    }

    File uidFile = new File(recordingDir, "2nd-Capture");
    try {
      FileUtils.forceMkdir(uidFile);
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "episode.xml"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "manifest.xml"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "org.opencastproject.capture.agent.properties"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "capture.stopped"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "series.xml"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setupFakeMediaPackageWithMediaFiles() {
    String directory = "scheduler-restart-test";
    setupFakeMediaPackageWithoutMediaFiles();
    File uidFile = new File("./target/" + directory, "2nd-Capture");
    try {
      FileUtils.forceMkdir(uidFile);
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "screen_out.mpg"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "camera_out.mpg"));
      FileUtils.touch(new File(uidFile.getAbsolutePath(), "audio_out.mp3"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void callingRefreshBeforeUpdateDoesntCauseNullPointerException() throws IOException, ConfigurationException,
          InterruptedException {
    String[] times = createThreeCaptures(-20, 5, 20);
    File testfile = setupCaptureAgentTestCalendar("calendars/ThreeCaptures.ics", times);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL, null);
    configurationManager.setItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    Dictionary<String, String> dictionary = new Hashtable<String, String>();
    dictionary.put(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL, testfile.getAbsolutePath());
    configurationManager.updated(dictionary);
    schedulerImpl.setConfigService(configurationManager);
    schedulerImpl = new SchedulerImpl(schedulerProperties, configurationManager, captureAgentImpl);
    captureAgentImpl.setConfigService(configurationManager);
    schedulerImpl.setConfigService(configurationManager);
    FileUtils.deleteQuietly(testfile);
  }
}
