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
package org.opencastproject.deliver.itunesu;

import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.RetryException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This is a helper of iTunes U Web service.
 *
 * It calls the iTunes U Web service to upload, delete, or list the media content under a given
 * group.
 * 
 * For technical details of iTunes U Web service refer to
 * <ul>
 * <li><a href="http://deimos.apple.com/rsrc/doc/iTunesUAdministrationGuide/iTunesUWebServices/chapter_18_section_2.html#//apple_ref/doc/uid/AdminGuide-CH13-SW2">What Is iTunes U Web Services?</a>
 * </ul>
 */
public class ITunesWSAPI {
  /** Configuration parameters for ITunes service. */
  private static ITunesConfiguration configuration = ITunesConfiguration.getInstance();

  /** Base URL of iTunesU Web service */
  private final static String WS_URL = "https://deimos.apple.com/WebObjects/Core.woa/API/";

  /** the API endpoint to get an upload URL */
  private final static String GET_UPLOAD_URL = "GetUploadURL";

  /** the API endpoint to show tree */
  private final static String SHOW_TREE = "ShowTree";

  /** number of retries before getting an upload URL */
  private final int NUM_RETRY = 5;

  /** Helper instance */
  private HTTPHelper helper;

  /** destination to issue request */
  private String destination;

  /** constructor */
  public ITunesWSAPI(String destination) {
    this.destination = destination;
  }

  /**
   * Gets the site URL.
   *
   * @return site URL
   */
  public String getSiteURL()
  {
    return configuration.getSiteURL();
  }

  /**
   * Uploads a file via iTunes U Web service.
   *
   * The service will create an authorization token given the credentials specified in the 
   * configuration file, request an upload URL using the authorization token, and upload the media
   * file to the upload URL using a multi-part form HTTP POST request. The service will also return 
   * the handle of the uploaded track under the given group upon successful uploading.
   *
   * @param fileName full path of the file
   * @return handle of the uploaded track
   * @throws FailedException 
   * @throws RetryException 
   */ 
  public String uploadFile(String fileName) throws FailedException, RetryException
  {
    // get an authorization token
    String token = getAuthorizationToken(false);

    // get an upload URL
    String uploadURLString = getUploadURL(token);

    // the upload URL is only valid for 90 seconds

    // upload file to the upload URL
    String handle = "";
    try {
      helper = new HTTPHelper(uploadURLString);
      handle = helper.uploadFile(fileName);
      if (handle.equals("!")) {
        // no more error message from iTunes U service to interpret
        throw new FailedException("Media file not accepted by iTunes U site!");
      }
    } catch (IOException e) {
      // cannot upload file
      throw new FailedException(e);
    }

    return handle;
  }

  /**
   * Lists tracks under a group. Regular expression matching is used instead of XML parsing to reduce
   * overhead and avoid external jar files such as jdom in the OSGi setting.
   *
   * @return list of ITUnesTrack objects.
   * @throws RetryException 
   * @throws FailedException 
   */
  public List<ITunesTrack> listTracks() throws RetryException, FailedException {
    // get an authorization token
    String token = getAuthorizationToken(false);

    String urlString = 
      WS_URL + SHOW_TREE + "/" + configuration.getSiteURL() + "." + destination;
    try {
      helper = new HTTPHelper(urlString);
    } catch (IOException e) {
      throw new RetryException(e);
    }

    String response = "";
    try {
      response = helper.httpGet(token);
    } catch (IOException e) {
      throw new RetryException(e);      
    }

    // use reluctant (lazy) qualifier to match one <Track> ... </Track> pair at a time
    // DOT matches anything including newline characters
    Pattern pattern = 
        Pattern.compile("<Track>.*?<Name>([^<]+)</Name>.*?<Handle>([^<]+)</Handle>.*?" +
                        "<Kind>([^<]+)</Kind>.*?" + 
                        "<DurationMilliseconds>([^<]+)</DurationMilliseconds>.*?</Track>", 
                        Pattern.DOTALL);
    Matcher matcher = pattern.matcher(response);

    String name, handle, kind;
    int duration;
    List<ITunesTrack> list = new ArrayList<ITunesTrack>();
    while (matcher.find()) {
      name = matcher.group(1);
      handle = matcher.group(2);
      kind = matcher.group(3);
      duration = Integer.parseInt(matcher.group(4));

      list.add(new ITunesTrack(name, handle, kind, duration));
    }

    return list;
  }

