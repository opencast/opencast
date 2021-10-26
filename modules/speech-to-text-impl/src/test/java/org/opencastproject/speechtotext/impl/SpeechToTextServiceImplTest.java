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

package org.opencastproject.speechtotext.impl;

import static org.easymock.EasyMock.capture;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.speechtotext.impl.engine.VoskEngine;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class SpeechToTextServiceImplTest {

  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceImplTest.class);

  private static boolean runVoskTest = false;

  private SpeechToTextServiceImpl speechToTextService;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupClass() {
    Process process = null;
    try {
      process = new ProcessBuilder(VoskEngine.VOSK_EXECUTABLE_DEFAULT_PATH).start();
      runVoskTest = true;
    } catch (Throwable t) {
      logger.warn("Skipping tests due to unsatisfied vosk-cli installation");
    } finally {
      IoSupport.closeQuietly(process);
    }
  }

  @Before
  public void setUp() throws Exception {

    // Skip tests if vosk-cli is not installed
    Assume.assumeTrue(runVoskTest);

    speechToTextService = new SpeechToTextServiceImpl();
    VoskEngine voskEngine = new VoskEngine();

    // create the needed mocks
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    Workspace workspace = EasyMock.createMock(Workspace.class);
    final String directory = testFolder.newFolder().getAbsolutePath();
    EasyMock.expect(workspace.rootDirectory()).andReturn(directory).anyTimes();

    EasyMock.expect(workspace.get(EasyMock.anyObject()))
            .andReturn(new File(getClass().getResource("/speech_to_text_test.mp4").toURI())).anyTimes();

    Field field = speechToTextService.getClass().getDeclaredField("selectedEngine");
    field.setAccessible(true);
    field.set(speechToTextService, voskEngine);

    final Capture<String> collection = EasyMock.newCapture();
    final Capture<String> name = EasyMock.newCapture();
    final Capture<InputStream> in = EasyMock.newCapture();
    EasyMock.expect(workspace.putInCollection(capture(collection), capture(name), capture(in))).andAnswer(() -> {
      File output = new File(directory, "speech_to_text_test.vtt");
      FileUtils.copyInputStreamToFile(in.getValue(), output);
      return output.toURI();
    }).once();

    final Capture<String> collectionID = EasyMock.newCapture();
    final Capture<String> fileName = EasyMock.newCapture();
    EasyMock.expect(workspace.getCollectionURI(capture(collectionID), capture(fileName))).andAnswer(() -> {
      File tmpVttFile = new File(directory + "/collection/" + collectionID.getValue() + "/" + fileName.getValue());
      return tmpVttFile.toURI();
    }).anyTimes();

    workspace.delete(EasyMock.anyObject());

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, workspace);

    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
            .andAnswer(() -> {
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(speechToTextService.process(job));
              return job;
            }).anyTimes();
    EasyMock.replay(serviceRegistry);

    speechToTextService.setServiceRegistry(serviceRegistry);
    speechToTextService.setWorkspace(workspace);
  }

  @Test
  public void testVoskSpeechToText() throws Exception {
    URI videoUri = getClass().getResource("/speech_to_text_test.mp4").toURI();
    Job job = speechToTextService.transcribe(videoUri, "eng");
    File output = new File(new URI(job.getPayload()));
    StringBuilder textFromSpeech = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(output.getPath()), StandardCharsets.UTF_8)) {
      stream.forEach(s -> textFromSpeech.append(s).append("\n"));
    } catch (IOException e) {
      Assert.fail("IOException occurred " + e.getMessage());
    }
    Assert.assertTrue(output.isFile());
    Assert.assertTrue(textFromSpeech.indexOf("look celia we have to follow our passions") > 0);
  }

}
