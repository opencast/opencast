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
package org.opencastproject.scheduler.impl;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Encoding;
import net.fortuna.ical4j.model.parameter.FmtType;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RelatedTo;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Create an iCalendar from the provided dublin core events.
 * 
 */
public class CalendarGenerator {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CalendarGenerator.class);

  /** iCalendar */
  protected Calendar cal;
  /** Series service for Series DC retrieval */
  protected SeriesService seriesService;

  /**
   * Default constructor that creates a CalendarGenerator object
   * 
   * @param seriesService
   *          Series service for retrieving series Dublin Core (if event has one)
   */
  public CalendarGenerator(SeriesService seriesService) {
    cal = new Calendar();
    cal.getProperties().add(new ProdId("Opencast Matterhorn Calendar File 0.5"));
    cal.getProperties().add(Version.VERSION_2_0);
    cal.getProperties().add(CalScale.GREGORIAN);
    this.seriesService = seriesService;
  }

  /**
   * gets the iCalendar creates by this object.
   * 
   * @return the iCalendar
   */
  public Calendar getCalendar() {
    return cal;
  }

  /**
   * Sets an iCalender to work with
   * 
   * @param cal
   *          the iCalendar to set
   */
  public void setCalendar(Calendar cal) {
    this.cal = cal;
  }

  /**
   * Adds an SchedulerEvent as a new entry to this iCalendar
   * 
   * @param catalog
   *          {@link DublinCoreCatalog} of event
   * @param captureAgentMetadata
   *          properties for capture agent metadata
   * 
   * @return true if the event could be added.
   */
  public boolean addEvent(DublinCoreCatalog catalog, Properties captureAgentMetadata) {
    logger.debug("Creating iCal VEvent from SchedulerEvent: {}", catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));

    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(catalog.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasStart()) {
      logger.debug("Couldn't get startdate from event!");
      return false;
    }
    if (!period.hasEnd()) {
      logger.debug("Couldn't get enddate from event!");
      return false;
    }

    DateTime startDate = new DateTime(period.getStart());
    DateTime endDate = new DateTime(period.getEnd());
    if (endDate.before(new Date())) {
      logger.debug("Event has already passed, skipping!");
      return false;
    }
    startDate.setUtc(true);
    endDate.setUtc(true);
    String seriesID = null;

    VEvent event = new VEvent(startDate, endDate, catalog.getFirst(DublinCore.PROPERTY_TITLE));
    try {
      ParameterList pl = new ParameterList();
      for (DublinCoreValue creator : catalog.get(DublinCore.PROPERTY_CREATOR)) {
        pl.add(new Cn(creator.getValue()));
      }
      event.getProperties().add(new Uid(catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER)));

      // TODO Organizer should be URI (email-address?) created fake address
      if (StringUtils.isNotEmpty(catalog.getFirst(DublinCore.PROPERTY_CREATOR))) {
        URI organizer = new URI("mailto", catalog.getFirst(DublinCore.PROPERTY_CREATOR) + "@matterhorn.opencast", null);
        event.getProperties().add(new Organizer(pl, organizer));
      }
      if (StringUtils.isNotEmpty(catalog.getFirst(DublinCore.PROPERTY_DESCRIPTION))) {
        event.getProperties().add(new Description(catalog.getFirst(DublinCore.PROPERTY_DESCRIPTION)));
      }
      // location corresponds to spatial? or is location part of CA configuration?
      if (StringUtils.isNotEmpty(catalog.getFirst(DublinCore.PROPERTY_SPATIAL))) {
        event.getProperties().add(new Location(catalog.getFirst(DublinCore.PROPERTY_SPATIAL)));
      }
      if (StringUtils.isNotEmpty(catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF))) {
        seriesID = catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF);
        event.getProperties().add(new RelatedTo(seriesID));
      }

      ParameterList dcParameters = new ParameterList();
      dcParameters.add(new FmtType("application/xml"));
      dcParameters.add(Value.BINARY);
      dcParameters.add(Encoding.BASE64);
      dcParameters.add(new XParameter("X-APPLE-FILENAME", "episode.xml"));
      Attach metadataAttachment = new Attach(dcParameters, getDublinCoreAsString(catalog).getBytes("UTF-8"));
      event.getProperties().add(metadataAttachment);

      String seriesDC = getSeriesDublinCoreAsString(seriesID);
      if (seriesDC != null) {
        logger.debug("Attaching series {} information to event {}", seriesID,
                catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
        ParameterList sDcParameters = new ParameterList();
        sDcParameters.add(new FmtType("application/xml"));
        sDcParameters.add(Value.BINARY);
        sDcParameters.add(Encoding.BASE64);
        sDcParameters.add(new XParameter("X-APPLE-FILENAME", "series.xml"));
        Attach seriesAttachment = new Attach(sDcParameters, seriesDC.getBytes("UTF-8"));
        event.getProperties().add(seriesAttachment);
      } else {
        logger.debug("No series provided for event {}.", catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      }

      ParameterList caParameters = new ParameterList();
      caParameters.add(new FmtType("application/text"));
      caParameters.add(Value.BINARY);
      caParameters.add(Encoding.BASE64);
      caParameters.add(new XParameter("X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"));
      Attach agentsAttachment = new Attach(caParameters, getCaptureAgentPropertiesAsString(captureAgentMetadata)
              .getBytes("UTF-8"));
      event.getProperties().add(agentsAttachment);

    } catch (Exception e1) {
      logger.error("could not create Calendar entry for Event {}. Reason : {} ",
              catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER), e1);
      return false;
    }
    cal.getComponents().add(event);

    logger.debug("new VEvent = {} ", event.toString());
    return true;
  }

  /**
   * Returns provided Dublin Core as String or null of serialization fails.
   * 
   * @param catalog
   *          {@link DublinCoreCatalog} to be serialized
   * @return string representation of DC
   */
  private String getDublinCoreAsString(DublinCoreCatalog catalog) {
    try {
      Document doc = catalog.toXml();

      Source source = new DOMSource(doc);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);

      return stringWriter.getBuffer().toString().trim();
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse DublinCoreCatalog for Series: {}", e.getMessage());
    } catch (IOException e) {
      logger.error("Could not open DublinCoreCatalog for Series to parse it: {}", e.getMessage());
    } catch (TransformerException e) {
      logger.error("Could not transform DublinCoreCatalog for Series: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Returns series DC associated with this event or null if {@link SeriesService} is not available or does not contain
   * entry for series with specified ID.
   * 
   * @param seriesID
   *          {@link DublinCoreCatalog} to be retrieved
   * @return DC serialized to string or null
   * @throws UnauthorizedException
   *           if the current user is not allowed to view this series
   */
  private String getSeriesDublinCoreAsString(String seriesID) throws UnauthorizedException {
    if (StringUtils.isBlank(seriesID))
      return null;
    if (seriesService == null) {
      logger.warn("No SeriesService available");
      return null;
    }

    DublinCoreCatalog seriesDC;
    try {
      seriesDC = seriesService.getSeries(seriesID);
    } catch (SeriesException e) {
      logger.error("Error loading DublinCoreCatalog for Series: {}", e.getMessage());
      return null;
    } catch (NotFoundException e) {
      logger.warn("Could not find series '" + seriesID + "': {}", e);
      return null;
    }
    return getDublinCoreAsString(seriesDC);
  }

  /**
   * Returns capture agent properties as string.
   * 
   * @param properties
   *          CA properties to be serialized to string
   * @return string representation of properties
   */
  private String getCaptureAgentPropertiesAsString(Properties properties) {
    StringWriter writer = new StringWriter();
    try {
      properties.store(writer, "Capture Agent specific data");
      return writer.toString();
    } catch (IOException e) {
      logger.error("Could not convert Capture Agent Data to String");
    }
    return null;
  }
}
