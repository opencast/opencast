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

package org.opencastproject.schema;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCores;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/** JAXB transfer object for {@link OcDublinCore}s. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dublincore", namespace = DublinCores.OC_DC_CATALOG_NS_URI)
@XmlRootElement(name = "dublincore", namespace = DublinCores.OC_DC_CATALOG_NS_URI)
public class JaxbOcDublinCore {
  @XmlElement(name = "abstract", namespace = DublinCore.TERMS_NS_URI)
  protected String abstrakt;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String accessRights;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String accrualMethod;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String accrualPeriodicity;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String accrualPolicy;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String alternative;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String audience;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String available;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String bibliographicCitation;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String conformsTo;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String contributor;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String coverage;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Date created;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String creator;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Date date;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Date dateAccepted;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Date dateCopyrighted;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Date dateSubmitted;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String description;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String educationLevel;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected Long extent;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String format;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String hasFormat;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String hasPart;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String hasVersion;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String identifier;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String instructionalMethod;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isFormatOf;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isPartOf;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isReferencedBy;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isReplacedBy;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isRequiredBy;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String issued;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String isVersionOf;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String language;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String license;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String mediator;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String medium;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String modified;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String provenance;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String publisher;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String references;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String relation;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String replaces;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String requires;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String rights;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String rightsHolder;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String source;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String spatial;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String subject;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String tableOfContents;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String temporal;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String title;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String type;
  @XmlElement(namespace = DublinCore.TERMS_NS_URI)
  protected String valid;
}
