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

import org.opencastproject.util.PathSupport.path
import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClientException
import org.opencastproject.util.data.Effect0
import org.opencastproject.util.data.Effect2
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function0
import org.opencastproject.util.data.Function2
import org.opencastproject.util.data.Option

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.FnX
import com.entwinemedia.fn.Prelude
import com.entwinemedia.fn.Unit
import com.google.common.io.Resources

import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.io.Serializable
import java.net.URISyntaxException
import java.net.URL
import java.nio.channels.FileLock
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Properties

import de.schlichtherle.io.FileWriter

/**
 * Contains operations concerning IO.
 */
object IoSupport {

    /**
     * the logging facility provided by log4j
     */
    private val logger = LoggerFactory.getLogger(IoSupport::class.java!!.getName())

    val systemTmpDir: String
        get() {
            var tmpdir: String? = System.getProperty("java.io.tmpdir")
            if (tmpdir == null) {
                tmpdir = File.separator + "tmp" + File.separator
            } else {
                if (!tmpdir.endsWith(File.separator)) {
                    tmpdir += File.separator
                }
            }
            return tmpdir
        }

    /** Function that reads an input stream into a string using utf-8 encoding. Stream does not get closed.  */
    val readToString: Function<InputStream, String> = object : Function.X<InputStream, String>() {
        @Throws(IOException::class)
        public override fun xapply(`in`: InputStream): String {
            return IOUtils.toString(`in`, "utf-8")
        }
    }

    /**
     * Closes a `Closable` quietly so that no exceptions are thrown.
     *
     * @param s
     * maybe null
     */
    fun closeQuietly(s: Closeable?): Boolean {
        if (s == null) {
            return false
        }
        try {
            s.close()
            return true
        } catch (e: IOException) {
            return false
        }

    }

    /**
     * Closes a `StreamHelper` quietly so that no exceptions are thrown.
     *
     * @param s
     * maybe null
     */
    fun closeQuietly(s: StreamHelper?): Boolean {
        if (s == null) {
            return false
        }
        try {
            s.stopReading()
        } catch (e: InterruptedException) {
            logger.warn("Interrupted while waiting for stream helper to stop reading")
        }

        return true
    }

    /**
     * Closes the processes input, output and error streams.
     *
     * @param process
     * the process
     * @return `true` if the streams were closed
     */
    fun closeQuietly(process: Process?): Boolean {
        if (process != null) {
            closeQuietly(process.inputStream)
            closeQuietly(process.errorStream)
            closeQuietly(process.outputStream)
            return true
        }
        return false
    }

