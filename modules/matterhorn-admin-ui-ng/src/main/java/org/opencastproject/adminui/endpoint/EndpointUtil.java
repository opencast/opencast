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
package org.opencastproject.adminui.endpoint;

import static java.lang.String.format;

import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.data.json.JObjectWrite;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

public final class EndpointUtil {
  private static final SimpleSerializer serializer = new SimpleSerializer();

  private EndpointUtil() {
  }

  /**
   * Create a streaming response entity.
   * Pass it as an entity parameter to one of the response builder methods like {@link org.opencastproject.util.RestUtil.R#ok(Object)}.
   */
  public static StreamingOutput stream(final Fx<Writer> out) {
    return new StreamingOutput() {
      @Override public void write(OutputStream s) throws IOException, WebApplicationException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(s));
        out.ap(writer);
        writer.flush();
      }
    };
  }

  public static Response ok(JObjectWrite json) {
    return Response.ok(stream(serializer.toJsonFx(json)), MediaType.APPLICATION_JSON_TYPE).build();
  }

  public static Response notFound(String msg, Object... args) {
    return Response.status(Status.NOT_FOUND).entity(format(msg, args)).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  public static String dateDay(Date date) {
    return EncodingSchemeUtils.formatDate(date, Precision.Day);
  }

  public static final Fn<Date, String> fnDay = new Fn<Date, String>() {
    @Override public String ap(Date date) {
      return dateDay(date);
    }
  };

  public static String dateSecond(Date date) {
    return EncodingSchemeUtils.formatDate(date, Precision.Second);
  }

  public static final Fn<Date, String> fnSecond = new Fn<Date, String>() {
    @Override public String ap(Date date) {
      return dateSecond(date);
    }
  };
}
