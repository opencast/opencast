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

package org.opencastproject.authorization.xacml;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.XmlSafeParser;

import org.jboss.security.xacml.core.model.policy.ActionMatchType;
import org.jboss.security.xacml.core.model.policy.ActionType;
import org.jboss.security.xacml.core.model.policy.ActionsType;
import org.jboss.security.xacml.core.model.policy.ApplyType;
import org.jboss.security.xacml.core.model.policy.AttributeDesignatorType;
import org.jboss.security.xacml.core.model.policy.AttributeValueType;
import org.jboss.security.xacml.core.model.policy.ConditionType;
import org.jboss.security.xacml.core.model.policy.EffectType;
import org.jboss.security.xacml.core.model.policy.ObjectFactory;
import org.jboss.security.xacml.core.model.policy.PolicyType;
import org.jboss.security.xacml.core.model.policy.ResourceMatchType;
import org.jboss.security.xacml.core.model.policy.ResourceType;
import org.jboss.security.xacml.core.model.policy.ResourcesType;
import org.jboss.security.xacml.core.model.policy.RuleType;
import org.jboss.security.xacml.core.model.policy.SubjectAttributeDesignatorType;
import org.jboss.security.xacml.core.model.policy.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Utility implementation for dealing with XACML data.
 */
public final class XACMLUtils {

  /** XACML rule for combining policies */
  public static final String RULE_COMBINING_ALG = "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides";
  /** XACML urn for actions */
  public static final String ACTION_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:action:action-id";
  /** XACML urn for resources */
  public static final String RESOURCE_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
  /** XACML urn for subject */
  public static final String SUBJECT_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
  /** XACML urn for roles */
  public static final String SUBJECT_ROLE_IDENTIFIER = "urn:oasis:names:tc:xacml:2.0:subject:role";
  /** XACML urn for string equality */
  public static final String XACML_STRING_EQUAL = "urn:oasis:names:tc:xacml:1.0:function:string-equal";
  /** XACML urn for string equality */
  public static final String XACML_STRING_IS_IN = "urn:oasis:names:tc:xacml:1.0:function:string-is-in";
  /** W3C String data type */
  public static final String W3C_STRING = "http://www.w3.org/2001/XMLSchema#string";
  /** The policy assertion issuer */
  public static final String ISSUER = "matterhorn";
  /** The JAXB Context to use for marshaling XACML security policy documents */
  protected static JAXBContext jBossXacmlJaxbContext;
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(XACMLUtils.class);