  /**
   * Deletes a track under a group. Creates a Web service document that specifies the DeteteTrack
   * command and uploads to the upload URL.
   *
   * @param handle handle of the track to be deleted
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String deleteTrack(String handle) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    buffer.append("<Version>1.1.4</Version>");
    buffer.append("<DeleteTrack>");
    buffer.append("<TrackHandle>" + handle + "</TrackHandle>");
    buffer.append("</DeleteTrack>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Creates a feed group. Creates a Web service document that specifies the AddGroup
   * command and uploads to the upload URL.
   *
   * @param handle handle of the parent (course)
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String addFeedGroup(String handle, String groupName, String feedURL) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    buffer.append("<Version>1.1.1</Version>");
    buffer.append("<AddGroup>");
    buffer.append("<ParentHandle>" + handle + "</ParentHandle>");
    buffer.append("<Group>");
    buffer.append("<Name>" + groupName + "</Name>");
    buffer.append("<GroupType>Feed</GroupType>");
    buffer.append("<ExternalFeed>");
    buffer.append("<URL>" + feedURL + "</URL>");
    buffer.append("<OwnerEmail>stone.xiang@yahoo.com</OwnerEmail>");
    buffer.append("<PollingInterval>Daily</PollingInterval>");
    buffer.append("<SecurityType>None</SecurityType>");
    buffer.append("<SignatureType>None</SignatureType>");
    buffer.append("</ExternalFeed>");
    buffer.append("</Group>");
    buffer.append("</AddGroup>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Updates a feed group. Creates a Web service document that specifies the UpdateGroup
   * command and uploads to the upload URL.
   *
   * @param handle handle of the track
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String updateFeedGroup(String handle) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    buffer.append("<Version>1.1.1</Version>");
    buffer.append("<UpdateGroup>");
    buffer.append("<GroupHandle>" + handle +"</GroupHandle>");
    buffer.append("</UpdateGroup>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Updates a feed. Creates a Web service document that specifies the UpdateFeed
   * command and uploads to the upload URL.
   *
   * @param handle handle of the group
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String updateFeed(String handle) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    buffer.append("<Version>1.1.4</Version>");
    buffer.append("<UpdateFeed>");
    buffer.append("<FeedIdentifier>");
    buffer.append("<Handle>" + handle +"</Handle>");
    buffer.append("</FeedIdentifier>");
    buffer.append("</UpdateFeed>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Adds a track. Creates a Web service document that specifies the AddTrack
   * command.
   *
   * @param handle handle of the track
   * @param name name of the track
   * @param durationMilliseconds duration of the track
   * @param albumName name of the album
   * @param artistName name of the artist
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String addTrack(String handle, String name, int durationMilliseconds, String albumName, String artistName) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    // buffer.append("<Version>1.1.4</Version>");
    buffer.append("<AddTrack>");
    buffer.append("<ParentHandle>" + destination + "</ParentHandle>");
    buffer.append("<ParentPath></ParentPath>");
    buffer.append("<Track>");
    buffer.append("<Name>" + name + "</Name>");
    // buffer.append("<Handle>" + handle +"</Handle>");
    buffer.append("<DiscNumber>1</DiscNumber>");
    buffer.append("<DurationMilliseconds>" + durationMilliseconds + "</DurationMilliseconds>");
    buffer.append("<AlbumName>" + albumName + "</AlbumName>");
    buffer.append("<ArtistName>" + artistName + "</ArtistName>");
    buffer.append("<DownloadURL>http://jcr-connect.at.northwestern.edu/documents/MaleSample2.mp3</DownloadURL>");
    buffer.append("</Track>");
    buffer.append("</AddTrack>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Escapes a string for XML.
   *
   * @param s the original string
   * @return escaped string
   */
  private String escapeString(String s)
  {
    return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  /**
   * Merges a track. Creates a Web service document that specifies the MergeTrack
   * command, which updates the metadata of the track.
   *
   * @param handle handle of the track
   * @param title title of the track
   * @param creator creator of the media
   * @param comment comment
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String mergeTrack(String handle, String title, String creator, String comment) throws FailedException, RetryException {
    StringBuffer buffer = new StringBuffer();

    // construct the XML request
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<ITunesUDocument>");
    buffer.append("<MergeTrack>");
    buffer.append("<TrackHandle>" + handle + "</TrackHandle>");
    buffer.append("<Track>");
    buffer.append("<Title>" + escapeString(title) + "</Title>");
    buffer.append("<DiscNumber>1</DiscNumber>");
    buffer.append("<ArtistName>" + escapeString(creator) + "</ArtistName>");
    // keep the metadata in the media file but leave this open as an option
    // buffer.append("<AlbumName>albumname value</AlbumName>");
    buffer.append("<Comment>" + escapeString(comment) + "</Comment>");
    // buffer.append("<TrackNumber>" + "1" + "</TrackNumber>");
    buffer.append("</Track>");
    buffer.append("</MergeTrack>");
    buffer.append("</ITunesUDocument>");

    String wsDocument = buffer.toString();

    return uploadWSDocument(wsDocument);
  }

  /**
   * Uploads a Web service document to iTunes U.
   *
   * @param wsDocument string content of the Web service document
   * @return response from the Web service
   * @throws FailedException 
   * @throws RetryException 
   */
  public String uploadWSDocument(String wsDocument) throws FailedException, RetryException {
    // get an authorization token - true for WS XML document
    String token = getAuthorizationToken(true);

    // get an upload URL
    String uploadURLString = getUploadURL(token);

    // the upload URL is only valid for 90 seconds
    
    String response = "";

    // create a temporary file for the WS document
    try {
      File temp = File.createTempFile("wsDocument",".xml");
      temp.deleteOnExit();

      // write to file
      FileWriter fw = new FileWriter( temp );
      fw.write(wsDocument);

      fw.close();

      helper = new HTTPHelper(uploadURLString);
      response = helper.uploadFile(temp.getAbsolutePath());      
    } catch (Exception e) {
      throw new FailedException(e);
    }

    Pattern pattern = Pattern.compile("<error>([^<]+)</error>");
    Matcher matcher = pattern.matcher(response);

    if (matcher.find()) {
      throw new FailedException(matcher.group(1));
    }

    return response;
  }

  /**
   * Generates the HMAC-SHA256 signature of a message string, as defined in
   * <A HREF="http://www.ietf.org/rfc/rfc2104.txt">RFC 2104</A>.
   *
   * @param message The string to sign.
   * @param key The bytes of the key to sign it with.
   *
   * @return A hexadecimal representation of the signature.
   */
  private String hmacSHA256(String message, byte[] key) 
  {
    // Start by getting an object to generate SHA-256 hashes with.
    MessageDigest sha256 = null;

    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new java.lang.AssertionError(
              this.getClass().getName()
              + ".hmacSHA256(): SHA-256 algorithm not found!");
    }

    // Hash the key if necessary to make it fit in a block (see RFC 2104).
    if (key.length > 64) {
      sha256.update(key);
      key = sha256.digest();
      sha256.reset();
    }

    // Pad the key bytes to a block (see RFC 2104).
    byte block[] = new byte[64];
    for (int i = 0; i < key.length; ++i) {
      block[i] = key[i];
    }

    for (int i = key.length; i < block.length; ++i) {
      block[i] = 0;
    }

    // Calculate the inner hash, defined in RFC 2104 as
    // SHA-256(KEY ^ IPAD + MESSAGE)), where IPAD is 64 bytes of 0x36.
    for (int i = 0; i < 64; ++i) {
      block[i] ^= 0x36;
    }

    sha256.update(block);

    try {
      sha256.update(message.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new java.lang.AssertionError(
            "ITunesU.hmacSH256(): UTF-8 encoding not supported!");
    }

    byte[] hash = sha256.digest();
    sha256.reset();

    // Calculate the outer hash, defined in RFC 2104 as
    // SHA-256(KEY ^ OPAD + INNER_HASH), where OPAD is 64 bytes of 0x5c.
    for (int i = 0; i < 64; ++i) {
      block[i] ^= (0x36 ^ 0x5c);
    }

    sha256.update(block);
    sha256.update(hash);
    hash = sha256.digest();

    // The outer hash is the message signature...
    // convert its bytes to hexadecimals.
    char[] hexadecimals = new char[hash.length * 2];
    for (int i = 0; i < hash.length; ++i) {
      for (int j = 0; j < 2; ++j) {
        int value = (hash[i] >> (4 - 4 * j)) & 0xf;
        char base = (value < 10) ? ('0') : ('a' - 10);
        hexadecimals[i * 2 + j] = (char)(base + value);
      }
    }

    // Return a hexadecimal string representation of the message signature.
    return new String(hexadecimals);
  }

  /**
   * Combines user credentials into an appropriately formatted string.
   *
   * @param credentials An array of credential strings. Credential
   *                    strings may contain any character but ';'
   *                    (semicolon), '\\' (backslash), and control
   *                    characters (with ASCII codes 0-31 and 127).
   *
   * @return <CODE>null</CODE> if and only if any of the credential strings 
   *         are invalid.
   */
  private String getCredentialsString(String[] credentials) 
  {
    // Create a buffer with which to generate the credentials string.
    StringBuffer buffer = new StringBuffer();

    // Verify and add each credential to the buffer.
    if (credentials != null) {
      for (int i = 0; i < credentials.length; ++i) {
        if (i > 0) {
          buffer.append(';');
        }
        for (int j = 0, n = credentials[i].length(); j < n; ++j) {
          char c = credentials[i].charAt(j);
          if (c != ';' && c != '\\' && c >= ' ' && c != 127) {
            buffer.append(c);
          } else {
            return null;
          }
        }
      }
    }

    // Return the credentials string.
    return buffer.toString();
  }

  /**
   * Obtains an authorization token.
   *
   * @param xmlControlFile whether this is for iTunes U Web service or for media file uploading 
   * @return the authorization token
   * @throws FailedException 
   */
  private String getAuthorizationToken(boolean xmlControlFile) throws FailedException 
  {
    // log("Auth user=" + user);

    String credentials = 
        getCredentialsString(new String [] {configuration.getAdministratorCredential()});
    Date now = new Date();
    byte[] key = null;
    try {
      key = configuration.getSharedSecret().getBytes("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new FailedException("ITunesU.hmacSH256(): US-ASCII encoding not supported!");
    }

    // Create a buffer with which to generate the authorization token.
    StringBuffer buffer = new StringBuffer();

    // Generate the authorization token.
    try {
        // Start with the appropriately encoded credentials.
      buffer.append("credentials=");
      buffer.append(URLEncoder.encode(credentials, "UTF-8"));

      // Add the appropriately encoded identity information.
      buffer.append("&identity=");
      // buffer.append(URLEncoder.encode(identity, "UTF-8"));

      // Add the appropriately formatted time stamp. Note that
      // the time stamp is expressed in seconds, not milliseconds.
      buffer.append("&time=");
      buffer.append(now.getTime() / 1000);

      // Generate and add the token signature.
      String data = buffer.toString();
      buffer.append("&signature=");
      buffer.append(hmacSHA256(data, key));

      if (xmlControlFile) {
        buffer.append("&type=XMLControlFile");
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 encoding support is required.
      throw new FailedException("getAuthorizationToken(): " + "UTF-8 encoding not supported!");
    }

    // Return the signed authorization token.
    return buffer.toString();
  }

  /**
   * Requests an upload URL by sending an authorization token to the Web service endpoint
   * in an HTTP GET request.
   *
   * @param token the authorization token
   * @return the upload URL
   * @throws RetryException 
   * @throws FailedException 
   */
  private String getUploadURL(String token) throws RetryException, FailedException
  {
    String urlString = 
      WS_URL + GET_UPLOAD_URL + "/" + configuration.getSiteURL() + "." + destination;
    try {
      helper = new HTTPHelper(urlString);
    } catch (IOException e) {
      throw new RetryException(e);
    }

    String uploadURLString = "";
    // request will be rejected if the time stamp in the authorization token is much later than
    // the system time of the iTunes U site
    for (int i = 0; i < NUM_RETRY; ++i) {
      try {
        uploadURLString = helper.httpGet(token);
      } catch (IOException e) {
        if (i == NUM_RETRY - 1) {
          // cannot get an upload URL after retries
          throw new FailedException(e);
        }
        // retry
      }

      if (! uploadURLString.equals("")) {
        break;
      }

      try {
        Thread.sleep(3000);
      } catch (Exception e) {
        // blank
      }
    } // end of for

    return uploadURLString;
  }
}
