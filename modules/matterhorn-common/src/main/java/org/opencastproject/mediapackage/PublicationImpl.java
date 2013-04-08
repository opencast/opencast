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
package org.opencastproject.mediapackage;

import org.opencastproject.util.MimeType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.net.URI;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "publication", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "publication", namespace = "http://mediapackage.opencastproject.org")
public class PublicationImpl extends AbstractMediaPackageElement implements Publication {
  /** Serial version UID */
  private static final long serialVersionUID = 11151970L;

  @XmlAttribute(name = "channel", required = true)
  private String channel;

  /** JAXB constructor */
  public PublicationImpl() {
    this.elementType = Type.Publication;
  }

  public PublicationImpl(String id,
                         String channel,
                         URI uri,
                         MimeType mimeType) {
    this();
    setURI(uri);
    setIdentifier(id);
    setMimeType(mimeType);
    this.channel = channel;
  }

  public static Publication publication(String id,
                                        String channel,
                                        URI uri,
                                        MimeType mimeType) {
    return new PublicationImpl(id, channel, uri, mimeType);
  }

  @Override
  public String getChannel() {
    return channel;
  }

  /** JAXB adapater */
  public static class Adapter extends XmlAdapter<PublicationImpl, Publication> {
    @Override public PublicationImpl marshal(Publication e) throws Exception {
      return (PublicationImpl) e;
    }

    @Override public Publication unmarshal(PublicationImpl e) throws Exception {
      return e;
    }
  }
}
