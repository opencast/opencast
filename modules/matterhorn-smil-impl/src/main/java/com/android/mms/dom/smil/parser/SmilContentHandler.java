/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.dom.smil.parser;

import com.android.mms.dom.smil.SmilDocumentImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.smil.SMILDocument;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SmilContentHandler extends DefaultHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SmilContentHandler.class);
  private static final String TAG = "SmilContentHandler";
  private static final boolean DEBUG = false;
  private static final boolean LOCAL_LOGV = false;

  private SMILDocument mSmilDocument;
  private Node mCurrentNode;

  /**
   * Resets this handler.
   * 
   */
  public void reset() {
    mSmilDocument = new SmilDocumentImpl();
    mCurrentNode = mSmilDocument;
  }

  /**
   * Returns the SMILDocument.
   * 
   * @return The SMILDocument instance
   */
  public SMILDocument getSmilDocument() {
    return mSmilDocument;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (LOCAL_LOGV) {
      logger.info(TAG, "SmilContentHandler.startElement. Creating element " + localName);
    }
    Element element = mSmilDocument.createElement(localName);
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        if (LOCAL_LOGV) {
          logger.info(TAG,
                  "Attribute " + i + " lname = " + attributes.getLocalName(i) + " value = " + attributes.getValue(i));
        }
        element.setAttribute(attributes.getLocalName(i), attributes.getValue(i));
      }
    }
    if (LOCAL_LOGV) {
      logger.info(TAG, "Appending " + localName + " to " + mCurrentNode.getNodeName());
    }
    mCurrentNode.appendChild(element);

    mCurrentNode = element;
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    if (LOCAL_LOGV) {
      logger.info(TAG, "SmilContentHandler.endElement. localName " + localName);
    }
    mCurrentNode = mCurrentNode.getParentNode();
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    if (LOCAL_LOGV) {
      logger.info(TAG, "SmilContentHandler.characters. ch = " + new String(ch, start, length));
    }
  }
}
