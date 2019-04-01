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
package org.opencastproject.metadata.dublincore

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.XMLCatalogImpl
import org.opencastproject.metadata.api.MetadataCatalog
import org.opencastproject.util.RequireUtil
import org.opencastproject.util.XmlNamespaceContext
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function2

import com.entwinemedia.fn.Fns
import com.entwinemedia.fn.data.ImmutableSetWrapper

import org.apache.commons.collections4.Closure
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.Predicate
import org.apache.commons.collections4.Transformer
import org.w3c.dom.Document
import org.xml.sax.Attributes

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.annotation.ParametersAreNonnullByDefault
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

/**
 * Catalog for DublinCore structured metadata to be serialized as XML.
 *
 *
 * Attention: Encoding schemes are not preserved! See http://opencast.jira.com/browse/MH-8759
 */
@ParametersAreNonnullByDefault
class DublinCoreCatalog
/** Create a new catalog.  */
internal constructor() : XMLCatalogImpl(), DublinCore, MetadataCatalog, Cloneable {

    var rootTag: EName? = null

    private val toDublinCoreValue = object : Function<XMLCatalogImpl.CatalogEntry, DublinCoreValue>() {
        override fun apply(e: XMLCatalogImpl.CatalogEntry): DublinCoreValue {
            return toDublinCoreValue(e)
        }
    }

    override val values: Map<EName, List<DublinCoreValue>>
        get() = mlist(data.values.iterator())
                .foldl(HashMap(),
                        object : Function2<HashMap<EName, List<DublinCoreValue>>, List<XMLCatalogImpl.CatalogEntry>, HashMap<EName, List<DublinCoreValue>>>() {
                            override fun apply(map: HashMap<EName, List<DublinCoreValue>>,
                                               entries: List<XMLCatalogImpl.CatalogEntry>): HashMap<EName, List<DublinCoreValue>> {
                                if (entries.size > 0) {
                                    val property = entries[0].eName
                                    map[property] = mlist(entries).map(toDublinCoreValue).value()
                                }
                                return map
                            }
                        })

    override val valuesFlat: List<DublinCoreValue>
        get() = `$`(data.values).bind(Fns.id()).map(toDublinCoreValue.toFn()).toList()

    override val properties: Set<EName>
        get() = ImmutableSetWrapper(data.keys)

    // make public
    public override val entriesSorted: List<XMLCatalogImpl.CatalogEntry>
        get() = super.entriesSorted

    fun addBindings(ctx: XmlNamespaceContext) {
        bindings = this.bindings.add(ctx)
    }

    override fun toString(): String {
        return "DublinCore" + if (identifier != null) "($identifier)" else ""
    }

    override fun get(property: EName, language: String): List<String> {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")
        if (DublinCore.LANGUAGE_ANY == language) {
            return CollectionUtils.collect<CatalogEntry, Any>(getValuesAsList(property), object : Transformer {
                override fun transform(o: Any): Any? {
                    return (o as XMLCatalogImpl.CatalogEntry).value
                }
            }) as List<String>
        } else {
            val values = ArrayList<String>()
            val langUndef = DublinCore.LANGUAGE_UNDEFINED == language
            CollectionUtils.forAllDo<CatalogEntry, >(getValuesAsList(property), object : Closure {
                override fun execute(o: Any) {
                    val c = o as XMLCatalogImpl.CatalogEntry
                    val lang = c.getAttribute(XMLCatalogImpl.XML_LANG_ATTR)
                    if (langUndef && lang == null || language == lang)
                        values.add(c.value)
                }
            })
            return values
        }
    }

    override fun get(property: EName): List<DublinCoreValue> {
        RequireUtil.notNull(property, "property")
        return mlist(getValuesAsList(property)).map(toDublinCoreValue).value()
    }

    private fun toDublinCoreValue(e: XMLCatalogImpl.CatalogEntry): DublinCoreValue {
        val langRaw = e.getAttribute(XMLCatalogImpl.XML_LANG_ATTR)
        val lang = langRaw ?: DublinCore.LANGUAGE_UNDEFINED
        val typeRaw = e.getAttribute(XMLCatalogImpl.XSI_TYPE_ATTR)
        return if (typeRaw != null) {
            DublinCoreValue.mk(e.value!!, lang, toEName(typeRaw))
        } else {
            DublinCoreValue.mk(e.value!!, lang)
        }
    }

    override fun getFirst(property: EName, language: String): String? {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")

        val f = getFirstCatalogEntry(property, language)
        return f?.value
    }

    override fun getFirst(property: EName): String? {
        RequireUtil.notNull(property, "property")

        val f = getFirstCatalogEntry(property, DublinCore.LANGUAGE_ANY)
        return f?.value
    }

    override fun getFirstVal(property: EName): DublinCoreValue? {
        val f = getFirstCatalogEntry(property, DublinCore.LANGUAGE_ANY)
        return if (f != null) toDublinCoreValue(f) else null
    }

    private fun getFirstCatalogEntry(property: EName, language: String): XMLCatalogImpl.CatalogEntry? {
        var entry: XMLCatalogImpl.CatalogEntry? = null
        when (language) {
            DublinCore.LANGUAGE_UNDEFINED -> entry = getFirstLocalizedValue(property, null!!)
            DublinCore.LANGUAGE_ANY -> for (value in getValuesAsList(property)) {
                entry = value
                // Prefer values without language information
                if (!value.hasAttribute(XMLCatalogImpl.XML_LANG_ATTR))
                    break
            }
            else -> entry = getFirstLocalizedValue(property, language)
        }
        return entry
    }

    override fun getAsText(property: EName, language: String, delimiter: String): String? {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")
        RequireUtil.notNull(delimiter, "delimiter")
        val values: List<XMLCatalogImpl.CatalogEntry>
        when (language) {
            DublinCore.LANGUAGE_UNDEFINED -> values = getLocalizedValuesAsList(property, null!!)
            DublinCore.LANGUAGE_ANY -> values = getValuesAsList(property)
            else -> values = getLocalizedValuesAsList(property, language)
        }
        return if (values.size > 0) `$`(values).mkString(delimiter) else null
    }

    override fun getLanguages(property: EName): Set<String> {
        RequireUtil.notNull(property, "property")
        val languages = HashSet<String>()
        for (entry in getValuesAsList(property)) {
            val language = entry.getAttribute(XMLCatalogImpl.XML_LANG_ATTR)
            if (language != null)
                languages.add(language)
            else
                languages.add(DublinCore.LANGUAGE_UNDEFINED)
        }
        return languages
    }

    override fun hasMultipleValues(property: EName, language: String): Boolean {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")
        return hasMultiplePropertyValues(property, language)
    }

    override fun hasMultipleValues(property: EName): Boolean {
        RequireUtil.notNull(property, "property")
        return hasMultiplePropertyValues(property, DublinCore.LANGUAGE_ANY)
    }

    private fun hasMultiplePropertyValues(property: EName, language: String): Boolean {
        if (DublinCore.LANGUAGE_ANY == language) {
            return getValuesAsList(property).size > 1
        } else {
            var counter = 0
            for (entry in getValuesAsList(property)) {
                if (equalLanguage(language, entry.getAttribute(XMLCatalogImpl.XML_LANG_ATTR)))
                    counter++
                if (counter > 1)
                    return true
            }
            return false
        }
    }

    override fun hasValue(property: EName, language: String): Boolean {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")
        return hasPropertyValue(property, language)
    }

    override fun hasValue(property: EName): Boolean {
        RequireUtil.notNull(property, "property")
        return hasPropertyValue(property, DublinCore.LANGUAGE_ANY)
    }

    private fun hasPropertyValue(property: EName, language: String): Boolean {
        return if (DublinCore.LANGUAGE_ANY == language) {
            getValuesAsList(property).size > 0
        } else {
            CollectionUtils.find(getValuesAsList(property), object : Predicate {
                override fun evaluate(o: Any): Boolean {
                    return equalLanguage((o as XMLCatalogImpl.CatalogEntry).getAttribute(XMLCatalogImpl.XML_LANG_ATTR), language)
                }
            }) != null
        }
    }

    override fun set(property: EName, value: String?, language: String) {
        RequireUtil.notNull(property, "property")
        if (language == null || DublinCore.LANGUAGE_ANY == language)
            throw IllegalArgumentException("Language code may not be null or LANGUAGE_ANY")
        setValue(property, value, language, null)
    }

    override operator fun set(property: EName, value: String) {
        RequireUtil.notNull(property, "property")
        setValue(property, value, DublinCore.LANGUAGE_UNDEFINED, null)
    }

    override fun set(property: EName, value: DublinCoreValue?) {
        RequireUtil.notNull(property, "property")
        if (value != null) {
            setValue(property, value.value, value.language, value.encodingScheme.orNull())
        } else {
            removeValue(property, DublinCore.LANGUAGE_ANY)
        }
    }

    override fun set(property: EName, values: List<DublinCoreValue>) {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(values, "values")
        removeValue(property, DublinCore.LANGUAGE_ANY)
        for (v in values) {
            add(property, v)
        }
    }

    private fun setValue(property: EName, value: String?, language: String, encodingScheme: EName?) {
        if (value == null) {
            // No value, remove the whole element
            removeValue(property, language)
        } else {
            val lang = if (DublinCore.LANGUAGE_UNDEFINED != language) language else null
            removeLocalizedValues(property, lang!!)
            add(property, value, language, encodingScheme)
        }
    }

    override fun add(property: EName, value: String) {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(value, "value")

        add(property, value, DublinCore.LANGUAGE_UNDEFINED, null)
    }

    override fun add(property: EName, value: String, language: String) {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(value, "value")
        if (language == null || DublinCore.LANGUAGE_ANY == language)
            throw IllegalArgumentException("Language code may not be null or LANGUAGE_ANY")

        add(property, value, language, null)
    }

    override fun add(property: EName, value: DublinCoreValue) {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(value, "value")

        add(property, value.value, value.language, value.encodingScheme.orNull())
    }

    internal fun add(property: EName, value: String, language: String, encodingScheme: EName?) {
        // Ignore empty rootTag element
        if (DublinCore.LANGUAGE_UNDEFINED == language && property != rootTag) {
            if (encodingScheme == null) {
                addElement(property, value)
            } else {
                addTypedElement(property, value, encodingScheme)
            }
        } else {
            // Language defined
            if (encodingScheme == null) {
                addLocalizedElement(property, value, language)
            } else {
                addTypedLocalizedElement(property, value, language, encodingScheme)
            }
        }
    }

    override fun remove(property: EName, language: String) {
        RequireUtil.notNull(property, "property")
        RequireUtil.notNull(language, "language")
        removeValue(property, language)
    }

    override fun remove(property: EName) {
        RequireUtil.notNull(property, "property")
        removeValue(property, DublinCore.LANGUAGE_ANY)
    }

    private fun removeValue(property: EName, language: String) {
        when (language) {
            DublinCore.LANGUAGE_ANY -> removeElement(property)
            DublinCore.LANGUAGE_UNDEFINED -> removeLocalizedValues(property, null!!)
            else -> removeLocalizedValues(property, language)
        }
    }

    override fun clear() {
        super.clear()
    }

    override fun clone(): Any {
        val clone = DublinCoreCatalog()
        clone.identifier = identifier
        clone.flavor = flavor
        clone.setSize(getSize())
        clone.checksum = checksum
        clone.bindings = bindings // safe, since XmlNamespaceContext is immutable
        clone.rootTag = rootTag
        for ((elmName, value) in data) {
            val elmNameCopy = EName(elmName.namespaceURI, elmName.localName)
            val elmsCopy = ArrayList<XMLCatalogImpl.CatalogEntry>()
            for (catalogEntry in value) {
                elmsCopy.add(XMLCatalogImpl.CatalogEntry(catalogEntry.eName, catalogEntry.value, catalogEntry.getAttributes()))
            }
            clone.data[elmNameCopy] = elmsCopy
        }
        return clone
    }

    internal fun equalLanguage(a: String, b: String): Boolean {
        return (a == null && eq(b, DublinCore.LANGUAGE_UNDEFINED) || b == null && eq(a, DublinCore.LANGUAGE_UNDEFINED) || eq(a, DublinCore.LANGUAGE_ANY)
                || eq(b, DublinCore.LANGUAGE_ANY) || a != null && eq(a, b))
    }

    // make public
    public override fun toEName(qName: String): EName {
        return super.toEName(qName)
    }

    // make public
    public override fun toQName(eName: EName): String {
        return super.toQName(eName)
    }

    // make public
    override fun addElement(element: EName, value: String, attributes: Attributes) {
        // Ignore empty root element
        if (rootTag != element) {
            super.addElement(element, value, attributes)
        }
    }

    // make public
    public override fun getValues(element: EName): Array<XMLCatalogImpl.CatalogEntry> {
        return super.getValues(element)
    }

    /**
     * Saves the dublin core metadata container to a dom.
     *
     * @throws ParserConfigurationException
     * if the xml parser environment is not correctly configured
     * @throws TransformerException
     * if serialization of the metadata document fails
     * @throws IOException
     * if an error with catalog serialization occurs
     */
    @Throws(ParserConfigurationException::class, TransformerException::class, IOException::class)
    override fun toXml(): Document {
        return DublinCoreXmlFormat.writeDocument(this)
    }

    @Throws(IOException::class)
    override fun toJson(): String {
        return DublinCoreJsonFormat.writeJsonObject(this).toJSONString()
    }

    companion object {
        private val serialVersionUID = -4568663918115847488L

        /** A flavor that matches any dublin core element  */
        val ANY_DUBLINCORE = MediaPackageElementFlavor.parseFlavor("dublincore/*")
    }
}
