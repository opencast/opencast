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

import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.functions.Misc.chuck
import org.opencastproject.util.data.functions.Strings.asStringNull

import org.opencastproject.util.data.Function2

import java.io.File
import java.net.URI
import java.net.URL
import java.util.TreeSet

/**
 * `UrlSupport` is a helper class to deal with urls.
 */
object UrlSupport {
    val DEFAULT_BASE_URL = "http://localhost:8080"

    /** URI constructor function without checked exceptions.  */
    fun uri(uri: String): URI {
        try {
            return URI(uri)
        } catch (e: Exception) {
            return chuck(e)
        }

    }

    /** URL constructor function without checked exceptions.  */
    fun url(url: String): URL {
        try {
            return URL(url)
        } catch (e: Exception) {
            return chuck(e)
        }

    }

    /** URL constructor function without checked exceptions.  */
    fun url(protocol: String, host: String, port: Int): URL {
        try {
            return URL(protocol, host, port, "/")
        } catch (e: Exception) {
            return chuck(e)
        }

    }

    /**
     * URL constructor function without checked exceptions.
     *
     * @see URL
     */
    fun url(context: URL, spec: String): URL {
        try {
            return URL(context, spec)
        } catch (e: Exception) {
            return chuck(e)
        }

    }

    /**
     * Sorts the given urls by path.
     *
     * @param urls
     * the urls to sort
     * @return the sorted urls
     */
    fun sort(urls: Array<String>): Array<String> {
        val set = TreeSet<String>()
        for (i in urls.indices)
            set.add(urls[i])
        val result = arrayOfNulls<String>(urls.size)
        val i = set.iterator()
        var index = 0
        while (i.hasNext()) {
            result[index++] = i.toString()
        }
        return result
    }

    /**
     * Concatenates the two urls with respect to leading and trailing slashes. The path will always end with a trailing
     * slash.
     *
     * @return the concatenated url of the two arguments
     */
    @JvmOverloads
    fun concat(prefix: String?, suffix: String?, close: Boolean = false): String {
        var prefix = prefix
        var suffix = suffix
        if (prefix == null)
            throw IllegalArgumentException("Argument prefix is null")
        if (suffix == null)
            throw IllegalArgumentException("Argument suffix is null")

        prefix = checkSeparator(prefix)
        suffix = checkSeparator(suffix)
        prefix = removeDoubleSeparator(prefix)
        suffix = removeDoubleSeparator(suffix)

        if (!prefix.endsWith("/") && !suffix.startsWith("/"))
            prefix += "/"
        if (prefix.endsWith("/") && suffix.startsWith("/"))
            suffix = suffix.substring(1)

        prefix += suffix

        // Close?
        if (close && !prefix.endsWith("/")) {
            prefix += "/"
        }
        return prefix
    }

    /**
     * Concatenates the urls with respect to leading and trailing slashes.
     *
     * @param parts
     * the parts to concat
     * @return the concatenated url
     */
    fun concat(vararg parts: String): String {
        if (parts == null)
            throw IllegalArgumentException("Argument parts is null")
        if (parts.size == 0)
            throw IllegalArgumentException("Array parts is empty")
        var path = parts[0]
        for (i in 1 until parts.size) {
            if (parts[i] != null) {
                path = concat(path, parts[i])
            }
        }
        return path
    }

    /**
     * Concatenates the urls with respect to leading and trailing slashes.
     *
     * @param parts
     * the parts to concat
     * @return the concatenated url
     */
    fun concat(parts: List<String>?): String {
        if (parts == null)
            throw IllegalArgumentException("Argument parts is null")
        if (parts.size == 0)
            throw IllegalArgumentException("Array parts is empty")
        return mlist(parts).reducel(object : Function2<String, String, String>() {
            override fun apply(s: String, s1: String): String {
                return concat(s, s1)
            }
        })
    }

    /** Create a URI from the given parts.  */
    fun uri(vararg parts: Any): URI {
        return URI.create(concat(mlist(*parts).map(asStringNull()).value()))
    }

    /**
     * Returns the trimmed url. Trimmed means that the url is free from leading or trailing whitespace characters, and
     * that a directory url like `/news/` is closed by a slash (`/`).
     *
     * @param url
     * the url to trim
     * @return the trimmed url
     */
    fun trim(url: String?): String {
        var url: String? = url ?: throw IllegalArgumentException("Argument url is null")

        url = url!!.trim { it <= ' ' }
        url = checkSeparator(url)

        if (url.endsWith("/") || url.length == 1)
            return url

        var index = url.lastIndexOf("/")
        index = url.indexOf(".", index)
        if (index == -1)
            url += "/"
        return url
    }

    /**
     * Checks that the path only contains the web path separator "/". If not, wrong ones are replaced.
     */
    private fun checkSeparator(path: String): String {
        var sp = File.separator
        if ("\\" == sp)
            sp = "\\\\"
        return path.replace(sp.toRegex(), "/")
    }

