# Workflow Operation Handler

## Introduction
Workflows are the central element to define how a media package is being processed by the Matterhorn services. Their definitions consist of a list of workflow operations, which basically map a piece of configuration to Matterhorn code:

    <definition xmlns="http://workflow.opencastproject.org">
         ....
        <operation
          id="tag"
          <configurations>
            <configuration key="source-flavors">presentation/trimmed</configuration>
            <configuration key="target-flavor">presentation/tagged</configuration>
          </configurations>
       </operation>
        ...
    </definition>

##Default Workflow Operations
The following table contains the workflow operations that are available in an out-of-the-box Matterhorn installation:

<table> 
<tr><th>Operation Handler	</th><th>Description						</th><th>Details</th></tr>
<tr><td>defaults		</td><td>Applies default workflow configuration values		</td><td>[Documentation](defaults-woh.md)</td></tr>
<tr><td>email			</td><td>Sends email notifications at any part of a workflow	</td><td>[Documentation](email-woh.md)</td></tr>
<tr><td>republish		</td><td>Republishes elements to search				</td><td>[Documentation](republish-woh.md)</td></tr>
<tr><td>tag			</td><td>Modify the tag sets of media package elements		</td><td>[Documentation](tag-woh.md)</td></tr>
<tr><td>apply-acl		</td><td>Apply ACL rom series to the mediapackage		</td><td>[Documentation](applyacl-woh.md)</td></tr>
<tr><td>inspect			</td><td>Inspect the media (check if it is valid)		</td><td>[Documentation](inspect-woh.md)</td></tr>
<tr><td>prepare-av		</td><td>Preparing audio and video work versions		</td><td>[Documentation](prepareav-woh.md)</td></tr>
<tr><td>compose			</td><td>Encode media files using FFmpeg			</td><td>[Documentation](compose-woh.md)</td></tr>
<tr><td>trim			</td><td>Waiting for user to review, then trim the recording	</td><td>[Documentation](trim-woh.md)</td></tr>
<tr><td>caption			</td><td>Waiting for user to upload captions			</td><td>[Documentation](caption-woh.md)</td></tr>
<tr><td>segment-video		</td><td>Extracting segments from presentation			</td><td>[Documentation](segmentvideo-woh.md)</td></tr>
<tr><td>image			</td><td>Extract images from a video using FFmpeg		</td><td>[Documentation](image-woh.md)</td></tr>
<tr><td>segmentpreviews		</td><td>Extract segment images from a video using FFmpeg	</td><td>[Documentation](segmentpreviews-woh.md)</td></tr>
<tr><td>extract-text		</td><td>Extracting text from presentation segments		</td><td>[Documentation](extracttext-woh.md)</td></tr>
<tr><td>publish-engage		</td><td>Distribute and publish media to the engage player	</td><td>[Documentation](publishengage-woh.md)</td></tr>
<tr><td>archive			</td><td>Archive the current state of the mediapackage		</td><td>[Documentation](archive-woh.md)</td></tr>
<tr><td>cleanup			</td><td>Cleanup the working file repository			</td><td>[Documentation](cleanup-woh.md)</td></tr>
<tr><td>zip			</td><td>Create a zipped archive of the current state of the mediapackage </td><td>[Documentation](zip-woh.md)</td></tr>
<tr><td>image-to-video		</td><td>Create a video track from a source image		</td><td>[Documentation](imagetovideo-woh.md)</td></tr>
<tr><td>composite		</td><td>Compose two videos on one canvas.			</td><td>[Documentation](composite-woh.md)</td></tr>
<tr><td>concat			</td><td>Concatenate multiple video tracks into one video track	</td><td>[Documentation](concat-woh.md)</td></tr>
<tr><td>post-mediapackage	</td><td>Send mediapackage to remote service			</td><td>[Documentation](postmediapackage-woh.md)</td></tr>
<tr><td>http-notify		</td><td>Notifies an HTTP endpoint about the process of the workflow </td><td>[Documentation](httpnotify-woh.md)</td></tr>
<tr><td>incident		</td><td>Testing incidents on a dummy job			</td><td>[Documentation](incident-woh.md)</td></tr>
<tr><td>analyze-audio		</td><td>Analyze first audio stream				</td><td>[Documentation](analyzeaudio-woh.md)</td></tr>
<tr><td>normalize-audio		</td><td>Normalize first audio stream				</td><td>[Documentation](normalizeaudio-woh.md)</td></tr>
<tr><td>editor			</td><td>Waiting for user to review, then create a new file based on edit-list </td><td>[Documentation](editor-woh.md)</td></tr>
<tr><td>silence			</td><td>Silence detection on audio of the mediapackage		</td><td>[Documentation](silence-woh.md)</td></tr>
<tr><td>waveform		</td><td>Create a waveform image of the audio of the Mediapackage </td><td>[Documentation](wafeform-woh.md)</td></tr>
</table>
