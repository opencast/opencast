package org.opencastproject.composer.impl.ffmpeg;

import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class FFmpegEmbedderEngine extends AbstractCmdlineEmbedderEngine {

  /** Default location of the ffmepg binary (resembling the installer) */
  public static final String FFMPEG_BINARY_DEFAULT = "ffmpeg";

  /** The ffmpeg commandline suffix */
  public static final String CMD_SUFFIX = "ffmpeg.command";

  private static final String CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path";

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(FFmpegEmbedderEngine.class);

  /**
   * Creates the ffmpeg embedder engine.
   */
  public FFmpegEmbedderEngine() {
    super(FFMPEG_BINARY_DEFAULT);
  }

  public void activate(ComponentContext cc) {
    // Configure ffmpeg
    String path = (String) cc.getBundleContext().getProperty(CONFIG_FFMPEG_PATH);
    if (path == null) {
      logger.debug("DEFAULT " + CONFIG_FFMPEG_PATH + ": " + FFmpegEncoderEngine.FFMPEG_BINARY_DEFAULT);
    } else {
      setBinary(path);
      logger.debug("FFmpegEmbedderEngine config binary: {}", path);
    }
  }

  @Override
  public File embed(File mediaSource, File[] captionSources, String[] captionLanguages, Map<String, String> properties)
          throws EmbedderException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String normalizeLanguage(String language) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void handleEmbedderOutput(String output, File... sourceFiles) {
    // TODO Auto-generated method stub
  }

  @Override
  protected File getOutputFile(Map<String, String> properties) {
    // TODO Auto-generated method stub
    return null;
  }

}
