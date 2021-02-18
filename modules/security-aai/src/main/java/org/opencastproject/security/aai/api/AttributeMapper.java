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
package org.opencastproject.security.aai.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
/**
 * Generic AAI Attribute mapper using Spring Expression language mappings.
 *
 */
public class AttributeMapper implements InitializingBean {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AttributeMapper.class);

  /** List of all attributes that should be fetched */
  private List<String> aaiAttributes;

  /**
   * Map of List of expressions. Key is a mapping name, value a list of
   * expressions
   */
  private Map<String, List<String>> attributeMap;

  /** Use HTTP Header or environment attributes */
  private boolean useHeader = true;

  /** The delimiter for multivalue attributes */
  private String multiValueDelimiter = ";";

  public void afterPropertiesSet() throws Exception {
    Assert.notNull(attributeMap, "attributeMap must be set");
  }

  /**
   * Apply all expressions on the sourceAttributes
   *
   * @param sourceAttributes
   *    Key is attribute name, value a list of split AAI attribute values
   * @param mappingId
   *    The mapping list to use
   * @return
   */
  public List<String> getMappedAttributes(
      Map<String, List<String>> sourceAttributes, String mappingId) {
    Set<String> mappedAttributes = new LinkedHashSet<String>();
    ExpressionParser parser = new SpelExpressionParser();

    List<String> expressions = attributeMap.get(mappingId);

    if (expressions == null) {
      throw new IllegalArgumentException("No mapping for \"" + mappingId
          + "\" specified. Did you forget to configure a <util:map id=\""
          + mappingId + "\" ...?");
    }

    for (String expression : expressions) {
      Expression exp = null;
      try {
        exp = parser.parseExpression(expression);
        String res = (String) exp.getValue(sourceAttributes);
        logger.debug("Mapping {} to {}", exp.getExpressionString(), res);
        if (res != null) {
          mappedAttributes.add(res);
        }
      } catch (Exception e) {
        logger.warn("Mapping for '{}' with expression {} exp.getExpressionString() failed: {}",
            mappingId, exp.getExpressionString(), e.getMessage());
      }
    }

    return CollectionUtils.arrayToList(mappedAttributes.toArray());
  }

  /**
   *
   * @param request
   *    The current HttpServletRequest
   * @param mappingId
   *    The mapping list to use
   * @return
   */
  public List<String> getMappedAttributes(HttpServletRequest request,
      String mappingId) {
    Assert
        .notNull(
            aaiAttributes,
            "aaiAttributes must be set. Did you forget to configure <util:list id=\"aaiAttribute\"?");
    Assert.isTrue(aaiAttributes.size() > 0,
            "At least one aaiAttribute must be set. Did you forget to configure some in "
                    + "<util:list id=\"aaiAttribute\"?");

    Map<String, List<String>> sourceAttributes = new HashMap<String, List<String>>();

    for (String aaiAttribute : aaiAttributes) {
      String value = null;
      if (this.isUseHeader()) {
        value = request.getHeader(aaiAttribute);
        if (value == null) {
          logger.warn("No header '{}' found in request.", aaiAttribute);
          continue;
        }
      } else {
        value = (String) request.getAttribute(aaiAttribute);
        if (value == null) {
          logger.warn("No attribute '{}' found in request.", aaiAttribute);
          continue;
        }
      }
      sourceAttributes.put(aaiAttribute,
          CollectionUtils.arrayToList(value.split(multiValueDelimiter)));
    }
    return this.getMappedAttributes(sourceAttributes, mappingId);
  }

  public Map<String, List<String>> getAttributeMap() {
    return attributeMap;
  }

  public void setAttributeMap(Map<String, List<String>> attributeMap) {
    this.attributeMap = attributeMap;
  }

  public List<String> getAaiAttributes() {
    return aaiAttributes;
  }

  public void setAaiAttributes(List<String> aaiAttributes) {
    this.aaiAttributes = aaiAttributes;
  }

  public boolean isUseHeader() {
    return useHeader;
  }

  public void setUseHeader(boolean useHeader) {
    this.useHeader = useHeader;
  }

  public String getMultiValueDelimiter() {
    return multiValueDelimiter;
  }

  public void setMultiValueDelimiter(String multiValueDelimiter) {
    this.multiValueDelimiter = multiValueDelimiter;
  }

}
