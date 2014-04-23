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

package org.opencastproject.manager.core;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * The <code>FilteringResponseWrapper</code> wraps the response to provide a
 * filtering writer for HTML responses. The filtering writer filters the
 * response such that any string of the form <code>${some text}</code> is
 * replaced by a translation of the <i>some text</i> according to the
 * <code>ResourceBundle</code> provided in the constructor. If no translation
 * exists in the resource bundle, the text is written unmodifed (except the
 * wrapping <code>${}</code> characters are removed.
 *
 * @author Leonid Oldenburger
 */
public class TemplateWrapperFilter extends HttpServletResponseWrapper {

  /**
   * the servlet's request providing the variable resolver at the time
   * the getWriter() method is called
   */
    private final ServletRequest request;

    /**
     * the writer sending output in this response
     */
    private PrintWriter writer;

    /**
     * Creates a wrapper instance using the given resource bundle for
     * translations.
     *
     * @param response the response to wrap
     * @param locale a resource bundle, that will be used for translation of the strings
     * @param request the original request - used to obtain the variable resolver
     */
    public TemplateWrapperFilter(final HttpServletResponse response, final ServletRequest request) {

      super(response);
        this.request = request;
    }

    /**
     * Returns a <code>PrintWriter</code> for the response. If <code>text/html</code>
     * is being generated a filtering writer is returned which translates
     * strings enclosed in <code>${}</code> according to the resource bundle
     * configured for this response.
     */
    public PrintWriter getWriter() throws IOException {

      if (writer == null) {
            final PrintWriter base = super.getWriter();

            if (doWrap()) {
                final TemplateResourceFilter filter = new TemplateResourceFilter(base, (HashMap) request.getAttribute("template_var"));

                writer = new PrintWriter(filter);
            } else {
                writer = base;
            }
        }
        return writer;
    }

    /**
     * Method proofs is there is a correct file to wrap.
     *
     * @return wrap state
     */
    private boolean doWrap() {
        boolean doWrap = getContentType() != null && getContentType().indexOf("text/html") >= 0;

        return doWrap;
    }
}