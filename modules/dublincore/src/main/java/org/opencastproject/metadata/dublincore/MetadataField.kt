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

import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import org.apache.commons.lang3.exception.ExceptionUtils.getMessage
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.json.simple.JSONArray
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.TimeZone

/**
 * This is a generic and very abstract view of a certain field/property in a metadata catalog. The main purpose of this
 * class is to have a generic access to the variety of information stored in metadata catalogs.
 *
 * @param <A>
 * Defines the type of the metadata value
</A> */
class MetadataField<A> {

    /** The id of a collection to validate values against.  */
    var collectionID = Opt.none<String>()
    /** The format to use for temporal date properties.  */
    var pattern = Opt.none<String>()
    /** The delimiter used to display and parse list values.  */
    var delimiter = Opt.none<String>()
    /** The id of the field used to identify it in the dublin core.  */
    var inputID: String? = null
        private set
    /** The i18n id for the label to show the property.  */
    var label: String? = null
    /** The provider to populate the property with.  */
    var listprovider = Opt.none<String>()
    /** The optional namespace of the field used if a field can be found in more than one namespace  */
    var namespace = Opt.some(DublinCore.TERMS_NS_URI)
    /**
     * In the order of properties where this property should be oriented in the UI i.e. 0 means the property should come
     * first, 1 means it should come second etc.
     */
    var order = Opt.none<Int>()
    /** The optional id of the field used to output for the ui, if not present will assume the same as the inputID.  */
    private var outputID = Opt.none<String>()
    /** Whether the property should not be edited.  */
    var isReadOnly: Boolean = false
    /** Whether the property is required to update the metadata.  */
    var isRequired: Boolean = false
    /** The type of the metadata for example text, date etc.  */
    var type: Type? = null
    /** The type of the metadata for the json to use example text, date, time, number etc.  */
    var jsonType: JsonType? = null

    var value = Opt.none<A>()
        private set
    var isTranslatable = Opt.none<Boolean>()
    var isUpdated = false
        private set
    private var collection = Opt.none<Map<String, String>>()
    var valueToJSON: Fn<Opt<A>, JValue>? = null
    var jsonToValue: Fn<Any, A>? = null

    /**
     * Possible types for the metadata field. The types are used in the frontend and backend to know how the metadata
     * fields should be formatted (if needed).
     */
    enum class Type {
        BOOLEAN, DATE, DURATION, ITERABLE_TEXT, MIXED_TEXT, ORDERED_TEXT, LONG, START_DATE, START_TIME, TEXT, TEXT_LONG
    }

    enum class JsonType {
        BOOLEAN, DATE, NUMBER, TEXT, MIXED_TEXT, ORDERED_TEXT, TEXT_LONG, TIME
    }

    /**
     * Copy constructor
     *
     * @param other
     * Other metadata field
     */
    constructor(other: MetadataField<A>) {

        this.inputID = other.inputID
        this.outputID = other.outputID
        this.label = other.label
        this.isReadOnly = other.isReadOnly
        this.isRequired = other.isRequired
        this.value = other.value
        this.isTranslatable = other.isTranslatable
        this.type = other.type
        this.jsonType = other.jsonType
        this.collection = other.collection
        this.collectionID = other.collectionID
        this.valueToJSON = other.valueToJSON
        this.jsonToValue = other.jsonToValue
        this.order = other.order
        this.namespace = other.namespace
        this.isUpdated = other.isUpdated
        this.pattern = other.pattern
        this.delimiter = other.delimiter
        this.listprovider = other.listprovider
    }

