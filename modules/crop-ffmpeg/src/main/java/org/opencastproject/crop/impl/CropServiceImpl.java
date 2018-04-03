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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.crop.impl;

import org.opencastproject.crop.api.CropException;
import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.io.LineReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Media analysis plugin that takes a video stream and removes black bars on each side
 *
 * This plugin runs
 * <pre>
 *     ffmpeg -i input.file -vf cropdetect=24:16:0 -max_muxing_qurue_size 2000 -y output.file
 *
 *     ffmpeg -i input.file -vf crop=wi-2*x:hi:x:0 -max_muxing_queue_size 2000 -y output.file
 * </pre>
 */
public class CropServiceImpl extends AbstractJobProducer implements CropService, ManagedService {

    /** Resulting collection in the working file repository */
    public static final String COLLECTION_ID = "cropping";

    /** List of available operations on jobs */
    private enum Operation {
        Crop
    }

    /** Path to the executable */
    protected String binary;


    public static final String FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path";
    public static final String FFMEG_BINARY_DEFAULT = "ffmpeg";

    /** The load intorduced on the system by creating a caption job */
    public static final float DEFAULT_CROP_JOB_LOAD = 1.0f;

    /** The key to look for in the service configuration file to override the DEFAULT_CROP_JOB_LOAD */
    public static final String CROP_JOB_LOAD_KEY = "job.load.cropping";

    /** The load introduced on the system by creating a cropping job */
    private float cropJobLoad = DEFAULT_CROP_JOB_LOAD;

    /** The logging facility */
    protected static final Logger logger = LoggerFactory.getLogger(CropServiceImpl.class);

    /** Reference to the receipt service */
    protected ServiceRegistry serviceRegistry = null;

    /** The mpeg7 service */
    protected Mpeg7CatalogService mpeg7CatalogService = null;

    /** The workspace to use when retrieving remote media files */
    protected Workspace workspace = null;

    protected SecurityService securityService = null;

    protected OrganizationDirectoryService organizationDirectoryService = null;

    protected UserDirectoryService userDirectoryService = null;

    /**
     * Creates a new instance of the crop service.
     */
    public CropServiceImpl() {
        super(JOB_TYPE);
        this.binary = FFMEG_BINARY_DEFAULT;
    }

