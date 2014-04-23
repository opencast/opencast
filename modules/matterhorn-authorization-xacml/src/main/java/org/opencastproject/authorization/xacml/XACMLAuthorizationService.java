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
package org.opencastproject.authorization.xacml;

import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES;
import static org.opencastproject.util.IoSupport.withFile;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Monadics.caseA;
import static org.opencastproject.util.data.Monadics.caseN;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Option.Match;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.core.model.context.AttributeType;
import org.jboss.security.xacml.core.model.context.RequestType;
import org.jboss.security.xacml.core.model.context.SubjectType;
import org.jboss.security.xacml.core.model.policy.ActionType;
import org.jboss.security.xacml.core.model.policy.ApplyType;
import org.jboss.security.xacml.core.model.policy.AttributeValueType;
import org.jboss.security.xacml.core.model.policy.EffectType;
import org.jboss.security.xacml.core.model.policy.PolicyType;
import org.jboss.security.xacml.core.model.policy.RuleType;
import org.jboss.security.xacml.factories.RequestAttributeFactory;
import org.jboss.security.xacml.factories.RequestResponseContextFactory;
import org.jboss.security.xacml.interfaces.PolicyDecisionPoint;
import org.jboss.security.xacml.interfaces.RequestContext;
import org.jboss.security.xacml.interfaces.XACMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * A XACML implementation of the {@link AuthorizationService}.
 */
