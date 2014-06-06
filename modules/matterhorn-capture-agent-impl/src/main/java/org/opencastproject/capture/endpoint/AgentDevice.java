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
package org.opencastproject.capture.endpoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A representation of the devices associated with a capture agent
 */
@XmlType(name = "agent-device", namespace = "http://capture.opencastproject.org")
@XmlRootElement(name = "agent-device", namespace = "http://capture.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentDevice {

  /**
   * The device's friendly name as defined in the property capture.device.names
   */
  @XmlElement(name = "name")
  private String name;
  /**
   * Used to classify the device as video or audio
   */
  @XmlElement(name = "type")
  private String type;

  /**
   * Default constructor for serialization. Do not use it otherwise.
   */
  public AgentDevice() {
  }

  /**
   * Constructor.
   *
   * @param device
   *          friendly device name
   * @param type
   *          device capture type ({@link org.opencastproject.capture.impl.CaptureAgent#CAPTURE_TYPE_AUDIO} or
   *          {@link org.opencastproject.capture.impl.CaptureAgent#CAPTURE_TYPE_VIDEO})
   */
  public AgentDevice(String device, String type) {
    this.name = device;
    this.type = type;
  }

  /**
   * Get device friendly name.
   *
   * @return device friendly name
   */
  public String getDevice() {
    return this.name;
  }

  /**
   * Get device capture type.
   *
   * @return device capture type ({@link org.opencastproject.capture.impl.CaptureAgent#CAPTURE_TYPE_AUDIO} or
   *         {@link org.opencastproject.capture.impl.CaptureAgent#CAPTURE_TYPE_VIDEO})
   */
  public String getType() {
    return this.type;
  }

  /**
   * Get {@link AgentDevice} as a {@link org.w3c.dom.Node} representation.
   *
   * @return XML {@link org.w3c.dom.Node} representation of the {@link AgentDevice}
   */
  public Node toXml(Document document) {
    Element agentDevice = document.createElement("agent-device");

    Element deviceName = document.createElement("name");
    deviceName.appendChild(document.createTextNode(getDevice()));
    agentDevice.appendChild(deviceName);

    Element deviceType = document.createElement("type");
    deviceType.appendChild(document.createTextNode(getType()));
    agentDevice.appendChild(deviceType);
    return agentDevice;
  }
}