    @Override
    public void activate(ComponentContext cc) {
        super.activate(cc);
        final String path = cc.getBundleContext().getProperty(FFMPEG_BINARY_CONFIG);
        this.binary = path == null ? FFMEG_BINARY_DEFAULT : path;
        logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, FFMEG_BINARY_DEFAULT);
    }

    protected Track crop(Job job, Track track) throws CropException, MediaPackageException {

        if (!track.hasVideo()) {
            logger.warn("Element {} is not a track", track);
            throw new CropException("Element is not a video track");
        }
        try {
            Track croppedTrack;

            File mediaFile = null;
            URL mediaUrl = null;
            try {
                mediaFile = workspace.get(track.getURI());
                mediaUrl = mediaFile.toURI().toURL();
            } catch (NotFoundException e) {
                throw new CropException("Error finding the video file in the workspace", e);
            } catch (IOException e) {
                throw  new CropException("Error reading the video file in the workspace", e);
            }
            if (track.getDuration() == null) {
                throw new MediaPackageException("Track " + track + " does not have a duration");
            }

            logger.info("starting cropping of {} ", track);


            croppedTrack =  cropFfmpeg(mediaFile, track);


            Track cropTrack = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Track.TYPE, track.getFlavor());
            URI uri;
            try {
                uri = croppedTrack.getURI();
            } catch (Exception e) {
                throw new CropException("Unable to put the mpeg7 catalog into the workspace", e);
            }
            cropTrack.setURI(uri);

            logger.info("Finished video cropping of {}", mediaUrl);

            return cropTrack;

        } catch (Exception e) {
            logger.warn("Error cropping video", e);
            if (e instanceof CropException) {
                throw (CropException) e;
            } else {
                throw new CropException(e);
            }
        }

    }

    protected Track cropFfmpeg(File mediafile, Track track) throws IOException, CropException {
        String outputPath = FilenameUtils.removeExtension(mediafile.getAbsolutePath()).concat("_cropdetect.mp4");
        String[] command = new String[] { binary, "-i", mediafile.getAbsolutePath().replaceAll(" ", "\\ "), "-vf", "cropdetect=24:16:240", "-max_muxing_queue_size", "2000", "-y", outputPath};
        String commandline = StringUtils.join(command, " ");

        logger.info("Running {}", commandline);

        ProcessBuilder pbuilder = new ProcessBuilder(command);
        int cropValue = 0;
        int widthVideo = 0;
        pbuilder.redirectErrorStream(true);
        int exitCode = 1;
        Process process = pbuilder.start();
        BufferedReader errStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String line = errStream.readLine();
            while (null != line) {
                if (line.startsWith("[Parsed_cropdetect")) {
                    // [Parsed_cropdetect_0 @ 0x8f6620] x1:120 x2:759 y1:0 y2:399 w:640 h:400 x:120 y:0 pts:639356 t:39.023193 crop=640:400:120:0
                    String[] lineSplitted = line.split(" ");
                    widthVideo = Integer.valueOf(lineSplitted[7].substring(2));
                    int x = Integer.valueOf(lineSplitted[9].substring(2));
                    if (cropValue == 0 || cropValue > x) {
                        cropValue = x;
                    }

                }
                line = errStream.readLine();
            }
            exitCode = process.waitFor();

        } catch (IOException e) {
            logger.error("Error executing ffmeg: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Waiting for encoder process exited was interrupted unexpected: {}", e.getMessage());
        }
        finally {
            IoSupport.closeQuietly(process);
            IoSupport.closeQuietly(errStream);
            if (exitCode != 0) {
                try {
                    FileUtils.forceDelete(new File(outputPath));
                } catch (IOException e) {

                }
            }
        }

        if (exitCode != 0) {
            throw new CropException("The encoder process exited abnormally with exit code " + exitCode);
        }
        if (cropValue > widthVideo / 3) {
            return track;
        }
        // FFMPEG-command for cropping video
        String crop = "crop=" + "iw-" + 2 * cropValue + ":ih:" + cropValue + ":0";
        logger.info("String for crop command: {}", crop);
        String croppedOutputPath = FilenameUtils.removeExtension(mediafile.getAbsolutePath()).concat("_cropped.mp4");
        String[] cropCommand = new String[] { binary, "-i", mediafile.getAbsolutePath().replaceAll(" ", "\\ "), "-vf", crop, "-max_muxing_queue_size", "2000", "-y", croppedOutputPath };
        String cropCommandline = StringUtils.join(cropCommand, " ");

        logger.info("running {}", cropCommandline);

        ProcessBuilder pbuilder1 = new ProcessBuilder(cropCommand);
        Process process1 = pbuilder1.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process1.getErrorStream()));
        try {
            LineReader lr = new LineReader(reader);
            String line = lr.readLine();
            while (null != line) {
                line = lr.readLine();
            }
        } catch (IOException e) {
            logger.error("Error executing ffmeg: {}", e.getMessage());
        } finally {
            reader.close();
        }
        // put output file into workspace
        FileInputStream outputFileStream = null;
        URI outputFileUri = null;
        try {
            outputFileStream = new FileInputStream(croppedOutputPath);
            outputFileUri = workspace.putInCollection(COLLECTION_ID, FilenameUtils.getName(croppedOutputPath), outputFileStream);
            logger.info("Copied the created outputfile to the workspace {}", outputFileUri.toString());
        } catch (FileNotFoundException e) {
            throw new CropException("Output file " + croppedOutputPath + " not found", e);
        } catch (IOException e) {
            throw new CropException("Can't write output file " + croppedOutputPath + " to workspace", e);
        } catch (IllegalArgumentException e) {
            throw new CropException(e);
        } finally {
            IoSupport.closeQuietly(outputFileStream);
            //logger.info("Deleted local output file at {}", outputPath);
            //FileUtils.deleteQuietly(new File(outputPath));
        }

        // create media package element
        MediaPackageElementBuilder mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

        // create catalog from media package element
        TrackImpl outputFileTrack = null;
        try {
            outputFileTrack = (TrackImpl) mpElementBuilder
                    .elementFromURI(outputFileUri, MediaPackageElement.Type.Track, track.getFlavor());
            outputFileTrack.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew().compact());
        } catch (UnsupportedElementException e) {
            throw new CropException("Unable to create track element from " + outputFileUri, e);
        }

        outputFileTrack.setDuration(track.getDuration());
        return outputFileTrack;
    }

    @Override
    protected String process(Job job) throws Exception {
        Operation op = null;
        String operation = job.getOperation();
        List<String> arguments = job.getArguments();
        try {
            op = Operation.valueOf(operation);
            switch (op) {
                case Crop:
                    Track track = (Track) MediaPackageElementParser
                            .getFromXml(arguments.get(0));
                    Track croppedTrack = crop(job, track);
                    return MediaPackageElementParser.getAsXml(croppedTrack);
                default:
                    throw new IllegalStateException("Don't know how to handle operations '" + operation + "'");
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceRegistryException("This service can't handle operations of type: " + op, e);
        } catch (IndexOutOfBoundsException e) {
            throw new ServiceRegistryException("This argument list for operations '" + op + "' does not meet expectations", e);
        } catch (Exception e) {
            throw new ServiceRegistryException("Error handling operations: " + op , e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see CropService#crop(org.opencastproject.mediapackage.Track)
     */
    @Override
    public Job crop(Track track) throws CropException, MediaPackageException {
        try {
            Job job = serviceRegistry.createJob(JOB_TYPE, Operation.Crop.toString(), Arrays.asList(MediaPackageElementParser.getAsXml(track)),
                    cropJobLoad);
            return job;
        } catch (ServiceRegistryException e) {
            throw new CropException("Unable to create a job", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary == null) {
            return;
        }
        logger.debug("Configuring the cropper");

        cropJobLoad = LoadUtil.getConfiguredLoadValue(dictionary, CROP_JOB_LOAD_KEY, DEFAULT_CROP_JOB_LOAD, serviceRegistry);

    }

    @Override
    protected ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    protected SecurityService getSecurityService() {
        return securityService;
    }

    @Override
    protected UserDirectoryService getUserDirectoryService() {
        return userDirectoryService;
    }

    @Override
    protected OrganizationDirectoryService getOrganizationDirectoryService() {
        return organizationDirectoryService;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) { this.serviceRegistry = serviceRegistry; }

    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) { this.mpeg7CatalogService = mpeg7CatalogService; }

    public void setUserDirectoryService(UserDirectoryService userDirectoryService) { this.userDirectoryService = userDirectoryService; }

    public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) { this.organizationDirectoryService = organizationDirectoryService; }

    public void setSecurityService(SecurityService securityService) { this.securityService = securityService; }
}

