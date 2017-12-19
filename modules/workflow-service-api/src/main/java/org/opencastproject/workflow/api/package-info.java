/**
 * Workflow service.
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED, namespace = "http://workflow.opencastproject.org", xmlns = {
        @XmlNs(prefix = "mp", namespaceURI = "http://mediapackage.opencastproject.org"),
        @XmlNs(prefix = "wf", namespaceURI = "http://workflow.opencastproject.org"),
        @XmlNs(prefix = "sec", namespaceURI = "http://org.opencastproject.security") })
package org.opencastproject.workflow.api;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