    /**
     * Extracts the content from the given input stream. This method is intended to faciliate handling of processes that
     * have error, input and output streams.
     *
     * @param is
     * the input stream
     * @return the stream content
     */
    fun getOutput(`is`: InputStream): String {
        val bis = InputStreamReader(`is`)
        val outputMsg = StringBuffer()
        val chars = CharArray(1024)
        try {
            var len = 0
            try {
                while ((len = bis.read(chars)) > 0) {
                    outputMsg.append(chars, 0, len)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } finally {
            if (bis != null)
                try {
                    bis.close()
                } catch (e: IOException) {
                }

        }
        return outputMsg.toString()
    }

    /**
     * Writes the contents variable to the `URL`. Note that the URL must be a local `URL`.
     *
     * @param file
     * The `URL` of the local file you wish to write to.
     * @param contents
     * The contents of the file you wish to create.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeUTF8File(file: URL, contents: String) {
        try {
            writeUTF8File(File(file.toURI()), contents)
        } catch (e: URISyntaxException) {
            throw IOException("Couldn't parse the URL", e)
        }

    }

    /**
     * Writes the contents variable to the `File`.
     *
     * @param file
     * The `File` of the local file you wish to write to.
     * @param contents
     * The contents of the file you wish to create.
     */
    @Throws(IOException::class)
    fun writeUTF8File(file: File, contents: String) {
        writeUTF8File(file.absolutePath, contents)
    }

    /**
     * Writes the contents variable to the `File` located at the filename.
     *
     * @param filename
     * The `File` of the local file you wish to write to.
     * @param contents
     * The contents of the file you wish to create.
     */
    @Throws(IOException::class)
    fun writeUTF8File(filename: String, contents: String) {
        val out = FileWriter(filename)
        try {
            out.write(contents)
        } finally {
            closeQuietly(out)
        }
    }

    /**
     * Convenience method to read in a file from either a remote or local source.
     *
     * @param url
     * The `URL` to read the source data from.
     * @param trustedClient
     * The `TrustedHttpClient` which should be used to communicate with the remote server. This can be null
     * for local file reads.
     * @return A String containing the source data or null in the case of an error.
     */
    @Deprecated("this method doesn't support UTF8 or handle HTTP response codes")
    @JvmOverloads
    fun readFileFromURL(url: URL, trustedClient: TrustedHttpClient? = null): String? {
        val sb = StringBuilder()
        var `in`: DataInputStream? = null
        var response: HttpResponse? = null
        try {
            // Do different things depending on what we're reading...
            if ("file" == url.protocol) {
                `in` = DataInputStream(url.openStream())
            } else {
                if (trustedClient == null) {
                    logger.error("Unable to read from remote source {} because trusted client is null!", url.file)
                    return null
                }
                val get = HttpGet(url.toURI())
                try {
                    response = trustedClient.execute(get)
                } catch (e: TrustedHttpClientException) {
                    logger.warn("Unable to fetch file from {}.", url, e)
                    trustedClient.close(response)
                    return null
                }

                `in` = DataInputStream(response!!.entity.content)
            }
            var c = 0
            while ((c = `in`.read()) != -1) {
                sb.append(c.toChar())
            }
        } catch (e: IOException) {
            logger.warn("IOException attempting to get file from {}.", url)
            return null
        } catch (e: URISyntaxException) {
            logger.warn("URI error attempting to get file from {}.", url)
            return null
        } catch (e: NullPointerException) {
            logger.warn("Nullpointer attempting to get file from {}.", url)
            return null
        } finally {
            IOUtils.closeQuietly(`in`)

            if (response != null && trustedClient != null) {
                trustedClient.close(response)
                response = null
            }
        }

        return sb.toString()
    }

    fun loadPropertiesFromFile(path: String): Properties {
        try {
            return loadPropertiesFromStream(FileInputStream(path))
        } catch (e: FileNotFoundException) {
            return chuck(e)
        }

    }

    fun loadPropertiesFromUrl(url: URL): Properties {
        try {
            return loadPropertiesFromStream(url.openStream())
        } catch (e: IOException) {
            return chuck(e)
        }

    }

    /** Load properties from a stream. Close the stream after reading.  */
    fun loadPropertiesFromStream(stream: InputStream): Properties {
        return withResource(stream, object : Function.X<InputStream, Properties>() {
            @Throws(Exception::class)
            public override fun xapply(`in`: InputStream): Properties {
                val p = Properties()
                p.load(`in`)
                return p
            }
        })
    }

    /** Load a properties file from the classpath using the class loader of the given class.  */
    @JvmOverloads
    fun loadPropertiesFromClassPath(resource: String, clazz: Class<*> = IoSupport::class.java): Properties {
        for (`in` in openClassPathResource(resource, clazz)) {
            return withResource(`in`, object : Function<InputStream, Properties>() {
                override fun apply(`is`: InputStream): Properties {
                    val p = Properties()
                    try {
                        p.load(`is`)
                    } catch (e: Exception) {
                        throw Error("Cannot load resource $resource@$clazz")
                    }

                    return p
                }
            })
        }
        return chuck(FileNotFoundException("$resource does not exist"))
    }

    /** Load a text file from the class path using the class loader of the given class.  */
    fun loadTxtFromClassPath(resource: String, clazz: Class<*>): Option<String> {
        return withResource(clazz.getResourceAsStream(resource), object : Function<InputStream, Option<String>>() {
            override fun apply(`is`: InputStream): Option<String> {
                try {
                    return some(IOUtils.toString(`is`))
                } catch (e: Exception) {
                    logger.warn("Cannot load resource $resource from classpath")
                    return none()
                }

            }
        })
    }

    /**
     * Handle a stream inside `f` and ensure that `s` gets closed properly.
     *
     */
    @Deprecated("use {@link #withResource(java.io.Closeable, org.opencastproject.util.data.Function)} instead")
    fun <A> withStream(s: InputStream, f: Function<InputStream, A>): A {
        try {
            return f.apply(s)
        } finally {
            IoSupport.closeQuietly(s)
        }
    }

    /**
     * Handle a closeable resource inside `f` and ensure it gets closed properly.
     */
    fun <A, B : Closeable> withResource(b: B, f: Function<B, A>): A {
        try {
            return f.apply(b)
        } finally {
            IoSupport.closeQuietly(b)
        }
    }

    /**
     * Handle a closeable resource inside `f` and ensure it gets closed properly.
     */
    fun <A, B : Closeable> withResource(b: B, f: Fn<in B, out A>): A {
        try {
            return f.apply(b)
        } finally {
            IoSupport.closeQuietly(b)
        }
    }

    /**
     * Open a classpath resource using the class loader of the given class.
     *
     * @return an input stream to the resource wrapped in a Some or none if the resource cannot be found
     */
    @JvmOverloads
    fun openClassPathResource(resource: String, clazz: Class<*> = IoSupport::class.java): Option<InputStream> {
        return option(clazz.getResourceAsStream(resource))
    }

    /** Get a classpath resource as a file using the class loader of [IoSupport].  */
    fun classPathResourceAsFile(resource: String): Option<File> {
        try {
            val res = IoSupport::class.java!!.getResource(resource)
            return if (res != null) {
                Option.some(File(res!!.toURI()))
            } else {
                Option.none()
            }
        } catch (e: URISyntaxException) {
            return Option.none()
        }

    }

    /**
     * Load a classpath resource into a string using UTF-8 encoding and the class loader of the given class.
     *
     * @return the content of the resource wrapped in a Some or none in case of any error
     */
    @JvmOverloads
    fun loadFileFromClassPathAsString(resource: String, clazz: Class<*> = IoSupport::class.java): Option<String> {
        try {
            val url = clazz.getResource(resource)
            return if (url != null)
                some(Resources.toString(clazz.getResource(resource), Charset.forName("UTF-8")))
            else
                none(String::class.java)
        } catch (e: IOException) {
            return none()
        }

    }

    /**
     * Handle a stream inside `f` and ensure that `s` gets closed properly.
     *
     *
     * **Please note:** The outcome of `f` is wrapped into a some. Therefore `f` is
     * *not* allowed to return `null`. Use an `Option` instead and flatten the overall
     * result.
     *
     * @return none, if the file does not exist
     */
    fun <A> withFile(file: File, f: Function2<InputStream, File, A>): Option<A> {
        var s: InputStream? = null
        try {
            s = FileInputStream(file)
            return some(f.apply(s, file))
        } catch (ignore: FileNotFoundException) {
            return none()
        } finally {
            IoSupport.closeQuietly(s)
        }
    }

    /**
     * Handle a stream inside `e` and ensure that `s` gets closed properly.
     *
     * @return true, if the file exists, false otherwise
     */
    fun withFile(file: File, e: Effect2<OutputStream, File>): Boolean {
        var s: OutputStream? = null
        try {
            s = FileOutputStream(file)
            e.apply(s, file)
            return true
        } catch (ignore: FileNotFoundException) {
            return false
        } finally {
            IoSupport.closeQuietly(s)
        }
    }

    /**
     * Handle a stream inside `f` and ensure that `s` gets closed properly.
     *
     * @param s
     * the stream creation function
     * @param toErr
     * error handler transforming an exception into something else
     * @param f
     * stream handler
     */
    @Deprecated("use\n" +
            "                {@link #withResource(org.opencastproject.util.data.Function0, org.opencastproject.util.data.Function, org.opencastproject.util.data.Function)}\n" +
            "                instead")
    fun <A, Err> withStream(s: Function0<InputStream>, toErr: Function<Exception, Err>,
                            f: Function<InputStream, A>): Either<Err, A> {
        var `in`: InputStream? = null
        try {
            `in` = s.apply()
            return right(f.apply(`in`))
        } catch (e: Exception) {
            return left(toErr.apply(e))
        } finally {
            IoSupport.closeQuietly(`in`)
        }
    }

    /**
     * Handle a closeable resource inside `f` and ensure that `r` gets closed properly.
     *
     * @param r
     * resource creation function
     * @param toErr
     * error handler transforming an exception into something else
     * @param f
     * resource handler
     */
    fun <A, Err, B : Closeable> withResource(r: Function0<B>,
                                             toErr: Function<Exception, Err>, f: Function<B, A>): Either<Err, A> {
        var b: B? = null
        try {
            b = r.apply()
            return right(f.apply(b))
        } catch (e: Exception) {
            return left(toErr.apply(e))
        } finally {
            IoSupport.closeQuietly(b)
        }
    }

    /**
     * Handle a stream inside `f` and ensure that `s` gets closed properly.
     *
     */
    @Deprecated("use {@link #withResource(java.io.Closeable, org.opencastproject.util.data.Function)} instead")
    fun <A> withStream(s: OutputStream, f: Function<OutputStream, A>): A {
        try {
            return f.apply(s)
        } finally {
            IoSupport.closeQuietly(s)
        }
    }

    /** Handle multiple streams inside `f` and ensure that they get closed properly.  */
    fun <A> withStreams(`in`: Array<InputStream>, out: Array<OutputStream>, f: Function2<Array<InputStream>, Array<OutputStream>, A>): A {
        try {
            return f.apply(`in`, out)
        } finally {
            for (a in `in`) {
                IoSupport.closeQuietly(a)
            }
            for (a in out) {
                IoSupport.closeQuietly(a)
            }
        }
    }

    /** Like [IOUtils.toString] but without checked exception.  */
    fun readToString(url: URL, encoding: String): String {
        try {
            return IOUtils.toString(url, encoding)
        } catch (e: IOException) {
            return chuck(e)
        }

    }

    /** Wrap function `f` to close the input stream after usage.  */
    fun <A> closeAfterwards(f: Function<InputStream, out A>): Function<InputStream, A> {
        return object : Function<InputStream, A>() {
            override fun apply(`in`: InputStream): A {
                val a = f.apply(`in`)
                IOUtils.closeQuietly(`in`)
                return a
            }
        }
    }

    /** Create a function that creates a [java.io.FileInputStream].  */
    fun fileInputStream(a: File): Function0<InputStream> {
        return object : Function0.X<InputStream>() {
            @Throws(Exception::class)
            override fun xapply(): InputStream {
                return FileInputStream(a)
            }
        }
    }

    /** Create a file from the list of path elements.  */
    fun file(vararg pathElems: String): File {
        return File(path(*pathElems))
    }

    /** Returns the given [File] back when it's ready for reading  */
    fun waitForFile(file: File): File {
        while (!Files.isReadable(file.toPath())) {
            Prelude.sleep(100L)
        }
        return file
    }

    /**
     * Run function `f` having exclusive read/write access to the given file.
     *
     *
     * Please note that the implementation uses Java NIO [java.nio.channels.FileLock] which only guarantees that two
     * Java processes cannot interfere with each other.
     *
     *
     * The implementation blocks until a lock can be acquired.
     *
     * @throws NotFoundException
     * if the path to the file, to create a lock for, does not exist
     * @throws IOException
     * if the file lock can not be created due to access limitations
     */
    @Synchronized
    @Throws(NotFoundException::class, IOException::class)
    fun <A> locked(file: File, f: Function<File, A>): A {
        val key = acquireLock(file)
        try {
            return f.apply(file)
        } finally {
            key.apply()
        }
    }

    /**
     * Acquire a lock on a file. Return a key to release the lock.
     *
     * @return a key to release the lock
     *
     * @throws NotFoundException
     * if the path to the file, to create a lock for, does not exist
     * @throws IOException
     * if the file lock can not be created due to access limitations
     */
    @Throws(NotFoundException::class, IOException::class)
    private fun acquireLock(file: File): Effect0 {
        val raf: RandomAccessFile
        try {
            raf = RandomAccessFile(file, "rw")
        } catch (e: FileNotFoundException) {
            // this exception is thrown only if the directory path to the file isn't exist
            // make sure to create all parent directories before locking the file
            throw NotFoundException("Error acquiring lock for " + file.absolutePath, e)
        }

        val lock = raf.channel.lock()
        return object : Effect0() {
            override fun run() {
                try {
                    lock.release()
                } catch (ignore: IOException) {
                }

                IoSupport.closeQuietly(raf)
            }
        }
    }

    /**
     * Serialize and deserialize an object. To test serializability.
     */
    fun <A : Serializable> serializeDeserialize(a: A): A {
        val out = ByteArrayOutputStream()
        try {
            withResource(
                    ObjectOutputStream(out),
                    object : FnX<ObjectOutputStream, Unit>() {
                        @Throws(Exception::class)
                        override fun applyX(out: ObjectOutputStream): Unit {
                            out.writeObject(a)
                            return Unit.unit
                        }
                    })
            return withResource(
                    ObjectInputStream(ByteArrayInputStream(out.toByteArray())),
                    object : FnX<ObjectInputStream, A>() {
                        @Throws(Exception::class)
                        override fun applyX(`in`: ObjectInputStream): A {
                            return `in`.readObject() as A
                        }
                    })
        } catch (e: IOException) {
            return Prelude.chuck(e)
        }

    }
}
/**
 * Convenience method to read in a file from a local source.
 *
 * @param url
 * The `URL` to read the source data from.
 * @return A String containing the source data or null in the case of an error.
 */
/** Load a properties file from the classpath using the class loader of [IoSupport].  */
/**
 * Open a classpath resource using the class loader of [IoSupport].
 *
 * @see .openClassPathResource
 */
/**
 * Load a classpath resource into a string using the class loader of [IoSupport].
 *
 * @see .loadFileFromClassPathAsString
 */