    /**
     * Metadata field constructor
     *
     * @param inputID
     * The identifier of the new metadata field
     * @param label
     * the label of the field. The string displayed next to the field value on the frontend. This is usually be a
     * translation key
     * @param readOnly
     * Define if the new metadata field can be or not edited
     * @param required
     * Define if the new metadata field is or not required
     * @param value
     * The metadata field value
     * @param type
     * The metadata field type @ EventMetadata.Type}
     * @param collection
     * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
     * be none. This is also possible to use the collectionId parameter for that.
     * @param collectionID
     * The id of the limit list of possible value that should be get through the resource endpoint.
     * @param valueToJSON
     * Function to format the metadata field value to a JSON value.
     * @param jsonToValue
     * Function to parse the JSON value of the metadata field.
     * @throws IllegalArgumentException
     * if the id, label, type, valueToJSON or/and jsonToValue parameters is/are null
     */
    @Throws(IllegalArgumentException::class)
    private constructor(inputID: String, outputID: Opt<String>, label: String, readOnly: Boolean, required: Boolean, value: A?,
                        translatable: Opt<Boolean>, type: Type?, jsonType: JsonType, collection: Opt<Map<String, String>>,
                        collectionID: Opt<String>, valueToJSON: Fn<Opt<A>, JValue>?, jsonToValue: Fn<Any, A>?, order: Opt<Int>,
                        namespace: Opt<String>) {
        if (valueToJSON == null)
            throw IllegalArgumentException("The function 'valueToJSON' must not be null.")
        if (jsonToValue == null)
            throw IllegalArgumentException("The function 'jsonToValue' must not be null.")
        if (StringUtils.isBlank(inputID))
            throw IllegalArgumentException("The metadata input id must not be null.")
        if (StringUtils.isBlank(label))
            throw IllegalArgumentException("The metadata label must not be null.")
        if (type == null)
            throw IllegalArgumentException("The metadata type must not be null.")

        this.inputID = inputID
        this.outputID = outputID
        this.label = label
        this.isReadOnly = readOnly
        this.isRequired = required
        if (value == null)
            this.value = Opt.none()
        else
            this.value = Opt.some(value)
        this.isTranslatable = translatable
        this.type = type
        this.jsonType = jsonType
        this.collection = collection
        this.collectionID = collectionID
        this.valueToJSON = valueToJSON
        this.jsonToValue = jsonToValue
        this.order = order
        this.namespace = namespace
    }

    /**
     * Set the option of a limited list of possible values.
     *
     * @param collection
     * The option of a limited list of possible values
     */
    fun setCollection(collection: Opt<Map<String, String>>?) {
        if (collection == null)
            this.collection = Opt.none()
        else {
            this.collection = collection
        }
    }

    fun toJSON(): JObject {
        val values = HashMap<String, Field>()
        values[JSON_KEY_ID] = f(JSON_KEY_ID, v(getOutputID(), Jsons.BLANK))
        values[JSON_KEY_LABEL] = f(JSON_KEY_LABEL, v(label, Jsons.BLANK))
        values[JSON_KEY_VALUE] = f(JSON_KEY_VALUE, valueToJSON!!.apply(value))
        values[JSON_KEY_TYPE] = f(JSON_KEY_TYPE, v(jsonType!!.toString().toLowerCase(), Jsons.BLANK))
        values[JSON_KEY_READONLY] = f(JSON_KEY_READONLY, v(isReadOnly))
        values[JSON_KEY_REQUIRED] = f(JSON_KEY_REQUIRED, v(isRequired))

        if (collection.isSome)
            values[JSON_KEY_COLLECTION] = f(JSON_KEY_COLLECTION, mapToJSON(collection.get()))
        else if (collectionID.isSome)
            values[JSON_KEY_COLLECTION] = f(JSON_KEY_COLLECTION, v(collectionID.get()))
        if (isTranslatable.isSome)
            values[JSON_KEY_TRANSLATABLE] = f(JSON_KEY_TRANSLATABLE, v(isTranslatable.get()))
        if (delimiter.isSome)
            values[JSON_KEY_DELIMITER] = f(JSON_KEY_DELIMITER, v(delimiter.get()))
        return obj(values)
    }

    fun fromJSON(json: Any) {
        this.setValue(jsonToValue!!.apply(json))
    }

    fun getCollection(): Opt<Map<String, String>> {
        return collection
    }

