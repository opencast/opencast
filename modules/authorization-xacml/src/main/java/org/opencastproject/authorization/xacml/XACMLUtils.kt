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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.authorization.xacml

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList

import org.jboss.security.xacml.core.model.policy.ActionMatchType
import org.jboss.security.xacml.core.model.policy.ActionType
import org.jboss.security.xacml.core.model.policy.ActionsType
import org.jboss.security.xacml.core.model.policy.ApplyType
import org.jboss.security.xacml.core.model.policy.AttributeDesignatorType
import org.jboss.security.xacml.core.model.policy.AttributeValueType
import org.jboss.security.xacml.core.model.policy.ConditionType
import org.jboss.security.xacml.core.model.policy.EffectType
import org.jboss.security.xacml.core.model.policy.ObjectFactory
import org.jboss.security.xacml.core.model.policy.PolicyType
import org.jboss.security.xacml.core.model.policy.ResourceMatchType
import org.jboss.security.xacml.core.model.policy.ResourceType
import org.jboss.security.xacml.core.model.policy.ResourcesType
import org.jboss.security.xacml.core.model.policy.RuleType
import org.jboss.security.xacml.core.model.policy.SubjectAttributeDesignatorType
import org.jboss.security.xacml.core.model.policy.TargetType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.io.StringWriter

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException

/**
 * Utility implementation for dealing with XACML data.
 */
object XACMLUtils {

    /** XACML rule for combining policies  */
    val RULE_COMBINING_ALG = "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides"
    /** XACML urn for actions  */
    val ACTION_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:action:action-id"
    /** XACML urn for resources  */
    val RESOURCE_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:resource:resource-id"
    /** XACML urn for subject  */
    val SUBJECT_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id"
    /** XACML urn for roles  */
    val SUBJECT_ROLE_IDENTIFIER = "urn:oasis:names:tc:xacml:2.0:subject:role"
    /** XACML urn for string equality  */
    val XACML_STRING_EQUAL = "urn:oasis:names:tc:xacml:1.0:function:string-equal"
    /** XACML urn for string equality  */
    val XACML_STRING_IS_IN = "urn:oasis:names:tc:xacml:1.0:function:string-is-in"
    /** W3C String data type  */
    val W3C_STRING = "http://www.w3.org/2001/XMLSchema#string"
    /** The policy assertion issuer  */
    val ISSUER = "matterhorn"
    /** The JAXB Context to use for marshaling XACML security policy documents  */
    internal var jBossXacmlJaxbContext: JAXBContext
    /** The logging facility  */
    private val logger = LoggerFactory.getLogger(XACMLUtils::class.java)