public class XACMLAuthorizationService implements AuthorizationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(XACMLAuthorizationService.class);

  /** The default filename for XACML attachments */
  public static final String XACML_FILENAME = "xacml.xml";

  public static final String READ_PERMISSION = "read";

  /** The workspace */
  protected Workspace workspace;

  /** The security service */
  protected SecurityService securityService;

  /** The series service */
  protected SeriesService seriesService;

  @Override
  public Tuple<AccessControlList, AclScope> getActiveAcl(final MediaPackage mp) {
    // tuple up with episode flavor
    final Function<AccessControlList, Tuple<AccessControlList, AclScope>> isEpisodeAcl = tupleA(AclScope.Episode);
    // tuple up with series flavor
    final Function<AccessControlList, Tuple<AccessControlList, AclScope>> isSeriesAcl = tupleA(AclScope.Series);
    return withContextClassLoader(new Function0<Tuple<AccessControlList, AclScope>>() {
      @Override
      public Tuple<AccessControlList, AclScope> apply() {
        // has an episode ACL?
        return getAcl(mp, list(XACML_POLICY_EPISODE)).map(isEpisodeAcl)
        // has a series ACL?
                .orElse(new Function0<Option<Tuple<AccessControlList, AclScope>>>() {
                  @Override
                  public Option<Tuple<AccessControlList, AclScope>> apply() {
                    return getAcl(mp, list(XACML_POLICY_SERIES, XACML_POLICY)).map(isSeriesAcl);
                  }
                })
                // no -> return an empty series ACL
                .getOrElse(getDefaultAcl(mp));
      }
    });
  }

  private Function0<Tuple<AccessControlList, AclScope>> getDefaultAcl(final MediaPackage mp) {
    return new Function0<Tuple<AccessControlList, AclScope>>() {
      @Override
      public Tuple<AccessControlList, AclScope> apply() {
        logger.debug("No XACML attachment found in {}", mp);
        if (StringUtils.isNotBlank(mp.getSeries())) {
          logger.info("Falling back to using default acl from series");
          try {
            return tuple(seriesService.getSeriesAccessControl(mp.getSeries()), AclScope.Series);
          } catch (Exception e) {
            logger.warn("Unable to get default acl from series '{}': {}", mp.getSeries(), e.getMessage());
          }
        }
        logger.trace("Falling back to using default public acl for mediapackage '{}'", mp);
        // TODO: We need a configuration option for open vs. closed by default
        // Right now, rights management is based on series. Here we make sure that
        // objects not belonging to a series are world readable
        AccessControlList accessControlList = new AccessControlList();
        List<AccessControlEntry> acl = accessControlList.getEntries();
        String anonymousRole = securityService.getOrganization().getAnonymousRole();
        acl.add(new AccessControlEntry(anonymousRole, READ_PERMISSION, true));
        return tuple(accessControlList, AclScope.Series);
      }
    };
  }

  private static <A, B> Function<A, Tuple<A, B>> tupleA(final B b) {
    return new Function<A, Tuple<A, B>>() {
      @Override
      public Tuple<A, B> apply(A a) {
        return tuple(a, b);
      }
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public Option<AccessControlList> getAcl(final MediaPackage mp, final AclScope scope) {
    return withContextClassLoader(new Function0<Option<AccessControlList>>() {
      @Override
      public Option<AccessControlList> apply() {
        switch (scope) {
          case Episode:
            return getAcl(mp, list(XACML_POLICY_EPISODE)).orElse(new Function0<Option<AccessControlList>>() {
              @Override
              public Option<AccessControlList> apply() {
                return getAcl(mp, list(XACML_POLICY_SERIES)).fold(
                        new Match<AccessControlList, Option<AccessControlList>>() {
                          @Override
                          public Option<AccessControlList> some(AccessControlList a) {
                            return Option.<AccessControlList> none();
                          }

                          @Override
                          public Option<AccessControlList> none() {
                            return getAcl(mp, list(XACML_POLICY));
                          }
                        });
              }
            });
          case Series:
            return getAcl(mp, list(XACML_POLICY_SERIES, XACML_POLICY));
          default:
            throw new NotImplementedException("AclScope " + scope + " has not been implemented yet!");
        }
      }
    });
  }

  @Override
  public Tuple<MediaPackage, Attachment> setAcl(final MediaPackage mp, final AclScope scope, final AccessControlList acl) {
    return withContextClassLoader(new Function0.X<Tuple<MediaPackage, Attachment>>() {
      @Override
      public Tuple<MediaPackage, Attachment> xapply() throws Exception {
        // Get XACML representation of these role + action tuples
        String xacmlContent = null;
        try {
          xacmlContent = XACMLUtils.getXacml(mp, acl);
        } catch (JAXBException e) {
          throw new MediaPackageException("Unable to generate xacml for mediapackage " + mp.getIdentifier());
        }

        // Remove the old xacml file(s)
        Attachment attachment = removeFromMediaPackageAndWorkspace(mp, toFlavors(scope)).getB();

        // add attachment
        final String elementId = toElementId(scope);
        URI uri;
        try {
          uri = workspace.put(mp.getIdentifier().toString(), elementId, XACML_FILENAME,
                  IOUtils.toInputStream(xacmlContent));
        } catch (IOException e) {
          throw new MediaPackageException("Can not store xacml for mediapackage " + mp.getIdentifier());
        }

        if (attachment == null) {
          attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                  .elementFromURI(uri, Attachment.TYPE, toFlavor(scope));
        }
        attachment.setURI(uri);
        attachment.setIdentifier(elementId);
        // setting the URI to a new source so the checksum will most like be invalid
        attachment.setChecksum(null);
        mp.add(attachment);

        logger.info("Saved XACML under {}", uri);

        // return augmented mediapackage
        return tuple(mp, attachment);
      }
    });
  }

  @Override
  public List<Attachment> getAclAttachments(MediaPackage mp, Option<AclScope> scope) {
    final List<MediaPackageElementFlavor> flavors = scope.map(toFlavorsF).getOrElse(
            Collections.<MediaPackageElementFlavor> nil());
    return getAttachments(mp, flavors);
  }

  @Override
  public MediaPackage removeAcl(MediaPackage mp, AclScope scope) {
    return removeFromMediaPackageAndWorkspace(mp, toFlavors(scope)).getA();
  }

  /** Apply function f within the context of the class loader of XACMLAuthorizationService. */
  private static <A> A withContextClassLoader(Function0<A> f) {
    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(XACMLAuthorizationService.class.getClassLoader());
      return f.apply();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  /** Return an attachment of a given set of flavors only if there is exactly one. */
  private static Option<Attachment> getSingleAttachment(MediaPackage mp, List<MediaPackageElementFlavor> flavors) {
    final List<Attachment> as = getAttachments(mp, flavors);
    if (as.size() == 0) {
      logger.debug("No XACML attachment of type {} found in {}", mkString(flavors, ","), mp);
      return none();
    } else if (as.size() == 1) {
      return some(as.get(0));
    } else { // > 1
      logger.warn("More than one XACML attachment of type {} is attached to {}", mkString(flavors, ","), mp);
      return none();
    }
  }

  private Function0<Option<Attachment>> getSingleAttachmentF(final MediaPackage mp,
          final List<MediaPackageElementFlavor> flavors) {
    return new Function0<Option<Attachment>>() {
      @Override
      public Option<Attachment> apply() {
        return getSingleAttachment(mp, flavors);
      }
    };
  }

  /** Return all attachments of the given flavors. */
  private static List<Attachment> getAttachments(MediaPackage mp, final List<MediaPackageElementFlavor> flavors) {
    return mlist(mp.getAttachments()).filter(new Function<Attachment, Boolean>() {
      @Override
      public Boolean apply(Attachment a) {
        return flavors.contains(a.getFlavor());
      }
    }).value();
  }

  /** Return the XACML attachment or none if the media package does not contain any XACMLs. */
  private Option<Attachment> getXacmlAttachment(MediaPackage mp) {
    return getSingleAttachment(mp, list(XACML_POLICY_EPISODE)).orElse(
            getSingleAttachmentF(mp, list(XACML_POLICY_SERIES, XACML_POLICY)));
  }

  /**
   * Get <em>all</em> flavors associated with a scope. This method has to exist as long as the deprecated
   * {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} flavor exists.
   */
  private static List<MediaPackageElementFlavor> toFlavors(AclScope scope) {
    switch (scope) {
      case Episode:
        return list(XACML_POLICY_EPISODE);
      case Series:
        return list(XACML_POLICY_SERIES, XACML_POLICY);
      default:
        return unexhaustiveMatch();
    }
  }

  /** Functional version of {@link #toFlavors(org.opencastproject.security.api.AclScope)}. */
  private static Function<AclScope, List<MediaPackageElementFlavor>> toFlavorsF = new Function<AclScope, List<MediaPackageElementFlavor>>() {
    @Override
    public List<MediaPackageElementFlavor> apply(AclScope scope) {
      return toFlavors(scope);
    }
  };

  /** Get the flavor associated with a scope. */
  private static MediaPackageElementFlavor toFlavor(AclScope scope) {
    switch (scope) {
      case Episode:
        return XACML_POLICY_EPISODE;
      case Series:
        return XACML_POLICY_SERIES;
      default:
        return unexhaustiveMatch();
    }
  }

  /** Get the element id associated with a scope. */
  private static String toElementId(AclScope scope) {
    switch (scope) {
      case Episode:
        return "security-policy-episode";
      case Series:
        return "security-policy-series";
      default:
        return unexhaustiveMatch();
    }
  }

  private Function0<Option<AccessControlList>> getAclF(final MediaPackage mp,
          final List<MediaPackageElementFlavor> flavors) {
    return new Function0<Option<AccessControlList>>() {
      @Override
      public Option<AccessControlList> apply() {
        return getAcl(mp, flavors);
      }
    };
  }

  /** Get the ACL of the given flavor from a media package. */
  private Option<AccessControlList> getAcl(final MediaPackage mp, final List<MediaPackageElementFlavor> flavors) {
    return mlist(getAttachments(mp, flavors)).match(
            Monadics.<Attachment, Option<AccessControlList>> caseNil(Options.<AccessControlList> never2()),
            caseA(new Function<Attachment, Option<AccessControlList>>() {
              @Override
              public Option<AccessControlList> apply(Attachment a) {
                return loadAcl(a.getURI());
              }
            }), caseN(new Function<List<Attachment>, Option<AccessControlList>>() {
              @Override
              public Option<AccessControlList> apply(List<Attachment> as) {
                // try to find the source policy. Some may be copies sent to distribution channels.
                return mlist(as)
                        .filter(unreferencedAttachments)
                        .match(Monadics
                                .<Attachment, Option<AccessControlList>> caseNil(new Function0<Option<AccessControlList>>() {
                                  @Override
                                  public Option<AccessControlList> apply() {
                                    logger.warn(
                                            "Multiple XACML policies of type {} are attached to {}, and none seem to be authoritative.",
                                            flavors, mp);
                                    return none();
                                  }
                                }), caseA(new Function<Attachment, Option<AccessControlList>>() {
                          @Override
                          public Option<AccessControlList> apply(Attachment a) {
                            return loadAcl(a.getURI());
                          }
                        }), caseN(new Function<List<Attachment>, Option<AccessControlList>>() {
                          @Override
                          public Option<AccessControlList> apply(List<Attachment> as) {
                            logger.warn("More than one non-referenced XACML policy of type {} is attached to {}.",
                                    flavors, mp);
                            return none();
                          }
                        }));
              }
            }));
  }

  private static final Function<Attachment, Boolean> unreferencedAttachments = new Function<Attachment, Boolean>() {
    @Override
    public Boolean apply(Attachment a) {
      return a.getReference() == null;
    }
  };

  /**
   * Get a file from the workspace.
   * 
   * @param uri
   *          The file uri
   * @return return the file if exists otherwise <code>null</code>
   */
  private File fromWorkspace(URI uri) {
    try {
      return workspace.get(uri);
    } catch (NotFoundException e) {
      logger.warn("XACML policy file not found '{}'.", uri);
      return null;
    } catch (IOException e) {
      logger.error("Unable to access XACML policy file. {}", uri, e);
      return null;
    }
  }

  /**
   * Remove all attachments of the given flavors from media package and workspace.
   * 
   * @return the a tuple with the mutated (!) media package as A and the deleted Attachment as B
   */
  private Tuple<MediaPackage, Attachment> removeFromMediaPackageAndWorkspace(MediaPackage mp,
          List<MediaPackageElementFlavor> flavors) {
    Attachment attachment = null;
    for (Attachment a : getAttachments(mp, flavors)) {
      attachment = (Attachment) a.clone();
      try {
        workspace.delete(a.getURI());
      } catch (Exception e) {
        logger.warn("Unable to delete XACML file: {}", e);
      }
      mp.remove(a);
    }
    return Tuple.tuple(mp, attachment);
  }

  private PolicyType unmarshalPolicy(InputStream in) {
    try {
      return ((JAXBElement<PolicyType>) XACMLUtils.jBossXacmlJaxbContext.createUnmarshaller().unmarshal(in)).getValue();
    } catch (JAXBException e) {
      throw new Error("Unable to unmarshall xacml document");
    }
  }

  /** Load an ACL from the given URI. */
  private Option<AccessControlList> loadAcl(final URI uri) {
    File file = fromWorkspace(uri);
    if (file == null)
      return none();

    return withFile(file, new Function2<InputStream, File, AccessControlList>() {
      @Override
      public AccessControlList apply(InputStream in, File aclFile) {
        final PolicyType policy = unmarshalPolicy(in);
        final AccessControlList acl = new AccessControlList();
        final List<AccessControlEntry> entries = acl.getEntries();
        for (Object object : policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
          if (object instanceof RuleType) {
            RuleType rule = (RuleType) object;
            if (rule.getTarget() == null) {
              continue;
            }
            ActionType action = rule.getTarget().getActions().getAction().get(0);
            String actionForAce = (String) action.getActionMatch().get(0).getAttributeValue().getContent().get(0);
            String role = null;
            JAXBElement<ApplyType> apply = (JAXBElement<ApplyType>) rule.getCondition().getExpression();
            for (JAXBElement<?> element : apply.getValue().getExpression()) {
              if (element.getValue() instanceof AttributeValueType) {
                role = (String) ((AttributeValueType) element.getValue()).getContent().get(0);
                break;
              }
            }
            if (role == null) {
              logger.warn("Unable to find a role in rule {}", rule);
              continue;
            }
            AccessControlEntry ace = new AccessControlEntry(role, actionForAce, rule.getEffect().equals(
                    EffectType.PERMIT));
            entries.add(ace);
          } else {
            logger.debug("Skipping {}", object);
          }
        }

        return acl;
      }
    });
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.AuthorizationService#hasPolicy(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public boolean hasPolicy(MediaPackage mp) {
    return getXacmlAttachment(mp).isSome();
  }

  @Override
  public boolean hasPermission(final MediaPackage mp, final String action) {
    return withContextClassLoader(new Function0<Boolean>() {
      @Override
      public Boolean apply() {
        return getXacmlAttachment(mp).map(new Function<Attachment, Boolean>() {
          @Override
          public Boolean apply(Attachment attachment) {
            final File xacmlPolicyFile = fromWorkspace(attachment.getURI());
            if (xacmlPolicyFile == null) {
              logger.warn("Unable to read XACML file from {}! Prevent access permissions.", attachment);
              return false;
            }

            final RequestContext requestCtx = RequestResponseContextFactory.createRequestCtx();
            final User user = securityService.getUser();

            // Create a subject type
            SubjectType subject = new SubjectType();
            subject.getAttribute().add(
                    RequestAttributeFactory.createStringAttributeType(XACMLUtils.SUBJECT_IDENTIFIER, XACMLUtils.ISSUER,
                            user.getUsername()));
            for (Role role : user.getRoles()) {
              AttributeType attSubjectID = RequestAttributeFactory.createStringAttributeType(
                      XACMLUtils.SUBJECT_ROLE_IDENTIFIER, XACMLUtils.ISSUER, role.getName());
              subject.getAttribute().add(attSubjectID);
            }

            // Create a resource type
            URI uri = null;
            try {
              uri = new URI(mp.getIdentifier().toString());
            } catch (URISyntaxException e) {
              logger.warn("Unable to represent mediapackage identifier '{}' as a URI", mp.getIdentifier().toString());
            }
            org.jboss.security.xacml.core.model.context.ResourceType resourceType = new org.jboss.security.xacml.core.model.context.ResourceType();
            resourceType.getAttribute().add(
                    RequestAttributeFactory.createAnyURIAttributeType(XACMLUtils.RESOURCE_IDENTIFIER,
                            XACMLUtils.ISSUER, uri));

            // Create an action type
            org.jboss.security.xacml.core.model.context.ActionType actionType = new org.jboss.security.xacml.core.model.context.ActionType();
            actionType.getAttribute().add(
                    RequestAttributeFactory.createStringAttributeType(XACMLUtils.ACTION_IDENTIFIER, XACMLUtils.ISSUER,
                            action));

            // Create a Request Type
            RequestType requestType = new RequestType();
            requestType.getSubject().add(subject);
            requestType.getResource().add(resourceType);
            requestType.setAction(actionType);
            try {
              requestCtx.setRequest(requestType);
            } catch (IOException e) {
              logger.warn("Unable to set the xacml request type", e);
              return false;
            }

            PolicyDecisionPoint pdp = getPolicyDecisionPoint(xacmlPolicyFile);

            return pdp.evaluate(requestCtx).getDecision() == XACMLConstants.DECISION_PERMIT;
          }
        }).getOrElse(true);
      }
    });
  }

  private PolicyDecisionPoint getPolicyDecisionPoint(File xacmlFile) {
    // Build a JBoss PDP configuration. This is a custom jboss format, so we're just hacking it together here
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    sb.append("<ns:jbosspdp xmlns:ns=\"urn:jboss:xacml:2.0\">");
    sb.append("<ns:Policies><ns:Policy><ns:Location>");
    sb.append(xacmlFile.toURI().toString());
    sb.append("</ns:Location></ns:Policy></ns:Policies><ns:Locators>");
    sb.append("<ns:Locator Name=\"org.jboss.security.xacml.locators.JBossPolicyLocator\">");
    sb.append("</ns:Locator></ns:Locators></ns:jbosspdp>");
    InputStream is = null;
    try {
      is = IOUtils.toInputStream(sb.toString(), "UTF-8");
      return new JBossPDP(is);
    } catch (IOException e) {
      // Only happens if 'UTF-8' is an invalid encoding, which it isn't
      throw new IllegalStateException("Unable to transform a string into a stream");
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Sets the workspace to use for retrieving XACML policies
   * 
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Declarative services callback to set the security service.
   * 
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Declarative services callback to set the series service.
   * 
   * @param seriesService
   *          the series service
   */
  protected void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

}