    fun setValue(value: A?) {
        if (value == null)
            this.value = Opt.none()
        else {
            this.value = Opt.some(value)
            this.isUpdated = true
        }
    }

    fun setInputId(inputID: String) {
        this.inputID = inputID
    }

    /**
     * @return The outputID if available, inputID if it is missing.
     */
    fun getOutputID(): String? {
        return if (outputID.isSome) {
            outputID.get()
        } else {
            inputID
        }
    }

    fun setOutputID(outputID: Opt<String>) {
        this.outputID = outputID
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MetadataField<*>::class.java)

        val PATTERN_DURATION = "HH:mm:ss"

        /** Keys for the different values in the configuration file  */
        val CONFIG_COLLECTION_ID_KEY = "collectionID"
        val CONFIG_PATTERN_KEY = "pattern"
        val CONFIG_DELIMITER_KEY = "delimiter"
        val CONFIG_INPUT_ID_KEY = "inputID"
        val CONFIG_LABEL_KEY = "label"
        val CONFIG_LIST_PROVIDER_KEY = "listprovider"
        val CONFIG_NAMESPACE_KEY = "namespace"
        val CONFIG_ORDER_KEY = "order"
        val CONFIG_OUTPUT_ID_KEY = "outputID"
        val CONFIG_PROPERTY_PREFIX = "property"
        val CONFIG_READ_ONLY_KEY = "readOnly"
        val CONFIG_REQUIRED_KEY = "required"
        val CONFIG_TYPE_KEY = "type"

        /* Keys for the different properties of the metadata JSON Object */
        protected val JSON_KEY_ID = "id"
        protected val JSON_KEY_LABEL = "label"
        protected val JSON_KEY_READONLY = "readOnly"
        protected val JSON_KEY_REQUIRED = "required"
        protected val JSON_KEY_TYPE = "type"
        protected val JSON_KEY_VALUE = "value"
        protected val JSON_KEY_COLLECTION = "collection"
        protected val JSON_KEY_TRANSLATABLE = "translatable"
        protected val JSON_KEY_DELIMITER = "delimiter"

        /** Labels for the temporal date fields  */
        private val LABEL_METADATA_PREFIX = "EVENTS.EVENTS.DETAILS.METADATA."

        fun getSimpleDateFormatter(pattern: String): SimpleDateFormat {
            val dateFormat: SimpleDateFormat
            if (StringUtils.isNotBlank(pattern)) {
                dateFormat = SimpleDateFormat(pattern)
            } else {
                dateFormat = SimpleDateFormat()
            }
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return dateFormat
        }

        /**
         * Create a metadata field based on a [Boolean].
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata is or not a readonly field
         * @param required
         * Define if the new metadata field is or not required
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return The new metadata field
         */
        fun createBooleanMetadata(inputID: String, outputID: Opt<String>, label: String,
                                  readOnly: Boolean, required: Boolean, order: Opt<Int>, namespace: Opt<String>): MetadataField<Boolean> {

            val booleanToJson = object : Fn<Opt<Boolean>, JValue>() {
                override fun apply(value: Opt<Boolean>): JValue {
                    return if (value.isNone)
                        Jsons.BLANK
                    else {
                        v(value.get(), Jsons.BLANK)
                    }
                }
            }

            val jsonToBoolean = object : Fn<Any, Boolean>() {
                override fun apply(value: Any): Boolean? {
                    if (value is Boolean) {
                        return value
                    }
                    val stringValue = value.toString()
                    return if (StringUtils.isBlank(stringValue)) {
                        null
                    } else java.lang.Boolean.parseBoolean(stringValue)
                }
            }

            return MetadataField(inputID, outputID, label, readOnly, required, null, Opt.none(), Type.BOOLEAN, JsonType.BOOLEAN,
                    Opt.none(), Opt.none(), booleanToJson, jsonToBoolean, order, namespace)
        }

