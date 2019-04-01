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

package org.opencastproject.mediapackage

import java.lang.String.format

import org.opencastproject.util.data.Function

import java.io.Serializable
import java.util.Objects

import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * ELement flavors describe [MediaPackageElement]s in a semantic way. They reveal or give at least a hint about
 * the meaning of an element.
 *
 */
@XmlJavaTypeAdapter(MediaPackageElementFlavor.FlavorAdapter::class)
class MediaPackageElementFlavor : Cloneable, Comparable<MediaPackageElementFlavor>, Serializable {

    /**
     * String representation of type
     */
    /**
     * Returns the type of this flavor.
     * The type is never `null`.
     *
     *
     * For example, if the type is a presentation movie which is represented as `presentation/source`,
     * this method will return `presentation`.
     *
     * @return the type of this flavor
     */
    var type: String? = null
        private set

    /**
     * String representation of subtype
     */
    /**
     * Returns the subtype of this flavor.
     * The subtype is never `null`.
     *
     *
     * For example, if the subtype is a presentation movie which is represented as `presentation/source`,
     * this method will return `source`.
     *
     * @return the subtype
     */
    var subtype: String? = null
        private set


    private constructor() {}

    /**
     * Creates a new flavor with the given type and subtype.
     *
     * @param type
     * the type of the flavor
     * @param subtype
     * the subtype of the flavor
     */
    constructor(type: String, subtype: String) {
        this.type = checkPartSyntax(type)
        this.subtype = checkPartSyntax(subtype)
    }

    /**
     * Checks that any of the parts this flavor consists of abide to the syntax restrictions
     *
     * @param part
     * @return
     */
    private fun checkPartSyntax(part: String?): String {
        // Parts may not be null
        if (part == null)
            throw IllegalArgumentException("Flavor parts may not be null!")

        // Parts may not contain the flavor separator character
        if (part.contains(SEPARATOR))
            throw IllegalArgumentException(
                    format("Invalid flavor part \"%s\". Flavor parts may not contain '%s'!", part, SEPARATOR))

        // Parts may not contain leading and trailing blanks, and may only consist of lowercase letters
        val adaptedPart = part.trim { it <= ' ' }.toLowerCase()

        // Parts may not be empty
        if (adaptedPart.isEmpty())
            throw IllegalArgumentException(
                    format("Invalid flavor part \"%s\". Flavor parts may not be blank or empty!", part))

        return adaptedPart
    }

/**
 * "Applies" this flavor to the given target flavor. E.g. applying '*\/preview' to 'presenter/source' yields
 * 'presenter/preview', applying 'presenter/*' to 'foo/source' yields 'presenter/source', and applying 'foo/bar' to
 * 'presenter/source' yields 'foo/bar'.
 *
 * @param target The target flavor to apply this flavor to.
 *
 * @return The resulting flavor.
*/
fun applyTo(target:MediaPackageElementFlavor):MediaPackageElementFlavor {
var type = this.type
var subtype = this.subtype
if (WILDCARD == this.type)
{
type = target.type
}
if (WILDCARD == this.subtype)
{
subtype = target.subtype
}
return MediaPackageElementFlavor(type, subtype)
}

/**
 * @see java.lang.Object.clone
*/
@Throws(CloneNotSupportedException::class)
public override fun clone():MediaPackageElementFlavor {
val m = super.clone() as MediaPackageElementFlavor
m.type = this.type
m.subtype = this.subtype
return m
}

/**
 * Defines equality between flavors and strings.
 *
 * @param flavor
 * string of the form "type/subtype"
*/
fun eq(flavor:String?):Boolean {
return flavor != null && flavor == toString()
}

/**
 * @see java.lang.String.compareTo
*/
public override fun compareTo(m:MediaPackageElementFlavor):Int {
return toString().compareTo(m.toString())
}

/**
 * Returns the flavor as a string "type/subtype".
*/
public override fun toString():String {
return type + SEPARATOR + subtype
}

/**
 * JAXB adapter implementation.
*/
internal class FlavorAdapter:XmlAdapter<String, MediaPackageElementFlavor>() {
@Throws(Exception::class)
public override fun marshal(flavor:MediaPackageElementFlavor?):String? {
if (flavor == null)
{
return null
}
return flavor!!.toString()
}

@Throws(Exception::class)
public override fun unmarshal(str:String):MediaPackageElementFlavor {
return parseFlavor(str)
}
}

/**
 * Check if two flavors match.
 * Two flavors match if both their type and subtype matches.
 *
 * @param other
 * Flavor to compare against
 * @return  If the flavors match
*/
fun matches(other:MediaPackageElementFlavor?):Boolean {
return (other != null
&& typeMatches(type!!, other!!.type)
&& typeMatches(subtype!!, other!!.subtype))
}

public override fun hashCode():Int {
return Objects.hash(type, subtype)
}

public override fun equals(other:Any?):Boolean {
return ((other is MediaPackageElementFlavor)
&& type == (other as MediaPackageElementFlavor).type
&& subtype == (other as MediaPackageElementFlavor).subtype)
}

companion object {

/**
 * Wildcard character used in type and subtype
*/
val WILDCARD = "*"

/**
 * Serial version uid
*/
private const val serialVersionUID = 1L

/**
 * Character that separates both parts of a flavor
*/
private val SEPARATOR = "/"

/** Constructor function for [.MediaPackageElementFlavor].  */
fun flavor(type:String, subtype:String):MediaPackageElementFlavor {
return MediaPackageElementFlavor(type, subtype)
}

/**
 * Creates a new media package element flavor.
 *
 * @param s
 * the media package flavor
 * @return the media package element flavor object
 * @throws IllegalArgumentException
 * if the string `s` does not contain a *dash* to divide the type from subtype.
*/
@Throws(IllegalArgumentException::class)
fun parseFlavor(s:String?):MediaPackageElementFlavor {
if (s == null)
throw IllegalArgumentException("Unable to create element flavor from 'null'")
val parts = s!!.split((SEPARATOR).toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
if (parts.size != 2)
throw IllegalArgumentException(format("Unable to create element flavor from \"%s\"", s))
return MediaPackageElementFlavor(parts[0], parts[1])
}

val parseFlavor:Function<String, MediaPackageElementFlavor> = object:Function<String, MediaPackageElementFlavor>() {
public override fun apply(s:String):MediaPackageElementFlavor {
return parseFlavor(s)
}
}

/**
 * Check if types match.
 * Two types match if they are equal or if one of them is a [wildcard][.WILDCARD].
*/
private fun typeMatches(a:String, b:String?):Boolean {
return a == b || WILDCARD == a || WILDCARD == b
}
}

}
