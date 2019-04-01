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
import org.opencastproject.util.data.Option.option

import org.opencastproject.rest.RestConstants
import org.opencastproject.rest.SharedHttpContext
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.data.functions.Strings

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.component.ComponentContext

import java.util.Dictionary
import java.util.Enumeration
import java.util.HashMap
import java.util.Hashtable

import javax.servlet.Servlet

/** Contains general purpose OSGi utility functions.  */
object OsgiUtil {

    /**
     * Get a mandatory, non-blank value from the *bundle* context.
     *
     * @throws RuntimeException
     * key does not exist or its value is blank
     */
    fun getContextProperty(cc: ComponentContext, key: String): String {
        val p = cc.bundleContext.getProperty(key)
        if (StringUtils.isBlank(p))
            throw RuntimeException("Please provide context property $key")
        return StringUtils.trimToEmpty(p)
    }

    /**
     * Get an optional, non-blank value from the *bundle* context.
     *
     * @throws RuntimeException
     * key does not exist or its value is blank
     */
    fun getOptContextProperty(cc: ComponentContext, key: String): Option<String> {
        return option(cc.bundleContext.getProperty(key)).bind(Strings.trimToNone)
    }

    /**
     * Get a mandatory, non-blank value from the *component* context.
     *
     * @throws RuntimeException
     * key does not exist or its value is blank
     */
    fun getComponentContextProperty(cc: ComponentContext, key: String): String {
        val p = cc.properties.get(key) as String
        if (StringUtils.isBlank(p))
            throw RuntimeException("Please provide context property $key")
        return StringUtils.trimToEmpty(p)
    }

    /**
     * Get a mandatory, non-blank value from a dictionary.
     *
     * @throws ConfigurationException
     * key does not exist or its value is blank
     */
    @Throws(ConfigurationException::class)
    fun getCfg(d: Dictionary<*, *>, key: String): String {
        val p = d.get(key) ?: throw ConfigurationException(key, "does not exist")
        val ps = p.toString()
        if (StringUtils.isBlank(ps))
            throw ConfigurationException(key, "is blank")
        return StringUtils.trimToEmpty(ps)
    }

    /** Get a value from a dictionary. Return none if the key does either not exist or the value is blank.  */
    fun getOptCfg(d: Dictionary<*, *>, key: String): Option<String> {
        return option(d.get(key)).bind(Strings.asString()).bind(Strings.trimToNone)
    }

    /** Get a value from a dictionary. Return none if the key does either not exist or the value is blank.  */
    fun getOptCfgAsInt(d: Dictionary<*, *>, key: String): Option<Int> {
        return option(d.get(key)).bind(Strings.asString()).bind(Strings.toInt)
    }

    /**
     * Filter a dictionary by key prefix. For example the following map
     * `{w.p.key1: "value1", w.p.key2: "value2", x: "1"}` filtered by `filterByPrefix(d, "w.p.")`
     * returns `{key1: "value1", key2: "value"}`.
     */
    fun filterByPrefix(d: Dictionary<*, *>, prefix: String): Map<String, String> {
        val filtered = HashMap<String, String>()
        val prefixLength = prefix.length
        val keys = d.keys()
        while (keys.hasMoreElements()) {
            val key = keys.nextElement().toString()
            if (key.startsWith(prefix)) {
                filtered[key.substring(prefixLength)] = d.get(key).toString()
            }
        }
        return filtered
    }

    /**
     * Get an optional boolean from a dictionary.
     */
    fun getOptCfgAsBoolean(d: Dictionary<*, *>, key: String): Option<Boolean> {
        return option(d.get(key)).bind(Strings.asString()).map(Strings.toBool)
    }

    /**
     * Get a mandatory integer from a dictionary.
     *
     * @throws ConfigurationException
     * key does not exist or is not an integer
     */
    @Throws(ConfigurationException::class)
    fun getCfgAsInt(d: Dictionary<*, *>, key: String): Int {
        try {
            return Integer.parseInt(getCfg(d, key))
        } catch (e: NumberFormatException) {
            throw ConfigurationException(key, "not an integer")
        }

    }

    /**
     * Get a mandatory boolean from a dictionary.
     *
     * @throws ConfigurationException
     * key does not exist
     */
    @Throws(ConfigurationException::class)
    fun getCfgAsBoolean(d: Dictionary<*, *>, key: String): Boolean {
        val p = d.get(key) ?: throw ConfigurationException(key, "does not exist")
        return BooleanUtils.toBoolean(p.toString())
    }

    /**
     * Check the existence of the given dictionary. Throw an exception if null.
     */
    @Throws(ConfigurationException::class)
    fun checkDictionary(properties: Dictionary<*, *>?, componentContext: ComponentContext) {
        if (properties == null) {
            val dicName = componentContext.properties.get("service.pid").toString()
            throw ConfigurationException("*", "Dictionary for $dicName does not exist")
        }
    }

    /** Create a config info string suitable for logging purposes.  */
    fun showConfig(vararg cfg: Tuple<String, *>): String {
        return "Config\n" + Collections.mkString(mlist(*cfg).map(object : Function<Tuple<String, *>, String>() {
            override fun apply(t: Tuple<String, *>): String {
                return t.a + "=" + t.b.toString()
            }
        }).value(), "\n")
    }

    fun registerServlet(bundleContext: BundleContext, service: Any, alias: String): ServiceRegistration<*> {
        val resourceProps = Hashtable<String, String>()
        resourceProps[SharedHttpContext.CONTEXT_ID] = RestConstants.HTTP_CONTEXT_ID
        resourceProps[SharedHttpContext.SHARED] = "true"
        resourceProps[SharedHttpContext.ALIAS] = alias
        return bundleContext.registerService(Servlet::class.java!!.getName(), service, resourceProps)
    }

}
