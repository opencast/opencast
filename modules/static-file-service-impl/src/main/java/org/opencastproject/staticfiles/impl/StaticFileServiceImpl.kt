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

package org.opencastproject.staticfiles.impl

import java.lang.String.format
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.opencastproject.util.RequireUtil.notNull

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.staticfiles.api.StaticFileService
import org.opencastproject.staticfiles.jmx.UploadStatistics
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.OsgiUtil
import org.opencastproject.util.ProgressInputStream
import org.opencastproject.util.jmx.JmxUtil

import com.google.common.util.concurrent.AbstractScheduledService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Service.Listener
import com.google.common.util.concurrent.Service.State

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.osgi.service.component.ComponentException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

import javax.management.ObjectInstance

/**
 * Stores and retrieves static file resources.
 */
 class StaticFileServiceImpl:StaticFileService {

/** The JMX business object for uploaded statistics  */
  private val staticFileStatistics = UploadStatistics()

/** The JMX bean object instance  */
  private var registerMXBean:ObjectInstance? = null

 // OSGi service references
  private var securityService:SecurityService? = null
private var orgDirectory:OrganizationDirectoryService? = null

/** The root directory for storing static files.  */
  private var rootDirPath:String? = null

private var purgeService:PurgeTemporaryStorageService? = null

/**
 * OSGI callback for activating this component
 *
 * @param cc
 * the osgi component context
 */
   fun activate(cc:ComponentContext) {
logger.info("Upload Static Resource Service started.")
registerMXBean = JmxUtil.registerMXBean(staticFileStatistics, "UploadStatistics")
rootDirPath = OsgiUtil.getContextProperty(cc, STATICFILES_ROOT_DIRECTORY_KEY)

val rootFile = File(rootDirPath!!)
if (!rootFile.exists())
{
try
{
FileUtils.forceMkdir(rootFile)
}
catch (e:IOException) {
throw ComponentException(
String.format("%s does not exists and could not be created", rootFile.getAbsolutePath()))
}

}
if (!rootFile.canRead())
throw ComponentException(String.format("Cannot read from %s", rootFile.getAbsolutePath()))

purgeService = PurgeTemporaryStorageService()
purgeService!!.addListener(object:Listener() {
public override fun failed(from:State?, failure:Throwable?) {
logger.warn("Temporary storage purging service failed: {}", getStackTrace(failure!!))
}
}, MoreExecutors.directExecutor())
purgeService!!.startAsync()
logger.info("Purging of temporary storage section scheduled")
}

/**
 * Callback from OSGi on service deactivation.
 */
   fun deactivate() {
JmxUtil.unregisterMXBean(registerMXBean!!)

purgeService!!.stopAsync()
purgeService = null
}

/** OSGi DI  */
   fun setSecurityService(securityService:SecurityService) {
this.securityService = securityService
}

/** OSGi DI  */
   fun setOrganizationDirectoryService(directoryService:OrganizationDirectoryService) {
orgDirectory = directoryService
}

@Throws(IOException::class)
public override fun storeFile(filename:String, inputStream:InputStream):String {
notNull(filename, "filename")
notNull(inputStream, "inputStream")
val uuid = UUID.randomUUID().toString()
val org = securityService!!.organization.id

val file = getTemporaryStorageDir(org).resolve(Paths.get(uuid, filename))
try
{
ProgressInputStream(inputStream).use({ progressInputStream->
progressInputStream.addPropertyChangeListener(object:PropertyChangeListener {
public override fun propertyChange(evt:PropertyChangeEvent) {
val totalNumBytesRead = evt.getNewValue() as Long
val oldTotalNumBytesRead = evt.getOldValue() as Long
staticFileStatistics.add(totalNumBytesRead - oldTotalNumBytesRead)
}
})

Files.createDirectories(file.getParent())
Files.copy(progressInputStream, file) })
}
catch (e:IOException) {
logger.error("Unable to save file '{}' to {}", filename, file, e)
throw e
}

return uuid
}

@Throws(NotFoundException::class, IOException::class)
public override fun getFile(uuid:String):InputStream {
if (StringUtils.isBlank(uuid))
throw IllegalArgumentException("The uuid must not be blank")

val org = securityService!!.organization.id

return Files.newInputStream(getFile(org, uuid))
}

@Throws(NotFoundException::class, IOException::class)
public override fun persistFile(uuid:String) {
val org = securityService!!.organization.id
Files.newDirectoryStream(getTemporaryStorageDir(org),
getDirsEqualsUuidFilter(uuid)).use({ folders-> for (folder in folders)
{
Files.move(folder, getDurableStorageDir(org).resolve(folder.getFileName()))
} })
}

@Throws(NotFoundException::class, IOException::class)
public override fun deleteFile(uuid:String) {
val org = securityService!!.organization.id
val file = getFile(org, uuid)
Files.deleteIfExists(file)
}

@Throws(NotFoundException::class)
public override fun getFileName(uuid:String):String {
val org = securityService!!.organization.id
try
{
val file = getFile(org, uuid)
return file.getFileName().toString()
}
catch (e:IOException) {
logger.warn("Error while reading file: {}", getStackTrace(e))
throw NotFoundException(e)
}

}

@Throws(NotFoundException::class)
public override fun getContentLength(uuid:String):Long? {
val org = securityService!!.organization.id
try
{
val file = getFile(org, uuid)
return Files.size(file)
}
catch (e:IOException) {
logger.warn("Error while reading file: {}", getStackTrace(e))
throw NotFoundException(e)
}

}

/**
 * Returns the temporary storage directory for an organization.
 *
 * @param org
 * The organization
 * @return Path to the temporary storage directory
 */
  private fun getTemporaryStorageDir(org:String):Path {
return Paths.get(rootDirPath, org, "temp")
}

private fun getDurableStorageDir(org:String):Path {
return Paths.get(rootDirPath!!, org)
}

@Throws(NotFoundException::class, IOException::class)
private fun getFile(org:String, uuid:String):Path {
 // First check if the file is part of the durable storage section
    Files.newDirectoryStream(getDurableStorageDir(org),
getDirsEqualsUuidFilter(uuid)).use({ dirs-> for (dir in dirs)
{
Files.newDirectoryStream(dir).use({ files-> for (file in files)
{
return file
} })
} })

 // Second check if the file is part of the temporary storage section
    Files.newDirectoryStream(getTemporaryStorageDir(org),
getDirsEqualsUuidFilter(uuid)).use({ dirs-> for (dir in dirs)
{
Files.newDirectoryStream(dir).use({ files-> for (file in files)
{
return file
} })
} })

throw NotFoundException(format("No file with UUID '%s' found.", uuid))
}

/**
 * Deletes all files found in the temporary storage section of an organization.
 *
 * @param org
 * The organization identifier
 * @throws IOException
 * if there was an error while deleting the files.
 */
  @Throws(IOException::class)
internal fun purgeTemporaryStorageSection(org:String, lifetime:Long) {
logger.info("Purge temporary storage section of organization '{}'", org)
val temporaryStorageDir = getTemporaryStorageDir(org)
if (Files.exists(temporaryStorageDir))
{
Files.newDirectoryStream(temporaryStorageDir,
object:DirectoryStream.Filter<Path> {
@Throws(IOException::class)
public override fun accept(path:Path):Boolean {
return (Files.getLastModifiedTime(path).toMillis() < (Date()).getTime() - lifetime)
}
}).use({ tempFilesStream-> for (file in tempFilesStream)
{
FileUtils.deleteQuietly(file.toFile())
} })
}
}

/**
 * Deletes all files found in the temporary storage section of all known organizations.
 *
 * @throws IOException
 * if there was an error while deleting the files.
 */
  @Throws(IOException::class)
internal fun purgeTemporaryStorageSection() {
logger.info("Start purging temporary storage section of all known organizations")
for (org in orgDirectory!!.organizations)
{
purgeTemporaryStorageSection(org.id, TimeUnit.DAYS.toMillis(1))
}
}

/** Scheduled service for purging temporary storage sections.  */
  private inner class PurgeTemporaryStorageService:AbstractScheduledService() {

@Throws(Exception::class)
protected override fun runOneIteration() {
this@StaticFileServiceImpl.purgeTemporaryStorageSection()
}

protected override fun scheduler():AbstractScheduledService.Scheduler {
return AbstractScheduledService.Scheduler.newFixedRateSchedule(0, 1, TimeUnit.HOURS)
}

}

companion object {

/** The logger  */
  private val logger = LoggerFactory.getLogger(StaticFileServiceImpl::class.java!!)

/** The key to find the root directory for the static file service in the OSGi properties.  */
   val STATICFILES_ROOT_DIRECTORY_KEY = "org.opencastproject.staticfiles.rootdir"

/**
 * Returns a [DirectoryStream.Filter] to filter the entries of a directory and only return items which filename
 * starts with the UUID.
 *
 * @param uuid
 * The UUID to filter by
 * @return the filter
 */
  private fun getDirsEqualsUuidFilter(uuid:String):DirectoryStream.Filter<Path> {
return object:DirectoryStream.Filter<Path> {
@Throws(IOException::class)
public override fun accept(entry:Path):Boolean {
return Files.isDirectory(entry) && entry.getFileName().toString() == uuid
}
}
}
}

}
