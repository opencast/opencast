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
package org.opencastproject.authorization.xacml.manager.endpoint;

import org.opencastproject.authorization.xacml.manager.api.AclTransition;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import static org.opencastproject.authorization.xacml.manager.impl.Util.splitConfiguredWorkflowRef;
import static org.opencastproject.util.Jsons.Obj;
import static org.opencastproject.util.Jsons.Val;
import static org.opencastproject.util.Jsons.ZERO_VAL;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.Jsons.stringVal;
import static org.opencastproject.util.data.Monadics.mlist;

/** Converter functions from business objects to JSON structures. */
public final class JsonConv {

  public static final String KEY_WORKFLOW_ID = "workflowId";
  public static final String KEY_WORKFLOW_PARAMS = "workflowParams";
  public static final String KEY_ID = "id";
  public static final String KEY_NAME = "name";
  public static final String KEY_ORGANIZATION_ID = "organizationId";
  public static final String KEY_APPLICATION_DATE = "applicationDate";
  public static final String KEY_TRANSITION_ID = "transitionId";
  public static final String KEY_EPISODE_ID = "episodeId";
  public static final String KEY_ACL = "acl";
  public static final String KEY_DONE = "done";
  public static final String KEY_SERIES_ID = "seriesId";
  public static final String KEY_OVERRIDE = "override";
  public static final String KEY_ACE = "ace";
  public static final String KEY_ROLE = "role";
  public static final String KEY_ACTION = "action";
  public static final String KEY_ALLOW = "allow";
  public static final String KEY_EPISODE_TRANSITIONS = "episodeTransitions";
  public static final String KEY_SERIES_TRANSITIONS = "seriesTransitions";

  private JsonConv() {
  }

  public static Function<Obj, Obj> append(final Obj b) {
    return new Function<Obj, Obj>() {
      @Override public Obj apply(Obj a) {
        return a.append(b);
      }
    };
  }

  /**
   * Nest an object under key <code>propName</code>.
   * <p/>
   * Example: key = acl, {id:1, name:"bla"} -&gt; {acl:{id:1, name:"bla"}}
   */
  public static Function<Obj, Obj> nest(final String key) {
    return new Function<Obj, Obj>() {
      @Override public Obj apply(Obj obj) {
        return obj(p(key, obj));
      }
    };
  }

  public static Obj digest(ManagedAcl acl) {
    return obj(p(KEY_ID, acl.getId()),
               p(KEY_NAME, acl.getName()));
  }

  public static final Function<ManagedAcl, Obj> digestManagedAcl = new Function<ManagedAcl, Obj>() {
    @Override public Obj apply(ManagedAcl acl) {
      return digest(acl);
    }
  };

  public static Obj full(ManagedAcl acl) {
    return obj(p(KEY_ID, acl.getId()),
               p(KEY_NAME, acl.getName()),
               p(KEY_ORGANIZATION_ID, acl.getOrganizationId()),
               p(KEY_ACL, full(acl.getAcl())));
  }

  public static final Function<ManagedAcl, Val> fullManagedAcl = new Function<ManagedAcl, Val>() {
    @Override public Val apply(ManagedAcl acl) {
      return full(acl);
    }
  };

  public static Obj full(AccessControlList acl) {
    return obj(p(KEY_ACE, arr(mlist(acl.getEntries()).map(fullAccessControlEntry))));
  }

  public static final Function<AccessControlList, Obj> fullAccessControlList = new Function<AccessControlList, Obj>() {
    @Override public Obj apply(AccessControlList acl) {
      return full(acl);
    }
  };

  public static Obj full(AccessControlEntry ace) {
    return obj(p(KEY_ROLE, ace.getRole()),
               p(KEY_ACTION, ace.getAction()),
               p(KEY_ALLOW, ace.isAllow()));
  }

  public static final Function<AccessControlEntry, Val> fullAccessControlEntry = new Function<AccessControlEntry, Val>() {
    @Override public Val apply(AccessControlEntry ace) {
      return full(ace);
    }
  };

  public static Obj full(EpisodeACLTransition t) {
    return obj(p(KEY_TRANSITION_ID, t.getTransitionId()),
               p(KEY_EPISODE_ID, t.getEpisodeId()),
               p(KEY_ORGANIZATION_ID, t.getOrganizationId()),
               p(KEY_APPLICATION_DATE, DateTimeSupport.toUTC(t.getApplicationDate().getTime())),
               p(KEY_ACL, t.getAccessControlList().map(fullManagedAcl).getOrElse(ZERO_VAL)),
               p(KEY_DONE, t.isDone()))
            .append(workflowObj(t));
  }

  public static Obj full(SeriesACLTransition t) {
    return obj(p(KEY_TRANSITION_ID, t.getTransitionId()),
               p(KEY_SERIES_ID, t.getSeriesId()),
               p(KEY_ORGANIZATION_ID, t.getOrganizationId()),
               p(KEY_APPLICATION_DATE, DateTimeSupport.toUTC(t.getApplicationDate().getTime())),
               p(KEY_ACL, full(t.getAccessControlList())),
               p(KEY_DONE, t.isDone()))
            .append(workflowObj(t));
  }

  public static Obj digest(TransitionResult r) {
    return obj(p(KEY_EPISODE_TRANSITIONS,
                 arr(mlist(r.getEpisodeTransistions()).map(digestEpisodeAclTransition))),
               p(KEY_SERIES_TRANSITIONS,
                 arr(mlist(r.getSeriesTransistions()).map(digestSeriesAclTransition))));
  }

  public static Obj digest(EpisodeACLTransition t) {
    return digestAclTransition(t)
            .append(obj(p(KEY_EPISODE_ID, t.getEpisodeId()),
                        p(KEY_ACL, t.getAccessControlList().map(digestManagedAcl).getOrElse(obj()))));
  }

  public static final Function<EpisodeACLTransition, Val> digestEpisodeAclTransition = new Function<EpisodeACLTransition, Val>() {
    @Override public Val apply(EpisodeACLTransition t) {
      return digest(t);
    }
  };

  public static Obj digest(SeriesACLTransition t) {
    return digestAclTransition(t)
            .append(obj(p(KEY_SERIES_ID, t.getSeriesId()),
                        p(KEY_OVERRIDE, t.isOverride()),
                        p(KEY_ACL, digest(t.getAccessControlList()))));
  }

  public static final Function<SeriesACLTransition, Val> digestSeriesAclTransition = new Function<SeriesACLTransition, Val>() {
    @Override public Val apply(SeriesACLTransition t) {
      return digest(t);
    }
  };

  private static Obj digestAclTransition(AclTransition t) {
    return obj(p(KEY_TRANSITION_ID, t.getTransitionId()),
               p(KEY_ORGANIZATION_ID, t.getOrganizationId()),
               p(KEY_APPLICATION_DATE, DateTimeSupport.toUTC(t.getApplicationDate().getTime())),
               p(KEY_DONE, t.isDone()))
            .append(workflowObj(t));
  }

  private static Obj workflowObj(AclTransition t) {
    final Tuple<Option<String>, Option<String>> ws = splitConfiguredWorkflowRef(t.getWorkflow());
    return obj(p(KEY_WORKFLOW_ID, ws.getA().map(stringVal).getOrElse(ZERO_VAL)),
               p(KEY_WORKFLOW_PARAMS, ws.getB().map(stringVal).getOrElse(ZERO_VAL)));
  }
}
