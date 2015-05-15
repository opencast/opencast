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
package org.opencastproject.kernel.bundleinfo;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.Jsons.stringVal;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.data.Collections.set;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.util.Jsons;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/** Bundle information via REST. */
@RestService(name = "bundleInfo", title = "Bundle Info", notes = { "The bundle info endpoint yields information about the OSGi bundles running on the whole Matterhorn cluster" }, abstractText = "This service indexes and queries available (distributed) episodes.")
public abstract class BundleInfoRestEndpoint {
  private static final String DEFAULT_BUNDLE_PREFIX = "matterhorn";

  protected abstract BundleInfoDb getDb();

  @GET
  // path prefix "bundles" is contained here and not in the path annotation of the class
  // See https://opencast.jira.com/browse/MH-9768
  @Path("bundles/list")
  @Produces(APPLICATION_JSON)
  @RestQuery(name = "list", description = "Return a list of all running bundles on the whole Matterhorn cluster.", reponses = { @RestResponse(description = "A list of bundles.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The search results, expressed as xml or json.")
  public Response getVersions() {
    final Monadics.ListMonadic<Jsons.Val> bundleInfos = mlist(getDb().getBundles()).map(
            Functions.<BundleInfo, Jsons.Val> co(bundleInfo));
    return ok(obj(p("bundleInfos", arr(bundleInfos)), p("count", bundleInfos.value().size())));
  }

  /** Return true if all bundles have the same bundle version and build number. */
  @GET
  @Path("bundles/check")
  @RestQuery(name = "check", description = "Check if all Matterhorn bundles throughout the cluster have the same OSGi bundle version and the same build number.", restParameters = { @RestParameter(name = "prefix", description = "The bundle name prefixes to check. Defaults to 'matterhorn' to check all matterhorn core bundles.", isRequired = false, defaultValue = "matterhorn", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "true/false", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "cannot find any bundles with the given prefix", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "The search results, expressed as xml or json.")
  public Response checkBundles(@DefaultValue(DEFAULT_BUNDLE_PREFIX) @QueryParam("prefix") List<String> prefixes) {
    return withBundles(prefixes, new Function<List<BundleInfo>, Response>() {
      @Override
      public Response apply(List<BundleInfo> infos) {
        final String bundleVersion = infos.get(0).getBundleVersion();
        final Option<String> buildNumber = infos.get(0).getBuildNumber();
        for (BundleInfo a : infos) {
          if (ne(a.getBundleVersion(), bundleVersion) || ne(a.getBuildNumber(), buildNumber))
            return ok(TEXT_PLAIN_TYPE, "false");
        }
        return ok(TEXT_PLAIN_TYPE, "true");
      }
    });
  }

  /** Return the common version of all bundles matching the given prefix. */
  @GET
  @Path("bundles/version")
  @Produces(APPLICATION_JSON)
  @RestQuery(name = "bundleVersion", description = "Return the common OSGi build version and build number of all bundles matching the given prefix.", restParameters = { @RestParameter(name = "prefix", description = "The bundle name prefixes to check. Defaults to 'matterhorn' to check all matterhorn core bundles.", isRequired = false, defaultValue = "matterhorn", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Version structure", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "cannot find any bundles with the given prefix", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "The search results, expressed as xml or json.")
  public Response getBundleVersion(@DefaultValue(DEFAULT_BUNDLE_PREFIX) @QueryParam("prefix") List<String> prefixes) {
    return withBundles(prefixes, new Function<List<BundleInfo>, Response>() {
      @Override
      public Response apply(List<BundleInfo> infos) {
        final Set<BundleVersion> versions = set();
        for (BundleInfo bundle : infos) {
          versions.add(bundle.getVersion());
        }
        final BundleInfo example = infos.get(0);
        switch (versions.size()) {
          case 0:
            // no versions...
            throw new Error("bug");
          case 1:
            // all versions align
            return ok(obj(p("consistent", true)).append(fullVersionJson.apply(example.getVersion())));
          default:
            // multiple versions found
            return ok(obj(
                    p("consistent", false),
                    p("versions",
                            arr(mlist(versions.iterator())
                                    .map(Functions.<BundleVersion, Jsons.Val> co(fullVersionJson))))));
        }
      }
    });
  }

  public static final Function<BundleVersion, Jsons.Obj> fullVersionJson = new Function<BundleVersion, Jsons.Obj>() {
    @Override
    public Jsons.Obj apply(BundleVersion version) {
      return obj(p("version", version.getBundleVersion()), p("buildNumber", version.getBuildNumber().map(stringVal)));
    }
  };

  public static Jsons.Obj bundleInfoJson(BundleInfo bundle) {
    return obj(p("host", bundle.getHost()), p("bundleSymbolicName", bundle.getBundleSymbolicName()),
            p("bundleId", bundle.getBundleId())).append(fullVersionJson.apply(bundle.getVersion()));
  }

  public static final Function<BundleInfo, Jsons.Obj> bundleInfo = new Function<BundleInfo, Jsons.Obj>() {
    @Override
    public Jsons.Obj apply(BundleInfo bundle) {
      return bundleInfoJson(bundle);
    }
  };

  /** Run <code>f</code> if there is at least one bundle matching the given prefixes. */
  private Response withBundles(List<String> prefixes, Function<List<BundleInfo>, Response> f) {
    final List<BundleInfo> info = getDb().getBundles(toArray(String.class, prefixes));
    if (info.size() > 0) {
      return f.apply(info);
    } else {
      return notFound("No bundles match one of the given prefixes");
    }
  }
}