  /** Static initializer for the single JAXB context */
  static {
    try {
      XACMLUtils.jBossXacmlJaxbContext = JAXBContext.newInstance("org.jboss.security.xacml.core.model.policy",
              PolicyType.class.getClassLoader());
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Private constructor to disable clients from instantiating this class.
   */
  private XACMLUtils() {
  }

  /**
   * Parses a XACML into an {@link AccessControlList}.
   * <p>
   * Only rules which follow the structure of those created by {@link #getXacml(MediaPackage, AccessControlList)} may be
   * successfully parsed. All other rules are ignored.
   * 
   * @param xacml
   *          the XACML to parse
   * @return the ACL, never {@code null}
   * @throws XACMLParsingException
   *           if parsing fails
   */
  public static AccessControlList parseXacml(InputStream xacml) throws XACMLParsingException {

    try {
      @SuppressWarnings("unchecked")
      final AccessControlList acl = new AccessControlList();
      final List<AccessControlEntry> entries = acl.getEntries();
      final PolicyType policy = ((JAXBElement<PolicyType>) XACMLUtils.jBossXacmlJaxbContext.createUnmarshaller().unmarshal(XmlSafeParser.parse(xacml))).getValue();
      for (Object object : policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {

        if (!(object instanceof RuleType)) {
          throw new XACMLParsingException("Object " + object + " of policy " + policy + " is not of type RuleType");
        }
        RuleType rule = (RuleType) object;
        if (rule.getTarget() == null) {
          if (rule.getRuleId().equals("DenyRule")) {
            logger.trace("Skipping global deny rule");
            continue;
          }
          throw new XACMLParsingException("Empty rule " + rule + " in policy " + policy);
        }

        String role = null;
        String actionForAce = null;
        try {
          ActionType action = rule.getTarget().getActions().getAction().get(0);
          actionForAce = (String) action.getActionMatch().get(0).getAttributeValue().getContent().get(0);

          @SuppressWarnings("unchecked") JAXBElement<ApplyType> apply = (JAXBElement<ApplyType>) rule.getCondition().getExpression();
          for (JAXBElement<?> element : apply.getValue().getExpression()) {
            if (element.getValue() instanceof AttributeValueType) {
              role = (String) ((AttributeValueType) element.getValue()).getContent().get(0);
              break;
            }
          }
        } catch (Exception e) {
          throw new XACMLParsingException("Rule " + rule + " of policy " + policy + " could not be parsed", e);
        }
        if (role == null) {
          throw new XACMLParsingException("Unable to find role in rule " + rule + " of policy " + policy);
        }
        AccessControlEntry ace = new AccessControlEntry(role, actionForAce, rule.getEffect().equals(EffectType.PERMIT));
        entries.add(ace);
      }
      return acl;
    } catch (Exception e) {
      if (e instanceof XACMLParsingException) {
        throw (XACMLParsingException) e;
      }
      throw new XACMLParsingException("XACML could not be parsed", e);
    }
  }

  /**
   * Builds an xml string containing the xacml for the mediapackage.
   *
   * @param mediapackage
   *          the mediapackage
   * @param accessControlList
   *          the tuples of roles to actions
   * @return
   * @throws JAXBException
   */
  public static String getXacml(MediaPackage mediapackage, AccessControlList accessControlList) throws JAXBException {
    ObjectFactory jbossXacmlObjectFactory = new ObjectFactory();
    PolicyType policy = new PolicyType();
    policy.setPolicyId(mediapackage.getIdentifier().toString());
    policy.setVersion("2.0");
    policy.setRuleCombiningAlgId(XACMLUtils.RULE_COMBINING_ALG);

    // TODO: Add target/resources to rule
    TargetType policyTarget = new TargetType();
    ResourcesType resources = new ResourcesType();
    ResourceType resource = new ResourceType();
    ResourceMatchType resourceMatch = new ResourceMatchType();
    resourceMatch.setMatchId(XACMLUtils.XACML_STRING_EQUAL);
    AttributeValueType resourceAttributeValue = new AttributeValueType();
    resourceAttributeValue.setDataType(XACMLUtils.W3C_STRING);
    resourceAttributeValue.getContent().add(mediapackage.getIdentifier().toString());
    AttributeDesignatorType resourceDesignator = new AttributeDesignatorType();
    resourceDesignator.setAttributeId(XACMLUtils.RESOURCE_IDENTIFIER);
    resourceDesignator.setDataType(XACMLUtils.W3C_STRING);

    // now go back up the tree
    resourceMatch.setResourceAttributeDesignator(resourceDesignator);
    resourceMatch.setAttributeValue(resourceAttributeValue);
    resource.getResourceMatch().add(resourceMatch);
    resources.getResource().add(resource);
    policyTarget.setResources(resources);
    policy.setTarget(policyTarget);

    // Loop over roleActions and add a rule for each
    for (AccessControlEntry ace : accessControlList.getEntries()) {
      boolean allow = ace.isAllow();

      RuleType rule = new RuleType();
      rule.setRuleId(ace.getRole() + "_" + ace.getAction() + (allow ? "_Permit" : "_Deny"));
      if (allow) {
        rule.setEffect(EffectType.PERMIT);
      } else {
        rule.setEffect(EffectType.DENY);
      }

      TargetType target = new TargetType();
      ActionsType actions = new ActionsType();
      ActionType action = new ActionType();
      ActionMatchType actionMatch = new ActionMatchType();
      actionMatch.setMatchId(XACMLUtils.XACML_STRING_EQUAL);
      AttributeValueType attributeValue = new AttributeValueType();
      attributeValue.setDataType(XACMLUtils.W3C_STRING);
      attributeValue.getContent().add(ace.getAction());
      AttributeDesignatorType designator = new AttributeDesignatorType();
      designator.setAttributeId(XACMLUtils.ACTION_IDENTIFIER);
      designator.setDataType(XACMLUtils.W3C_STRING);

      // now go back up the tree
      actionMatch.setActionAttributeDesignator(designator);
      actionMatch.setAttributeValue(attributeValue);
      action.getActionMatch().add(actionMatch);
      actions.getAction().add(action);
      target.setActions(actions);
      rule.setTarget(target);

      ConditionType condition = new ConditionType();
      ApplyType apply = new ApplyType();
      apply.setFunctionId(XACMLUtils.XACML_STRING_IS_IN);

      AttributeValueType conditionAttributeValue = new AttributeValueType();
      conditionAttributeValue.setDataType(XACMLUtils.W3C_STRING);
      conditionAttributeValue.getContent().add(ace.getRole());

      SubjectAttributeDesignatorType subjectDesignator = new SubjectAttributeDesignatorType();
      subjectDesignator.setDataType(XACMLUtils.W3C_STRING);
      subjectDesignator.setAttributeId(XACMLUtils.SUBJECT_ROLE_IDENTIFIER);
      apply.getExpression().add(jbossXacmlObjectFactory.createAttributeValue(conditionAttributeValue));
      apply.getExpression().add(jbossXacmlObjectFactory.createSubjectAttributeDesignator(subjectDesignator));

      condition.setExpression(jbossXacmlObjectFactory.createApply(apply));
      rule.setCondition(condition);
      policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
    }

    // Add the global deny rule
    RuleType deny = new RuleType();
    deny.setEffect(EffectType.DENY);
    deny.setRuleId("DenyRule");
    policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(deny);

    // serialize to xml
    StringWriter writer = new StringWriter();
    XACMLUtils.jBossXacmlJaxbContext.createMarshaller().marshal(jbossXacmlObjectFactory.createPolicy(policy), writer);
    return writer.getBuffer().toString();
  }

}