    /** Static initializer for the single JAXB context  */
    init {
        try {
            XACMLUtils.jBossXacmlJaxbContext = JAXBContext.newInstance("org.jboss.security.xacml.core.model.policy",
                    PolicyType::class.java.classLoader)
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Parses a XACML into an [AccessControlList].
     *
     *
     * Only rules which follow the structure of those created by [.getXacml] may be
     * successfully parsed. All other rules are ignored.
     *
     * @param xacml
     * the XACML to parse
     * @return the ACL, never `null`
     * @throws XACMLParsingException
     * if parsing fails
     */
    @Throws(XACMLParsingException::class)
    fun parseXacml(xacml: InputStream): AccessControlList {

        try {
            val acl = AccessControlList()
            val entries = acl.entries
            val policy = (XACMLUtils.jBossXacmlJaxbContext.createUnmarshaller().unmarshal(xacml) as JAXBElement<PolicyType>).value
            for (`object` in policy.combinerParametersOrRuleCombinerParametersOrVariableDefinition) {

                if (`object` !is RuleType) {
                    throw XACMLParsingException("Object $`object` of policy $policy is not of type RuleType")
                }
                if (`object`.target == null) {
                    if (`object`.ruleId == "DenyRule") {
                        logger.trace("Skipping global deny rule")
                        continue
                    }
                    throw XACMLParsingException("Empty rule $`object` in policy $policy")
                }

                var role: String? = null
                var actionForAce: String? = null
                try {
                    val action = `object`.target.actions.action[0]
                    actionForAce = action.actionMatch[0].attributeValue.content[0] as String

                    val apply = `object`.condition.expression as JAXBElement<ApplyType>
                    for (element in apply.value.expression) {
                        if (element.value is AttributeValueType) {
                            role = (element.value as AttributeValueType).content[0] as String
                            break
                        }
                    }
                } catch (e: Exception) {
                    throw XACMLParsingException("Rule $`object` of policy $policy could not be parsed", e)
                }

                if (role == null) {
                    throw XACMLParsingException("Unable to find role in rule $`object` of policy $policy")
                }
                val ace = AccessControlEntry(role, actionForAce, `object`.effect == EffectType.PERMIT)
                entries!!.add(ace)
            }
            return acl
        } catch (e: Exception) {
            if (e is XACMLParsingException) {
                throw e
            }
            throw XACMLParsingException("XACML could not be parsed", e)
        }

    }

    /**
     * Builds an xml string containing the xacml for the mediapackage.
     *
     * @param mediapackage
     * the mediapackage
     * @param accessControlList
     * the tuples of roles to actions
     * @return
     * @throws JAXBException
     */
    @Throws(JAXBException::class)
    fun getXacml(mediapackage: MediaPackage, accessControlList: AccessControlList): String {
        val jbossXacmlObjectFactory = ObjectFactory()
        val policy = PolicyType()
        policy.policyId = mediapackage.identifier.toString()
        policy.version = "2.0"
        policy.ruleCombiningAlgId = XACMLUtils.RULE_COMBINING_ALG

        // TODO: Add target/resources to rule
        val policyTarget = TargetType()
        val resources = ResourcesType()
        val resource = ResourceType()
        val resourceMatch = ResourceMatchType()
        resourceMatch.matchId = XACMLUtils.XACML_STRING_EQUAL
        val resourceAttributeValue = AttributeValueType()
        resourceAttributeValue.dataType = XACMLUtils.W3C_STRING
        resourceAttributeValue.content.add(mediapackage.identifier.toString())
        val resourceDesignator = AttributeDesignatorType()
        resourceDesignator.attributeId = XACMLUtils.RESOURCE_IDENTIFIER
        resourceDesignator.dataType = XACMLUtils.W3C_STRING

        // now go back up the tree
        resourceMatch.resourceAttributeDesignator = resourceDesignator
        resourceMatch.attributeValue = resourceAttributeValue
        resource.resourceMatch.add(resourceMatch)
        resources.resource.add(resource)
        policyTarget.resources = resources
        policy.target = policyTarget

        // Loop over roleActions and add a rule for each
        for (ace in accessControlList.entries!!) {
            val allow = ace.isAllow

            val rule = RuleType()
            rule.ruleId = ace.role + "_" + ace.action + if (allow) "_Permit" else "_Deny"
            if (allow) {
                rule.effect = EffectType.PERMIT
            } else {
                rule.effect = EffectType.DENY
            }

            val target = TargetType()
            val actions = ActionsType()
            val action = ActionType()
            val actionMatch = ActionMatchType()
            actionMatch.matchId = XACMLUtils.XACML_STRING_EQUAL
            val attributeValue = AttributeValueType()
            attributeValue.dataType = XACMLUtils.W3C_STRING
            attributeValue.content.add(ace.action)
            val designator = AttributeDesignatorType()
            designator.attributeId = XACMLUtils.ACTION_IDENTIFIER
            designator.dataType = XACMLUtils.W3C_STRING

            // now go back up the tree
            actionMatch.actionAttributeDesignator = designator
            actionMatch.attributeValue = attributeValue
            action.actionMatch.add(actionMatch)
            actions.action.add(action)
            target.actions = actions
            rule.target = target

            val condition = ConditionType()
            val apply = ApplyType()
            apply.functionId = XACMLUtils.XACML_STRING_IS_IN

            val conditionAttributeValue = AttributeValueType()
            conditionAttributeValue.dataType = XACMLUtils.W3C_STRING
            conditionAttributeValue.content.add(ace.role)

            val subjectDesignator = SubjectAttributeDesignatorType()
            subjectDesignator.dataType = XACMLUtils.W3C_STRING
            subjectDesignator.attributeId = XACMLUtils.SUBJECT_ROLE_IDENTIFIER
            apply.expression.add(jbossXacmlObjectFactory.createAttributeValue(conditionAttributeValue))
            apply.expression.add(jbossXacmlObjectFactory.createSubjectAttributeDesignator(subjectDesignator))

            condition.expression = jbossXacmlObjectFactory.createApply(apply)
            rule.condition = condition
            policy.combinerParametersOrRuleCombinerParametersOrVariableDefinition.add(rule)
        }

        // Add the global deny rule
        val deny = RuleType()
        deny.effect = EffectType.DENY
        deny.ruleId = "DenyRule"
        policy.combinerParametersOrRuleCombinerParametersOrVariableDefinition.add(deny)

        // serialize to xml
        val writer = StringWriter()
        XACMLUtils.jBossXacmlJaxbContext.createMarshaller().marshal(jbossXacmlObjectFactory.createPolicy(policy), writer)
        return writer.buffer.toString()
    }

}
/**
 * Private constructor to disable clients from instantiating this class.
 */
