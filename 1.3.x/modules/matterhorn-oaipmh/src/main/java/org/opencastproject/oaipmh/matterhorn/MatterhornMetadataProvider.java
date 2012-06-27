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

package org.opencastproject.oaipmh.matterhorn;

import org.opencastproject.oaipmh.server.MetadataFormat;
import org.opencastproject.oaipmh.server.MetadataProvider;
import org.opencastproject.oaipmh.server.OaiPmhRepository;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.search.api.SearchResultItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class MatterhornMetadataProvider implements MetadataProvider {

  private static final MetadataFormat METADATA_FORMAT = new MetadataFormat() {
    @Override
    public String getPrefix() {
      return "matterhorn";
    }

    @Override
    public URL getSchema() {
      // todo define a location for the schema
      try {
        return new URL("http://www.opencastproject.org/oai/matterhorn.xsd");
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public URI getNamespace() {
      try {
        return new URI("http://www.opencastproject.org/oai/matterhorn");
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  };

  @Override
  public MetadataFormat getMetadataFormat() {
    return METADATA_FORMAT;
  }

  @Override
  public Element createMetadata(OaiPmhRepository repository, final SearchResultItem item) {
    final Document mp;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      mp = builder.parse(new InputSource(new StringReader(item.getOcMediapackage())));
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    XmlGen xml = new XmlGen() {
      @Override
      public Element create() {
        return (Element) mp.getFirstChild();
      }
    };
    return xml.create();
  }
}
