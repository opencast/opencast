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
package org.opencastproject.comments;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Option;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Convenience implementation that supports serializing and deserializing comment catalogs.
 */
public final class CommentParser {

  private static final String NAMESPACE = "http://comment.opencastproject.org";

  private CommentParser() {
  }

  /** The xpath facility */
  private static final XPath xpath = XPathFactory.newInstance().newXPath();

  /**
   * Parses the comments catalog and returns its object representation.
   * 
   * @param xml
   *          the serialized comments
   * @param userDirectoryService
   *          the user directory service
   * @return the comments list
   * @throws CommentException
   *           unable to parse comment from XML
   */
  public static List<Comment> getCommentsFromXml(String xml, UserDirectoryService userDirectoryService)
          throws CommentException {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(IOUtils.toInputStream(xml, "UTF-8"));

      List<Comment> comments = new ArrayList<Comment>();
      NodeList commentNodes = (NodeList) xpath.evaluate("comments/comment", doc.getDocumentElement(),
              XPathConstants.NODESET);
      for (int i = 0; i < commentNodes.getLength(); i++) {
        comments.add(commentFromManifest(commentNodes.item(i), userDirectoryService));
      }

      return comments;
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  /**
   * Parses the comment catalog and returns its object representation.
   * 
   * @param xml
   *          the serialized comment
   * @param userDirectoryService
   *          the user directory service
   * @return the comment instance
   * @throws CommentException
   *           unable to parse comment from XML
   */
  public static Comment getCommentFromXml(String xml, UserDirectoryService userDirectoryService)
          throws CommentException {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(IOUtils.toInputStream(xml, "UTF-8"));
      return commentFromManifest(doc.getDocumentElement(), userDirectoryService);
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  /**
   * Parses the comment reply catalog and returns its object representation.
   * 
   * @param xml
   *          the serialized comment reply
   * @param userDirectoryService
   *          the user directory service
   * @return the comment reply instance
   * @throws CommentException
   *           unable to parse comment reply from XML
   */
  public static CommentReply getCommentReplyFromXml(String xml, UserDirectoryService userDirectoryService)
          throws CommentException {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(IOUtils.toInputStream(xml, "UTF-8"));
      return replyFromManifest(doc.getDocumentElement(), userDirectoryService);
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  private static Comment commentFromManifest(Node commentNode, UserDirectoryService userDirectoryService)
          throws UnsupportedElementException {
    try {
      // id
      Long id = null;
      Double idAsDouble = ((Number) xpath.evaluate("@id", commentNode, XPathConstants.NUMBER)).doubleValue();
      if (!idAsDouble.isNaN())
        id = idAsDouble.longValue();

      // text
      String text = (String) xpath.evaluate("text/text()", commentNode, XPathConstants.STRING);

      // Author
      Node authorNode = (Node) xpath.evaluate("author", commentNode, XPathConstants.NODE);
      User author = userFromManifest(authorNode, userDirectoryService);

      // ResolvedStatus
      Boolean resolved = BooleanUtils.toBoolean((Boolean) xpath.evaluate("@resolved", commentNode,
              XPathConstants.BOOLEAN));

      // Reason
      String reason = (String) xpath.evaluate("reason/text()", commentNode, XPathConstants.STRING);
      if (StringUtils.isNotBlank(reason))
        reason = reason.trim();

      // CreationDate
      String creationDateString = (String) xpath.evaluate("creationDate/text()", commentNode, XPathConstants.STRING);
      Date creationDate = new Date(DateTimeSupport.fromUTC(creationDateString));

      // ModificationDate
      String modificationDateString = (String) xpath.evaluate("modificationDate/text()", commentNode,
              XPathConstants.STRING);
      Date modificationDate = new Date(DateTimeSupport.fromUTC(modificationDateString));

      // Create comment
      Comment comment = Comment.create(Option.option(id), text.trim(), author, reason, resolved, creationDate,
              modificationDate);

      // Replies
      NodeList replyNodes = (NodeList) xpath.evaluate("replies/reply", commentNode, XPathConstants.NODESET);
      for (int i = 0; i < replyNodes.getLength(); i++) {
        comment.addReply(replyFromManifest(replyNodes.item(i), userDirectoryService));
      }

      return comment;
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading comment information from manifest", e);
    } catch (Exception e) {
      if (e instanceof UnsupportedElementException)
        throw (UnsupportedElementException) e;
      throw new UnsupportedElementException(
              "Error while reading comment creation or modification date information from manifest", e);
    }
  }

  private static CommentReply replyFromManifest(Node commentReplyNode, UserDirectoryService userDirectoryService)
          throws UnsupportedElementException {
    try {
      // id
      Long id = null;
      Double idAsDouble = ((Number) xpath.evaluate("@id", commentReplyNode, XPathConstants.NUMBER)).doubleValue();
      if (!idAsDouble.isNaN())
        id = idAsDouble.longValue();

      // text
      String text = (String) xpath.evaluate("text/text()", commentReplyNode, XPathConstants.STRING);

      // Author
      Node authorNode = (Node) xpath.evaluate("author", commentReplyNode, XPathConstants.NODE);
      User author = userFromManifest(authorNode, userDirectoryService);

      // CreationDate
      String creationDateString = (String) xpath.evaluate("creationDate/text()", commentReplyNode,
              XPathConstants.STRING);
      Date creationDate = new Date(DateTimeSupport.fromUTC(creationDateString));

      // ModificationDate
      String modificationDateString = (String) xpath.evaluate("modificationDate/text()", commentReplyNode,
              XPathConstants.STRING);
      Date modificationDate = new Date(DateTimeSupport.fromUTC(modificationDateString));

      // Create reply
      return CommentReply.create(Option.option(id), text.trim(), author, creationDate, modificationDate);
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading comment reply information from manifest", e);
    } catch (Exception e) {
      if (e instanceof UnsupportedElementException)
        throw (UnsupportedElementException) e;
      throw new UnsupportedElementException(
              "Error while reading comment reply creation or modification date information from manifest", e);
    }
  }

  private static User userFromManifest(Node authorNode, UserDirectoryService userDirectoryService) {
    try {
      // Username
      String userName = (String) xpath.evaluate("username/text()", authorNode, XPathConstants.STRING);
      return userDirectoryService.loadUser(userName);
    } catch (XPathExpressionException e) {
      throw new UnsupportedElementException("Error while reading comment author information from manifest", e);
    }
  }

  /**
   * Serializes a comment collection.
   * 
   * @param comments
   *          the comment collection
   * @return the serialized comment collection
   * @throws CommentException
   *           unable to serialize comment collection to XML
   */
  public static String getAsXml(Collection<Comment> comments) throws CommentException {
    Document doc = newDocument();
    // Root element "comments"
    Element commentsXml = doc.createElementNS(NAMESPACE, "comments");
    commentsXml.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NAMESPACE);
    doc.appendChild(commentsXml);

    for (Comment c : comments) {
      commentsXml.appendChild(getAsXmlDocument(c, doc));
    }

    return serializeNode(commentsXml.getOwnerDocument());
  }

  /**
   * Serializes the comment.
   * 
   * @param comment
   *          the comment
   * @return the serialized comment
   * @throws CommentException
   *           unable to serialize comment to XML
   */
  public static String getAsXml(Comment comment) throws CommentException {
    Document doc = newDocument();

    Element commentNode = getAsXmlDocument(comment, doc);
    return serializeNode(doc.appendChild(commentNode).getOwnerDocument());
  }

  /**
   * Serializes the comment reply.
   * 
   * @param reply
   *          the comment reply
   * @return the serialized comment reply
   * @throws CommentException
   *           unable to serialize comment reply to XML
   */
  public static String getAsXml(CommentReply reply) throws CommentException {
    Document xmlDocument = getAsXmlDocument(reply);
    return serializeNode(xmlDocument);
  }

  /**
   * Serializes the comment to a {@link org.w3c.dom.Document}.
   * 
   * @param comment
   *          the comment
   * @return the serialized comment
   */
  private static Element getAsXmlDocument(Comment comment, Document doc) {
    // Root element "comment"
    Element commentXml = doc.createElementNS(NAMESPACE, "comment");
    commentXml.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NAMESPACE);

    // Identifier
    if (comment.getId().isSome())
      commentXml.setAttribute("id", comment.getId().get().toString());

    // Resolved status
    commentXml.setAttribute("resolved", Boolean.toString(comment.isResolvedStatus()));

    // Author
    Element authorNode = getAuthorNode(comment.getAuthor(), doc);
    commentXml.appendChild(authorNode);

    // Creation date
    Element creationDate = doc.createElement("creationDate");
    creationDate.setTextContent(DateTimeSupport.toUTC(comment.getCreationDate().getTime()));
    commentXml.appendChild(creationDate);

    // Modification date
    Element modificationDate = doc.createElement("modificationDate");
    modificationDate.setTextContent(DateTimeSupport.toUTC(comment.getModificationDate().getTime()));
    commentXml.appendChild(modificationDate);

    // Text
    Element text = doc.createElement("text");
    if (StringUtils.isNotBlank(comment.getText()))
      text.appendChild(doc.createCDATASection(comment.getText()));
    commentXml.appendChild(text);

    // Reason
    Element reason = doc.createElement("reason");
    if (StringUtils.isNotBlank(comment.getReason()))
      reason.setTextContent(comment.getReason());
    commentXml.appendChild(reason);

    // Replies
    Element repliesNode = doc.createElement("replies");
    for (CommentReply r : comment.getReplies()) {
      repliesNode.appendChild(getAsXml(r, doc));
    }
    commentXml.appendChild(repliesNode);

    return commentXml;
  }

  /**
   * Serializes the comment reply to a {@link org.w3c.dom.Document}.
   * 
   * @param reply
   *          the comment reply
   * @return the serialized comment reply
   */
  private static Document getAsXmlDocument(CommentReply reply) {
    Document doc = newDocument();

    Element replyNode = getAsXml(reply, doc);
    return doc.appendChild(replyNode).getOwnerDocument();
  }

  private static Element getAsXml(CommentReply reply, Document doc) {
    // Root element "comment"
    Element replyXml = doc.createElementNS(NAMESPACE, "reply");
    replyXml.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NAMESPACE);

    // Identifier
    if (reply.getId().isSome())
      replyXml.setAttribute("id", reply.getId().get().toString());

    // Author
    Element authorNode = getAuthorNode(reply.getAuthor(), doc);
    replyXml.appendChild(authorNode);

    // Creation date
    Element creationDate = doc.createElement("creationDate");
    creationDate.setTextContent(DateTimeSupport.toUTC(reply.getCreationDate().getTime()));
    replyXml.appendChild(creationDate);

    // Modification date
    Element modificationDate = doc.createElement("modificationDate");
    modificationDate.setTextContent(DateTimeSupport.toUTC(reply.getModificationDate().getTime()));
    replyXml.appendChild(modificationDate);

    // Text
    Element text = doc.createElement("text");
    if (StringUtils.isNotBlank(reply.getText()))
      text.appendChild(doc.createCDATASection(reply.getText()));

    replyXml.appendChild(text);

    return replyXml;
  }

  private static Element getAuthorNode(User author, Document doc) {
    Element authorNode = doc.createElement("author");
    Element username = doc.createElement("username");
    username.setTextContent(author.getUsername());
    authorNode.appendChild(username);
    Element email = doc.createElement("email");
    email.setTextContent(author.getEmail());
    authorNode.appendChild(email);
    Element name = doc.createElement("name");
    if (StringUtils.isNotBlank(author.getName()))
      name.appendChild(doc.createCDATASection(author.getName()));
    authorNode.appendChild(name);
    return authorNode;
  }

  private static String serializeNode(Document xmlDocument) throws CommentException {
    // Serialize the document
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
      return writer.getBuffer().toString().trim();
    } catch (Exception e) {
      throw new CommentException(e);
    }
  }

  /** Create a new DOM document. */
  private static Document newDocument() {
    final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    try {
      return docBuilderFactory.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      return chuck(e);
    }
  }

}
