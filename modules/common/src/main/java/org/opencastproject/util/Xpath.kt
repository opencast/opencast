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

package org.opencastproject.util

import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.util.ArrayList

import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/** A thin layer around [XPath] to evaluate expressions in the context of a [Node].  */
class Xpath private constructor(private val node: Node) {
    private val xpath: XPath

    init {
        this.xpath = XPathFactory.newInstance().newXPath()
    }

    /** Evaluate the xpath expression against the contained document. The expression must return a node.  */
    // todo replace return type with Valid once it is implemented
    fun node(expr: String): Option<Node> {
        try {
            return option(xpath.evaluate(expr, node, XPathConstants.NODE) as Node)
        } catch (e: XPathExpressionException) {
            return none()
        }

    }

    /** Evaluate the xpath expression against the contained document. The expression must return a string (text).  */
    // todo replace return type with Valid once it is implemented
    fun string(expr: String): Option<String> {
        try {
            return option((xpath.evaluate(expr, node, XPathConstants.STRING) as String).trim { it <= ' ' })
        } catch (e: XPathExpressionException) {
            return none()
        }

    }

    /** Evaluate the xpath expression against the contained document. The expression must return a nodelist.  */
    // todo replace return type with Valid once it is implemented
    fun nodeSet(expr: String): Option<NodeList> {
        try {
            return option(xpath.evaluate(expr, node, XPathConstants.NODESET) as NodeList)
        } catch (e: XPathExpressionException) {
            return none()
        }

    }

    /** Evaluate the xpath expression against the contained document. The expression must return a list of strings (text).  */
    // todo replace return type with Valid once it is implemented
    fun strings(expr: String): List<String> {
        val list = ArrayList<String>()
        return nodeSet(expr).map(object : Function<NodeList, List<String>>() {
            override fun apply(nodes: NodeList): List<String> {
                for (i in 0 until nodes.length) {
                    list.add(nodes.item(i).nodeValue)
                }
                return list
            }
        }).getOrElse(list)
    }

    companion object {

        /** Create a new evaluation context for `node`.  */
        fun mk(node: Node): Xpath {
            return Xpath(node)
        }

        /** Create a new evaluation context for `node` respecting the given namespace resolutions.  */
        fun mk(node: Node, ns: NamespaceContext): Xpath {
            val xpath = Xpath(node)
            xpath.xpath.namespaceContext = ns
            return xpath
        }
    }
}