    /**
     * Removes any occurrence of double separators ("//") and replaces it with "/".
     * Any double separators right after the protocol part are left untouched so that, e.g. http://localhost
     * stays http://localhost
     *
     * @param path
     * the path to check
     * @return the corrected path
     */
    fun removeDoubleSeparator(path: String): String {
        var path = path
        var protocol = ""
        var index = path.indexOf("://")

        // Strip off the protocol
        if (index != -1) {
            protocol = path.substring(0, index + 3)
            path = path.substring(index + 3)
        }

        // Search rest of path for double separators
        index = path.indexOf("//", index)
        while (index != -1) {
            path = path.substring(0, index) + path.substring(index + 1)
            index = path.indexOf("//", index)
        }
        return protocol + path
    }

    /**
     * Returns `true` if url `a` is a direct prefix of url `b`. For example,
     * `/news` is the parent of `/news/today`.
     *
     *
     * Note that `a` is also an extended prefix of `b` if `a` and `b` are
     * equal.
     *
     * @param a
     * the first url
     * @param b
     * the second url
     * @return `true` if `a` is the direct prefix of `b`
     */
    fun isPrefix(a: String, b: String): Boolean {
        if (isExtendedPrefix(a, b)) {
            if (a.length < b.length) {
                var bRest = b.substring(a.length + 1)
                if (bRest.endsWith("/"))
                    bRest = bRest.substring(0, bRest.length - 2)
                return bRest.indexOf("/", 1) < 0
            } else {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if url `a` is a prefix of url `b`. For example, `/news`
     * is an ancestor of `/news/today/morning`.
     *
     *
     * Note that `a` is also an extended prefix of `b` if `a` and `b` are
     * equal.
     *
     * @param a
     * the first url
     * @param b
     * the second url
     * @return `true` if `a` is a prefix of `b`
     */
    fun isExtendedPrefix(a: String, b: String): Boolean {
        return if (b.startsWith(a)) {
            if (b.length > a.length)
                a.endsWith("/") || b.substring(a.length).startsWith("/")
            else
                true
        } else false
    }

    /**
     * Returns the url extension that `url` defines over `prefix`. For example, the extension of url
     * `/news/today` and prefix `/news` is `today`.
     *
     *
     * If `prefix` is not a prefix of `url`, this method returns `null`, if
     * `url` and `prefix` match, the empty string is returned.
     *
     * @param prefix
     * the url prefix
     * @param url
     * the url
     * @return the url extension over the prefix
     */
    fun getExtension(prefix: String, url: String): String? {
        var prefix = prefix
        prefix = prefix.trim { it <= ' ' }
        if (isExtendedPrefix(prefix, url)) {
            if (url.length > prefix.length) {
                var extension = url.substring(prefix.length + 1)
                if (extension.endsWith("/")) {
                    extension = extension.substring(0, extension.length - 1)
                }
                return extension
            } else
                return ""
        }
        return null
    }

/**
 * Returns the extension that is encoded into the url. Possible extensions are:
 *
 *  * /*
 *  * /**
 *  * `null`
 *
 *
 * @param url
 * the url with extension
 * @return the url extension or `null` if no extension can be found
*/
fun getExtension(url:String):String? {
if (url.endsWith("/**"))
return "/**"
else if (url.endsWith("/*"))
return "/*"
else
return null
}

/**
 * Strips off the extension and returns the pure url.
 *
 * @param url
 * the url with extension
 * @return the url
*/
fun stripExtension(url:String):String {
val extension = getExtension(url)
if (extension == null)
return url
else
return url.substring(0, url.length - extension!!.length)
}

/**
 * Returns `true` if the url is valid, that is, if it contains only allowed characters.
 *
 * @return `true` or the invalid character
*/
fun isValid(url:String):Boolean {
return (checkUrl(url) == null)
}

/**
 * Returns `null` if the url is valid, that is, if it contains only allowed characters. Otherwhise, the
 * invalid character is returned.
 *
 * @return `null` or the invalid character
*/
fun getInvalidCharacter(url:String):Char? {
val c = checkUrl(url)
return c
}

/**
 * Returns `null` if the url is valid, that is, if it contains only allowed characters. Otherwhise, the
 * invalid character is returned.
 *
 * @return `null` or the invalid character
*/
private fun checkUrl(url:String):Char? {
val original = StringBuffer(url)
for (i in 0 until original.length)
{
val value = (original.get(i)).charValue().toInt()
// a-z
if (value >= 'a'.charValue().toInt() && value <= 'z'.charValue().toInt())
{
continue
}
// A-Z
if (value >= 'A'.charValue().toInt() && value <= 'Z'.charValue().toInt())
{
continue
}
// 0-9
if (value >= '0'.charValue().toInt() && value <= '9'.charValue().toInt())
{
continue
}
// Special characters
if (((value == '-'.charValue().toInt()) || (value == '_'.toInt()) || (value == '.'.toInt()) || (value == ','.toInt())
|| (value == ';'.toInt())))
{
continue
}
return Character.valueOf(original.get(i))
}
return null
}

}/**
 * This class should not be instanciated, since it only provides static utility methods.
*//**
 * Concatenates the two urls with respect to leading and trailing slashes.
 *
 *
 * Note that returned path will only end with a slash if `suffix` does. If you need a trailing slash, see
 * [.concat].
 *
 * @return the concatenated url of the two arguments
*/
