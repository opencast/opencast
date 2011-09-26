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
package org.opencastproject.videosegmenter.impl.jmf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.AudioDeviceUnavailableEvent;
import javax.media.CachingControlEvent;
import javax.media.ConfigureCompleteEvent;
import javax.media.ConnectionErrorEvent;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataLostErrorEvent;
import javax.media.DataStarvedEvent;
import javax.media.DeallocateEvent;
import javax.media.DurationUpdateEvent;
import javax.media.EndOfMediaEvent;
import javax.media.InternalErrorEvent;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RateChangeEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.RestartingEvent;
import javax.media.SizeChangeEvent;
import javax.media.StartEvent;
import javax.media.StopAtTimeEvent;
import javax.media.StopByRequestEvent;
import javax.media.StopEvent;
import javax.media.StopTimeChangeEvent;
import javax.media.TransitionEvent;
import javax.media.format.FormatChangeEvent;

/**
 * This implementation of the <code>ControllerListener</code> is listening for events from the Java Media Framework
 * Player, which are structure like this:
 * <pre>
 *   ControllerEvent
 *    AudioDeviceUnavailableEvent
 *    CachingControlEvent
 *    ControllerClosedEvent
 *        ControllerErrorEvent
 *            ConnectionErrorEvent
 *            InternalErrorEvent
 *            ResourceUnavailableEvent
 *        DataLostErrorEvent
 *    DurationUpdateEvent
 *    FormatChangeEvent
 *        SizeChangeEvent
 *    MediaTimeSetEvent
 *    RateChangeEvent
 *    StopTimeChangeEvent
 *    TransitionEvent
 *        ConfigureCompleteEvent
 *        RealizeCompleteEvent
 *        PrefetchCompleteEvent
 *        StartEvent
 *        StopEvent
 *            DataStarvedEvent
 *            DeallocateEvent
 *            EndOfMediaEvent
 *            RestartingEvent
 *            StopAtTimeEvent
 *            StopByRequestEvent
 * </pre>
 */
public class PlayerListener implements ControllerListener {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PlayerListener.class);

  /** Object used for wait synchronization */
  protected Object waitSync = new Object();

  /** Flag to indicate whether the last state transition was successful */
  protected boolean stateTransitionOK = true;
  
  /** The player that as being listened to */
  protected Player player = null;

  /**
   * Creates a new player listener for the realized player <code>p</code>.
   * 
   * @param p
   *          the player
   */
  public PlayerListener(Player p) {
    player = p;
  }

  /**
   * Returns as soon as there is a successful state transition in the player that matches the requested state.
   * 
   * @param state
   *          the controller state
   * @return <code>true</code> if the transition was successful
   */
  public boolean waitForState(int state) {
    synchronized (waitSync) {
      try {
        while (player.getState() < state && stateTransitionOK) {
          waitSync.wait();
        }
      } catch (Exception e) {
        logger.error("Error waiting for player to transition into state " + state, e);
        return false;
      }
    }
    return stateTransitionOK;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
   */
  public void controllerUpdate(ControllerEvent evt) {
    if (evt instanceof AudioDeviceUnavailableEvent) {
      logger.debug("JMF controllerUpdate: " + "AudioDeviceUnavailableEvent");
    } else if (evt instanceof CachingControlEvent) {
      logger.debug("JMF controllerUpdate: " + "CachingControlEvent");
    } else if (evt instanceof ControllerClosedEvent) {
      logger.debug("JMF controllerUpdate: " + "ControllerClosedEvent");
      if (evt instanceof ControllerErrorEvent) {
        logger.debug("ControllerErrorEvent");
        if (evt instanceof ConnectionErrorEvent) {
          logger.debug("-ConnectionErrorEvent");
        } else if (evt instanceof InternalErrorEvent) {
          logger.debug("-InternalErrorEvent");
        } else if (evt instanceof ResourceUnavailableEvent) {
          logger.debug("-ResourceUnavailableEvent");
          synchronized (waitSync) {
            stateTransitionOK = false;
            waitSync.notifyAll();
          }
        } else {
          logger.debug("-??: " + evt.getClass().getName());
        }
      } else if (evt instanceof DataLostErrorEvent) {
        logger.debug("DataLostErrorEvent");
      } else {
        logger.debug("??: " + evt.getClass().getName());
      }
    } else if (evt instanceof DurationUpdateEvent) {
      logger.debug("JMF controllerUpdate: " + "DurationUpdateEvent");
    } else if (evt instanceof FormatChangeEvent) {
      logger.debug("JMF controllerUpdate: " + "FormatChangeEvent");
      if (evt instanceof SizeChangeEvent) {
        logger.debug("SizeChangeEvent");
      } else {
        logger.debug("??: " + evt.getClass().getName());
      }
    } else if (evt instanceof MediaTimeSetEvent) {
      logger.debug("JMF controllerUpdate: " + "MediaTimeSetEvent");
    } else if (evt instanceof RateChangeEvent) {
      logger.debug("JMF controllerUpdate: " + "RateChangeEvent");
    } else if (evt instanceof StopTimeChangeEvent) {
      logger.debug("JMF controllerUpdate: " + "StopTimeChangeEvent");
    } else if (evt instanceof TransitionEvent) {
      logger.debug("JMF controllerUpdate: " + "TransitionEvent");
      if (evt instanceof ConfigureCompleteEvent) {
        logger.debug("ConfigureCompleteEvent");
        synchronized (waitSync) {
          stateTransitionOK = true;
          waitSync.notifyAll();
        }
      } else if (evt instanceof RealizeCompleteEvent) {
        logger.debug("RealizeCompleteEvent");
        synchronized (waitSync) {
          stateTransitionOK = true;
          waitSync.notifyAll();
        }
      } else if (evt instanceof PrefetchCompleteEvent) {
        logger.debug("PrefetchCompleteEvent");
        synchronized (waitSync) {
          stateTransitionOK = true;
          waitSync.notifyAll();
        }
      } else if (evt instanceof StartEvent) {
        logger.debug("StartEvent");
      } else if (evt instanceof StopEvent) {
        logger.debug("StopEvent");
        if (evt instanceof DataStarvedEvent) {
          logger.debug("-DataStarvedEvent");
        } else if (evt instanceof DeallocateEvent) {
          logger.debug("-DeallocateEvent");
        } else if (evt instanceof EndOfMediaEvent) {
          logger.debug("-EndOfMediaEvent");
        } else if (evt instanceof RestartingEvent) {
          logger.debug("-RestartingEvent");
        } else if (evt instanceof StopAtTimeEvent) {
          logger.debug("-StopAtTimeEvent");
        } else if (evt instanceof StopByRequestEvent) {
          logger.debug("-StopByRequestEvent");
        } else {
          logger.debug("-??: " + evt.getClass().getName());
        }
      } else {
        logger.debug("??: " + evt.getClass().getName());
      }
    } else {
      logger.debug("JMF controllerUpdate ?? : " + evt.getClass().getName());
    }
  }

}
