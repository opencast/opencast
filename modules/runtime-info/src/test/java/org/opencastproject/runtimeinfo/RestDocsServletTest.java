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
package org.opencastproject.runtimeinfo;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.systems.OpencastConstants;

import org.apache.commons.text.StringEscapeUtils;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Regression test for the reflected XSS in the /docs.html endpoint: the {@code path} query
 * parameter must be HTML-encoded in the "REST docs unavailable for ..." fallback message, so
 * that a crafted value such as a script tag is displayed as text instead of being executed.
 */
public class RestDocsServletTest {

  @Test
  public void pathParameterIsHtmlEscapedInUnavailableMessage() throws Exception {
    String payload = "<script>alert(document.domain)</script>";

    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.anonymous.feedback.url"))
        .andReturn(null);
    EasyMock.expect(bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn(null);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.storage.dir")).andReturn(null);
    // no REST endpoint registered under the requested alias
    EasyMock.expect(bundleContext.getAllServiceReferences(
            EasyMock.<String>isNull(), EasyMock.eq(RestConstants.SERVICES_FILTER)))
        .andReturn(new ServiceReference[0]);

    HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
    // a servlet container URL-decodes query parameters before calling getParameter()
    EasyMock.expect(req.getParameter("path")).andReturn(payload);

    StringWriter body = new StringWriter();
    HttpServletResponse resp = EasyMock.createMock(HttpServletResponse.class);
    EasyMock.expect(resp.getWriter()).andReturn(new PrintWriter(body));
    resp.setContentType("text/html");

    EasyMock.replay(bundleContext, req, resp);

    RestDocsServlet servlet = new RestDocsServlet();
    servlet.start(bundleContext);
    servlet.doGet(req, resp);

    EasyMock.verify(bundleContext, req, resp);

    Assertions.assertEquals("REST docs unavailable for " + StringEscapeUtils.escapeHtml4(payload),
        body.toString());
    Assertions.assertFalse(body.toString().contains(payload),
        "The payload must not be reflected without HTML encoding");
  }
}
