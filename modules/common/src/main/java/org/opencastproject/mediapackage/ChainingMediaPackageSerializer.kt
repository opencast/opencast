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

import com.entwinemedia.fn.Prelude.chuck
import com.entwinemedia.fn.Stream.`$`
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace

import com.entwinemedia.fn.Fn2

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

/**
 * This class was created to allow more than one [MediaPackageSerializer] to be applied to the same
 * [MediaPackage]. For example if you enabled a redirect serializer to move urls from an old server to a new one
 * and a stream security serializer then the urls could be redirected and then signed.
 */
class ChainingMediaPackageSerializer : MediaPackageSerializer {

    /** List of serializers ordered by their ranking  */
    private val serializers = ArrayList<MediaPackageSerializer>()

    override val ranking: Int
        get() = RANKING

    /** OSGi DI  */
    internal fun addMediaPackageSerializer(serializer: MediaPackageSerializer) {
        serializers.add(serializer)
        Collections.sort(serializers) { o1, o2 -> o1.ranking - o2.ranking }
        logger.info("MediaPackageSerializer '{}' with ranking {} added to serializer chain.", serializer,
                serializer.ranking)
    }

    /** OSGi DI  */
    internal fun removeMediaPackageSerializer(serializer: MediaPackageSerializer) {
        serializers.remove(serializer)
        logger.info("MediaPackageSerializer '{}' with ranking {} removed from serializer chain.", serializer,
                serializer.ranking)
    }

    @Throws(URISyntaxException::class)
    override fun encodeURI(uri: URI): URI {
        return `$`(serializers).reverse().foldl(uri, object : Fn2<URI, MediaPackageSerializer, URI>() {
            override fun apply(uri: URI, serializer: MediaPackageSerializer): URI {
                try {
                    return serializer.encodeURI(uri)
                } catch (e: URISyntaxException) {
                    logger.warn("Error while encoding URI with serializer '{}': {}", serializer, getStackTrace(e))
                    return chuck(e)
                }

            }
        })
    }

    @Throws(URISyntaxException::class)
    override fun decodeURI(uri: URI): URI {
        return `$`(serializers).foldl(uri, object : Fn2<URI, MediaPackageSerializer, URI>() {
            override fun apply(uri: URI, serializer: MediaPackageSerializer): URI {
                try {
                    return serializer.decodeURI(uri)
                } catch (e: URISyntaxException) {
                    logger.warn("Error while encoding URI with serializer '{}': {}", serializer, getStackTrace(e))
                    return chuck(e)
                }

            }
        })
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ChainingMediaPackageSerializer::class.java!!)

        /** This serializer should never be chained again and zero as a neutral ranking therefore seems to be appropriate  */
        val RANKING = 0
    }

}
