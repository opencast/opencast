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
package org.opencastproject.util;

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Prelude.sleep;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClient.RequestRunner;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

/** Functions to support Apache httpcomponents and HTTP related operations in general. */

/** Functions to support Apache httpcomponents. */
public final class HttpUtil {
  private HttpUtil() {
  }

  public static HttpPost post(NameValuePair... formParams) {
    final HttpPost post = new HttpPost();
    setFormParams(post, formParams);
    return post;
  }

  public static HttpPost post(String uri, NameValuePair... formParams) {
    final HttpPost post = new HttpPost(uri);
    setFormParams(post, formParams);
    return post;
  }

  public static HttpPost post(String uri, List<NameValuePair> formParams) {
    final HttpPost post = new HttpPost(uri);
    setFormParams(post, Collections.toArray(NameValuePair.class, formParams));
    return post;
  }

  public static HttpGet get(String path, Tuple<String, String>... queryParams) {
    final String url = mlist(path, mlist(queryParams).map(new Function<Tuple<String, String>, String>() {
      @Override
      public String apply(Tuple<String, String> a) {
        try {
          return a.getA() + "=" + URLEncoder.encode(a.getB(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
          return chuck(e);
        }
      }
    }).mkString("&")).mkString("?");
    return new HttpGet(url);
  }

  public static String path(String... path) {
    return UrlSupport.concat(path);
  }

  private static void setFormParams(HttpEntityEnclosingRequest r, NameValuePair[] formParams) {
    final List<NameValuePair> params = list(formParams);
    try {
      r.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      chuck(e);
    }
  }

  public static NameValuePair param(String name, String value) {
    return new BasicNameValuePair(name, value);
  }

  public static NameValuePair param(Tuple<String, String> p) {
    return new BasicNameValuePair(p.getA(), p.getB());
  }

  public static final Function<Tuple<String, String>, NameValuePair> param_ = new Function<Tuple<String, String>, NameValuePair>() {
    @Override
    public NameValuePair apply(Tuple<String, String> p) {
      return param(p);
    }
  };

  public static final Function<HttpResponse, Integer> getStatusCode = new Function<HttpResponse, Integer>() {
    @Override
    public Integer apply(HttpResponse response) {
      return response.getStatusLine().getStatusCode();
    }
  };

  /**
   * Return the content of the response as a string if its status code equals one of the given statuses. Throw an
   * exception on an unexpected status.
   * <p/>
   * Function composition of {@link #getContentFn} and {@link #expect(int...)}.
   */
  public static Function<HttpResponse, String> getContentOn(final int... status) {
    return getContentFn.o(expect(status));
  }

  public static String getContentOn(final RequestRunner<String> runner, final int... status) {
    final Either<Exception, String> res = runner.run(getContentOn(status));
    if (res.isRight()) {
      return res.right().value();
    } else {
      return chuck(res.left().value());
    }
  }

  /** Return the content of the response as a string. */
  public static final Function<HttpResponse, String> getContentFn = new Function.X<HttpResponse, String>() {
    @Override
    public String xapply(HttpResponse httpResponse) throws Exception {
      final Header h = httpResponse.getEntity().getContentEncoding();
      if (h != null) {
        return IOUtils.toString(httpResponse.getEntity().getContent(), h.getValue());
      } else {
        return IOUtils.toString(httpResponse.getEntity().getContent());
      }
    }
  };

  /** Return the response if its status code equals one of the given statuses or throw an exception. */
  public static Function<HttpResponse, HttpResponse> expect(final int... status) {
    return new Function.X<HttpResponse, HttpResponse>() {
      @Override
      public HttpResponse xapply(HttpResponse response) {
        final int sc = response.getStatusLine().getStatusCode();
        for (int s : status) {
          if (sc == s)
            return response;
        }
        String responseBody;
        try {
          responseBody = IOUtils.toString(response.getEntity().getContent());
        } catch (IOException e) {
          responseBody = "";
        }
        throw new RuntimeException("Returned status " + sc + " does not match any of the expected codes. "
                + responseBody);
      }
    };
  }

  /** Get the value or throw the exception. */
  public static <A> A getOrError(Either<Exception, A> response) {
    if (response.isRight()) {
      return response.right().value();
    } else {
      return chuck(response.left().value());
    }
  }

  public static boolean isOk(HttpResponse res) {
    return res.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
  }

  /**
   * Wait for a certain status of a resource.
   *
   * @return either an exception or the status code of the last http response
   */
  public static Either<Exception, Integer> waitForResource(final TrustedHttpClient http, final URI resourceUri,
          final int expectedStatus, final long timeout, final long pollingInterval) {
    long now = 0L;
    while (true) {
      final HttpHead head = new HttpHead(resourceUri);
      final Either<Exception, Integer> result = http.<Integer> run(head).apply(getStatusCode);
      for (final Integer status : result.right()) {
        if (eq(status, expectedStatus) || now >= timeout) {
          return right(status);
        } else if (now < timeout) {
          if (!sleep(pollingInterval)) {
            return left(new Exception("Interrupted"));
          } else {
            now = now + pollingInterval;
          }
        }
      }
      for (Exception e : result.left()) {
        return left(e);
      }
    }
  }
}