        /**
         * Creates a copy of a [MetadataField] and sets the value based upon a string.
         *
         * @param oldField
         * The field whose other values such as ids, label etc. will be copied.
         * @param value
         * The value that will be interpreted as being from a JSON value.
         * @return A new [MetadataField] with the value set
         */
        fun copyMetadataFieldWithValue(oldField: MetadataField<*>, value: String): MetadataField<*> {
            val newField = MetadataField(oldField)
            newField.fromJSON(value)
            return newField
        }

        /**
         * Create a metadata field based on a [Date].
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata is or not a readonly field
         * @param required
         * Define if the new metadata field is or not required
         * @param pattern
         * The date pattern for [SimpleDateFormat].
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return The new metadata field
         */
        fun createDateMetadata(inputID: String, outputID: Opt<String>, label: String,
                               readOnly: Boolean, required: Boolean, pattern: String, order: Opt<Int>, namespace: Opt<String>): MetadataField<Date> {
            val dateFormat = getSimpleDateFormatter(pattern)

            val dateToJSON = object : Fn<Opt<Date>, JValue>() {
                override fun apply(date: Opt<Date>): JValue {
                    return if (date.isNone)
                        Jsons.BLANK
                    else {
                        v(dateFormat.format(date.get()), Jsons.BLANK)
                    }
                }
            }

            val jsonToDate = object : Fn<Any, Date>() {
                override fun apply(value: Any): Date? {
                    try {
                        val date = value as String

                        return if (StringUtils.isBlank(date)) null else dateFormat.parse(date)

                    } catch (e: java.text.ParseException) {
                        logger.error("Not able to parse date {}: {}", value, e.message)
                        return null
                    }

                }
            }

            val dateField = MetadataField(inputID, outputID, label, readOnly, required, null, Opt.none(),
                    Type.DATE, JsonType.DATE, Opt.none(), Opt.none(), dateToJSON, jsonToDate,
                    order, namespace)
            if (StringUtils.isNotBlank(pattern)) {
                dateField.pattern = Opt.some(pattern)
            }
            return dateField
        }

