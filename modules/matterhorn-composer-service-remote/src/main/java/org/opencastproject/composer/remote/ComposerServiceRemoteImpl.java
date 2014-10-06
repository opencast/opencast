/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.composer.remote;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileBuilder;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.data.Option;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
public class ComposerServiceRemoteImpl extends RemoteBase implements ComposerService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl.class);

  public ComposerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  @Override
  public Job encode(Track sourceTrack, String profileId) throws EncoderException {
    HttpPost post = new HttpPost("/encode");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request for track " + sourceTrack, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("Encoding job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException("Unable to encode track " + sourceTrack + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to encode track " + sourceTrack + " using a remote composer service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#trim(Track, String, long, long)
   */
  @Override
  public Job trim(Track sourceTrack, String profileId, long start, long duration) throws EncoderException {
    HttpPost post = new HttpPost("/trim");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("start", Long.toString(start)));
      params.add(new BasicNameValuePair("duration", Long.toString(duration)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request for track " + sourceTrack, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("Trimming job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException("Unable to trim track " + sourceTrack + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to trim track " + sourceTrack + " using a remote composer service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  @Override
  public Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException {
    HttpPost post = new HttpPost("/mux");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("videoSourceTrack", MediaPackageElementParser.getAsXml(sourceVideoTrack)));
      params.add(new BasicNameValuePair("audioSourceTrack", MediaPackageElementParser.getAsXml(sourceAudioTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request", e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("Muxing job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (IOException e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to mux tracks " + sourceVideoTrack + " and " + sourceAudioTrack
            + " using a remote composer");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    HttpGet get = new HttpGet("/profile/" + profileId + ".xml");
    HttpResponse response = null;
    try {
      response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return EncodingProfileBuilder.getInstance().parseProfile(response.getEntity().getContent());
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long)
   */
  @Override
  public Job image(Track sourceTrack, String profileId, double... times) throws EncoderException {
    HttpPost post = new HttpPost("/image");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("time", buildTimeArray(times)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Image extraction job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to compose an image from track " + sourceTrack
            + " using the remote composer service proxy");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#convertImage(org.opencastproject.mediapackage.Attachment,
   *      java.lang.String)
   */
  @Override
  public Job convertImage(Attachment image, String profileId) throws EncoderException, MediaPackageException {
    HttpPost post = new HttpPost("/convertimage");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceImage", MediaPackageElementParser.getAsXml(image)));
      params.add(new BasicNameValuePair("profileId", profileId));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Image conversion job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to convert image at " + image + " using the remote composer service proxy");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Attachment, java.lang.String)
   */
  @Override
  public Job captions(Track mediaTrack, Catalog[] captions) throws EmbedderException {
    HttpPost post = new HttpPost("/captions");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediaTrack", MediaPackageElementParser.getAsXml(mediaTrack)));
      params.add(new BasicNameValuePair("captions", MediaPackageElementParser.getArrayAsXml(Arrays.asList(captions))));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EmbedderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Caption embedding job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EmbedderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EmbedderException("Unable to embed an captions from catalogs " + captions + " to track " + mediaTrack
            + " using the remote composer service proxy");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @Override
  public EncodingProfile[] listProfiles() {
    HttpGet get = new HttpGet("/profiles.xml");
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        EncodingProfileList profileList = EncodingProfileBuilder.getInstance().parseProfileList(
                response.getEntity().getContent());
        List<EncodingProfileImpl> list = profileList.getProfiles();
        return list.toArray(new EncodingProfile[list.size()]);
      }
    } catch (Exception e) {
      throw new RuntimeException(
              "Unable to list the encoding profiles registered with the remote composer service proxy", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to list the encoding profiles registered with the remote composer service proxy");
  }

  /**
   * Builds string containing times in seconds separated by comma.
   *
   * @param times
   *          time array to be converted to string
   * @return string represented specified time array
   */
  protected String buildTimeArray(double[] times) {
    if (times.length == 0)
      return "";

    StringBuilder builder = new StringBuilder();
    builder.append(Double.toString(times[0]));
    for (int i = 1; i < times.length; i++) {
      builder.append(";" + Double.toString(times[i]));
    }
    return builder.toString();
  }

  @Override
  public Job watermark(Track mediaTrack, String watermark, String profileId) throws EncoderException,
          MediaPackageException {
    HttpPost post = new HttpPost("/watermark");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(mediaTrack)));
      params.add(new BasicNameValuePair("watermark", watermark));
      params.add(new BasicNameValuePair("profileId", profileId));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new EncoderException("Unable to assemble a remote composer request for track " + mediaTrack, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("watermarking job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException("Unable to watermark track " + mediaTrack + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to watermark track " + mediaTrack + " using a remote composer service");
  }

  @Override
  public Job composite(Dimension compositeTrackSize, LaidOutElement<Track> upperTrack,
          LaidOutElement<Track> lowerTrack, Option<LaidOutElement<Attachment>> watermark, String profileId,
          String background) throws EncoderException, MediaPackageException {
    HttpPost post = new HttpPost("/composite");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("compositeSize", Serializer.json(compositeTrackSize).toJson()));
      params.add(new BasicNameValuePair("lowerTrack", MediaPackageElementParser.getAsXml(lowerTrack.getElement())));
      params.add(new BasicNameValuePair("lowerLayout", Serializer.json(lowerTrack.getLayout()).toJson()));
      params.add(new BasicNameValuePair("upperTrack", MediaPackageElementParser.getAsXml(upperTrack.getElement())));
      params.add(new BasicNameValuePair("upperLayout", Serializer.json(upperTrack.getLayout()).toJson()));
      if (watermark.isSome()) {
        params.add(new BasicNameValuePair("watermarkAttachment", MediaPackageElementParser.getAsXml(watermark.get()
                .getElement())));
        params.add(new BasicNameValuePair("watermarkLayout", Serializer.json(watermark.get().getLayout()).toJson()));
      }
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("background", background));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Composite video job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to composite video from track " + lowerTrack.getElement() + " and "
            + upperTrack.getElement() + " using the remote composer service proxy");
  }

  @Override
  public Job concat(String profileId, Dimension outputDimension, Track... tracks) throws EncoderException,
          MediaPackageException {
    HttpPost post = new HttpPost("/concat");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("outputDimension", Serializer.json(outputDimension).toJson()));
      params.add(new BasicNameValuePair("sourceTracks", MediaPackageElementParser.getArrayAsXml(Arrays.asList(tracks))));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Concat video job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to concat videos from tracks " + tracks
            + " using the remote composer service proxy");
  }

  @Override
  public Job imageToVideo(Attachment sourceImageAttachment, String profileId, double time) throws EncoderException,
          MediaPackageException {
    HttpPost post = new HttpPost("/imagetovideo");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("sourceAttachment", MediaPackageElementParser.getAsXml(sourceImageAttachment)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("time", Double.toString(time)));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new EncoderException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        Job r = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Image to video converting job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new EncoderException(e);
    } finally {
      closeConnection(response);
    }
    throw new EncoderException("Unable to convert an image to a video from attachment " + sourceImageAttachment
            + " using the remote composer service proxy");
  }

}