        fun createDurationMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                        readOnly: Boolean, required: Boolean, order: Opt<Int>, namespace: Opt<String>): MetadataField<String> {
            return createDurationMetadataField(inputID, outputID, label, readOnly, required, Opt.none(),
                    Opt.none(), Opt.none(), order, namespace)
        }

        fun createDurationMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                        readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>, collection: Opt<Map<String, String>>,
                                        collectionId: Opt<String>, order: Opt<Int>, namespace: Opt<String>): MetadataField<String> {

            val periodToJSON = object : Fn<Opt<String>, JValue>() {
                override fun apply(value: Opt<String>): JValue {
                    var returnValue: Long? = 0L
                    val period = EncodingSchemeUtils.decodePeriod(value.get())
                    if (period != null && period.hasStart() && period.hasEnd()) {
                        returnValue = period.end!!.time - period.start!!.time
                    } else {
                        try {
                            returnValue = java.lang.Long.parseLong(value.get())
                        } catch (e: NumberFormatException) {
                            logger.debug("Unable to parse duration '{}' as either period or millisecond duration.", value.get())
                        }

                    }
                    return v(DurationFormatUtils.formatDuration(returnValue!!, PATTERN_DURATION))
                }
            }

            val jsonToPeriod = object : Fn<Any, String>() {
                override fun apply(value: Any): String? {
                    if (value !is String) {
                        logger.warn("The given value for duration can not be parsed.")
                        return ""
                    }

                    val durationParts = value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (durationParts.size < 3)
                        return null
                    val hours = Integer.parseInt(durationParts[0])
                    val minutes = Integer.parseInt(durationParts[1])
                    val seconds = Integer.parseInt(durationParts[2])

                    val returnValue = ((hours.toLong() * 60 + minutes.toLong()) * 60 + seconds.toLong()) * 1000

                    return returnValue.toString()
                }
            }
            return MetadataField(inputID, outputID, label, readOnly, required, "", isTranslatable, Type.DURATION,
                    JsonType.TEXT, collection, collectionId, periodToJSON, jsonToPeriod, order, namespace)
        }

        /**
         * Create a metadata field of type mixed iterable String
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return the new metadata field
         */
        fun createMixedIterableStringMetadataField(inputID: String,
                                                   outputID: Opt<String>, label: String, readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>,
                                                   collection: Opt<Map<String, String>>, collectionId: Opt<String>, delimiter: Opt<String>, order: Opt<Int>,
                                                   namespace: Opt<String>): MetadataField<Iterable<String>> {

            val iterableToJSON = object : Fn<Opt<Iterable<String>>, JValue>() {
                override fun apply(value: Opt<Iterable<String>>): JValue {
                    if (value.isNone)
                        return arr()

                    val `val` = value.get()
                    val list = ArrayList<JValue>()

                    if (`val` is String) {
                        // The value is a string so we need to split it.
                        val stringVal = `val` as String
                        for (entry in stringVal.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                            if (StringUtils.isNotBlank(entry))
                                list.add(v(entry, Jsons.BLANK))
                        }
                    } else {
                        // The current value is just an iterable string.
                        for (v in value.get()) {
                            list.add(v(v, Jsons.BLANK))
                        }
                    }
                    return arr(list)
                }
            }

            val jsonToIterable = object : Fn<Any, Iterable<String>>() {
                override fun apply(arrayIn: Any): Iterable<String> {
                    val parser = JSONParser()
                    val array: JSONArray?
                    if (arrayIn is String) {
                        try {
                            array = parser.parse(arrayIn) as JSONArray
                        } catch (e: ParseException) {
                            throw IllegalArgumentException("Unable to parse Mixed Iterable value into a JSONArray:", e)
                        }

                    } else {
                        array = arrayIn as JSONArray
                    }

                    if (array == null)
                        return ArrayList()
                    val arrayOut = arrayOfNulls<String>(array.size)
                    for (i in array.indices) {
                        arrayOut[i] = array[i] as String
                    }
                    return Arrays.asList<String>(*arrayOut)
                }

            }

            val mixedField = MetadataField(inputID, outputID, label, readOnly, required,
                    ArrayList(), isTranslatable, Type.MIXED_TEXT, JsonType.MIXED_TEXT, collection, collectionId,
                    iterableToJSON, jsonToIterable, order, namespace)
            mixedField.delimiter = delimiter
            return mixedField
        }

        /**
         * Create a metadata field of type iterable String
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return the new metadata field
         */
        fun createIterableStringMetadataField(inputID: String, outputID: Opt<String>,
                                              label: String, readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>,
                                              collection: Opt<Map<String, String>>, collectionId: Opt<String>, delimiter: Opt<String>, order: Opt<Int>,
                                              namespace: Opt<String>): MetadataField<Iterable<String>> {

            val iterableToJSON = object : Fn<Opt<Iterable<String>>, JValue>() {
                override fun apply(value: Opt<Iterable<String>>): JValue {
                    if (value.isNone)
                        return arr()

                    val `val` = value.get()
                    val list = ArrayList<JValue>()

                    if (`val` is String) {
                        // The value is a string so we need to split it.
                        val stringVal = `val` as String
                        for (entry in stringVal.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                            list.add(v(entry, Jsons.BLANK))
                        }
                    } else {
                        // The current value is just an iterable string.
                        for (v in value.get()) {
                            list.add(v(v, Jsons.BLANK))
                        }
                    }
                    return arr(list)
                }
            }

            val jsonToIterable = object : Fn<Any, Iterable<String>>() {
                override fun apply(arrayIn: Any): Iterable<String>? {
                    val array = arrayIn as JSONArray ?: return null
                    val arrayOut = arrayOfNulls<String>(array.size)
                    for (i in array.indices) {
                        arrayOut[i] = array[i] as String
                    }
                    return Arrays.asList<String>(*arrayOut)
                }

            }

            val iterableField = MetadataField(inputID, outputID, label, readOnly, required,
                    ArrayList(), isTranslatable, Type.ITERABLE_TEXT, JsonType.TEXT, collection, collectionId,
                    iterableToJSON, jsonToIterable, order, namespace)
            iterableField.delimiter = delimiter
            return iterableField
        }

        fun createLongMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                    readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>, collection: Opt<Map<String, String>>,
                                    collectionId: Opt<String>, order: Opt<Int>, namespace: Opt<String>): MetadataField<Long> {

            val longToJSON = object : Fn<Opt<Long>, JValue>() {
                override fun apply(value: Opt<Long>): JValue {
                    return if (value.isNone)
                        Jsons.BLANK
                    else
                        v(value.get().toString())
                }
            }

            val jsonToLong = object : Fn<Any, Long>() {
                override fun apply(value: Any): Long {
                    if (value !is String) {
                        logger.warn("The given value for Long can not be parsed.")
                        return 0L
                    }
                    return java.lang.Long.parseLong(value)
                }
            }

            return MetadataField(inputID, outputID, label, readOnly, required, 0L, isTranslatable, Type.TEXT, JsonType.NUMBER,
                    collection, collectionId, longToJSON, jsonToLong, order, namespace)
        }

        private fun createTemporalMetadata(inputID: String, outputID: Opt<String>, label: String,
                                           readOnly: Boolean, required: Boolean, pattern: String, type: Type, jsonType: JsonType,
                                           order: Opt<Int>, namespace: Opt<String>): MetadataField<String> {
            if (StringUtils.isBlank(pattern)) {
                throw IllegalArgumentException(
                        "For temporal metadata field $inputID of type $type there needs to be a pattern.")
            }

            val dateFormat = getSimpleDateFormatter(pattern)

            val jsonToDateString = object : Fn<Any, String>() {
                override fun apply(value: Any): String? {
                    val date = value as String

                    if (StringUtils.isBlank(date))
                        return ""

                    try {
                        dateFormat.parse(date)
                    } catch (e: java.text.ParseException) {
                        logger.error("Not able to parse date string {}: {}", value, getMessage(e))
                        return null
                    }

                    return date
                }
            }

            val dateToJSON = object : Fn<Opt<String>, JValue>() {
                override fun apply(periodEncodedString: Opt<String>): JValue {
                    if (periodEncodedString.isNone || StringUtils.isBlank(periodEncodedString.get())) {
                        return Jsons.BLANK
                    }

                    // Try to parse the metadata as DCIM metadata.
                    val p = EncodingSchemeUtils.decodePeriod(periodEncodedString.get())
                    if (p != null) {
                        return v(dateFormat.format(p.start), Jsons.BLANK)
                    }

                    // Not DCIM metadata so it might already be formatted (given from the front and is being returned there
                    try {
                        dateFormat.parse(periodEncodedString.get())
                        return v(periodEncodedString.get(), Jsons.BLANK)
                    } catch (e: Exception) {
                        logger.error(
                                "Unable to parse temporal metadata '{}' as either DCIM data or a formatted date using pattern {} because: {}",
                                periodEncodedString.get(), pattern, getStackTrace(e))
                        throw IllegalArgumentException(e)
                    }

                }
            }

            val temporalStart = MetadataField(inputID, outputID, label, readOnly, required, null,
                    Opt.none(), type, jsonType, Opt.none(), Opt.none(), dateToJSON,
                    jsonToDateString, order, namespace)
            temporalStart.pattern = Opt.some(pattern)

            return temporalStart
        }

        fun createTemporalStartDateMetadata(inputID: String, outputID: Opt<String>,
                                            label: String, readOnly: Boolean, required: Boolean, pattern: String, order: Opt<Int>,
                                            namespace: Opt<String>): MetadataField<String> {
            return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_DATE,
                    JsonType.DATE, order, namespace)
        }

        fun createTemporalStartTimeMetadata(inputID: String, outputID: Opt<String>,
                                            label: String, readOnly: Boolean, required: Boolean, pattern: String, order: Opt<Int>,
                                            namespace: Opt<String>): MetadataField<String> {
            return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_TIME,
                    JsonType.TIME, order, namespace)
        }

        /**
         * Create a metadata field of type String with a single line in the front end.
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return the new metadata field
         */
        fun createTextMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                    readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>, collection: Opt<Map<String, String>>,
                                    collectionId: Opt<String>, order: Opt<Int>, namespace: Opt<String>): MetadataField<String> {
            return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
                    collectionId, order, Type.TEXT, JsonType.TEXT, namespace)
        }

        /**
         * Create a metadata field of type String with a single line in the front end which can be ordered and filtered.
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return the new metadata field
         */
        fun createOrderedTextMetadataField(
                inputID: String,
                outputID: Opt<String>,
                label: String,
                readOnly: Boolean,
                required: Boolean,
                isTranslatable: Opt<Boolean>,
                collection: Opt<Map<String, String>>,
                collectionId: Opt<String>,
                order: Opt<Int>,
                namespace: Opt<String>): MetadataField<String> {
            return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
                    collectionId, order, Type.ORDERED_TEXT, JsonType.ORDERED_TEXT, namespace)
        }


        /**
         * Create a metadata field of type String with many lines in the front end.
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @return the new metadata field
         */
        fun createTextLongMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                        readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>, collection: Opt<Map<String, String>>,
                                        collectionId: Opt<String>, order: Opt<Int>, namespace: Opt<String>): MetadataField<String> {
            return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
                    collectionId, order, Type.TEXT_LONG, JsonType.TEXT_LONG, namespace)
        }

        /**
         * Create a metadata field of type String specifying the type for the front end.
         *
         * @param inputID
         * The identifier of the new metadata field
         * @param label
         * The label of the new metadata field
         * @param readOnly
         * Define if the new metadata field can be or not edited
         * @param required
         * Define if the new metadata field is or not required
         * @param isTranslatable
         * If the field value is not human readable and should be translated before
         * @param collection
         * If the field has a limited list of possible value, the option should contain this one. Otherwise it should
         * be none.
         * @param order
         * The ui order for the new field, 0 at the top and progressively down from there.
         * @param type
         * The metadata field type as defined in [MetadataField.Type]
         * @return the new metadata field
         */
        private fun createTextAnyMetadataField(inputID: String, outputID: Opt<String>, label: String,
                                               readOnly: Boolean, required: Boolean, isTranslatable: Opt<Boolean>, collection: Opt<Map<String, String>>,
                                               collectionId: Opt<String>, order: Opt<Int>, type: Type, jsonType: JsonType, namespace: Opt<String>): MetadataField<String> {

            val stringToJSON = object : Fn<Opt<String>, JValue>() {
                override fun apply(value: Opt<String>): JValue {
                    return v(value.getOr(""))
                }
            }

            val jsonToString = object : Fn<Any, String>() {
                override fun apply(jsonValue: Any?): String? {
                    if (jsonValue == null)
                        return ""
                    if (jsonValue !is String) {
                        logger.warn("Value cannot be parsed as String. Expecting type 'String', but received type '{}'.", jsonValue.javaClass.name)
                        return null
                    }
                    return jsonValue
                }
            }

            return MetadataField(inputID, outputID, label, readOnly, required, "", isTranslatable, type, jsonType,
                    collection, collectionId, stringToJSON, jsonToString, order, namespace)
        }

        /**
         * Turn a map into a [JObject] object
         *
         * @param map
         * the source map
         * @return a new [JObject] generated with the map values
         */
        fun mapToJSON(map: Map<String, String>?): JObject {
            if (map == null) {
                throw IllegalArgumentException("Map must not be null!")
            }

            val fields = ArrayList<Field>()
            for ((key, value) in map) {
                fields.add(f(key, v(value, Jsons.BLANK)))
            }
            return obj(fields)
        }

        fun createMetadataField(configuration: Map<String, String>): MetadataField<*> {

            val inputID = configuration[CONFIG_INPUT_ID_KEY]
            val label = configuration[CONFIG_LABEL_KEY]

            val collectionID = Opt.nul(configuration[CONFIG_COLLECTION_ID_KEY])
            val delimiter = Opt.nul(configuration[CONFIG_DELIMITER_KEY])
            val outputID = Opt.nul(configuration[CONFIG_OUTPUT_ID_KEY])
            val listprovider = Opt.nul(configuration[CONFIG_LIST_PROVIDER_KEY])
            val namespace = Opt.nul(configuration[CONFIG_NAMESPACE_KEY])

            val type = if (configuration.containsKey(CONFIG_TYPE_KEY))
                Type.valueOf(configuration[CONFIG_TYPE_KEY].toUpperCase())
            else
                null
            val required = if (configuration.containsKey(CONFIG_REQUIRED_KEY))
                java.lang.Boolean.valueOf(configuration[CONFIG_REQUIRED_KEY].toUpperCase())
            else
                null
            val readOnly = if (configuration.containsKey(CONFIG_READ_ONLY_KEY))
                java.lang.Boolean.valueOf(configuration[CONFIG_READ_ONLY_KEY].toUpperCase())
            else
                null

            val pattern = if (configuration.containsKey(CONFIG_PATTERN_KEY))
                configuration[CONFIG_PATTERN_KEY]
            else
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

            var order = Opt.none<Int>()
            if (configuration.containsKey(CONFIG_ORDER_KEY)) {
                try {
                    order = Opt.some(Integer.parseInt(configuration[CONFIG_ORDER_KEY]))
                } catch (e: NumberFormatException) {
                    logger.warn("Unable to parse order value {} of metadata field {}", configuration[CONFIG_ORDER_KEY],
                            inputID, e)
                }

            }

            val metadataField = createMetadataField(inputID, outputID, label, readOnly!!, required!!, Opt.none(), type!!,
                    Opt.none(), collectionID, order, namespace, delimiter, pattern)
            metadataField.listprovider = listprovider
            return metadataField
        }

        fun createMetadataField(inputID: String, outputID: Opt<String>, label: String, readOnly: Boolean,
                                required: Boolean, translatable: Opt<Boolean>, type: Type, collection: Opt<Map<String, String>>,
                                collectionID: Opt<String>, order: Opt<Int>, namespace: Opt<String>, delimiter: Opt<String>, pattern: String): MetadataField<*> {

            when (type) {
                MetadataField.Type.BOOLEAN -> return createBooleanMetadata(inputID, outputID, label, readOnly, required, order, namespace)
                MetadataField.Type.DATE -> return createDateMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace)
                MetadataField.Type.DURATION -> return createDurationMetadataField(inputID, outputID, label, readOnly, required, translatable,
                        collection, collectionID, order, namespace)
                MetadataField.Type.ITERABLE_TEXT -> return createIterableStringMetadataField(inputID, outputID, label, readOnly, required, translatable,
                        collection, collectionID, delimiter, order, namespace)
                MetadataField.Type.MIXED_TEXT -> return createMixedIterableStringMetadataField(inputID, outputID, label, readOnly, required,
                        translatable, collection, collectionID, delimiter, order, namespace)
                MetadataField.Type.LONG -> return createLongMetadataField(inputID, outputID, label, readOnly, required, translatable,
                        collection, collectionID, order, namespace)
                MetadataField.Type.TEXT -> return createTextMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                        collectionID, order, namespace)
                MetadataField.Type.TEXT_LONG -> return createTextLongMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                        collectionID, order, namespace)
                MetadataField.Type.START_DATE -> return createTemporalStartDateMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace)
                MetadataField.Type.START_TIME -> return createTemporalStartTimeMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace)
                MetadataField.Type.ORDERED_TEXT -> return createOrderedTextMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                        collectionID, order, namespace)
                else -> throw IllegalArgumentException("Unknown metadata type! $type")
            }
        }
    }
}
