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
package org.opencastproject.scheduler.impl.solr;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Temporal;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.impl.SchedulerServiceDatabaseException;
import org.opencastproject.scheduler.impl.SchedulerServiceIndex;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.SolrUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements {@link SchedulerServiceIndex}.
 */
public class SchedulerServiceSolrIndex implements SchedulerServiceIndex {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceSolrIndex.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.scheduler.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.scheduler.solr.dir";
  
  /** the default scheduler index suffix */
  public static final String SOLR_ROOT_SUFFIX = "/schedulerindex";

  /** Delimeter used for concatenating multivalued fields for sorting fields in solr */
  public static final String SOLR_MULTIVALUED_DELIMETER = "; ";

  /** Connection to the solr server. Solr is used to search for events. The event data are stored as xml files. */
  private SolrServer solrServer = null;

  /** The root directory to use for solr config and data files */
  protected String solrRoot = null;

  /** The URL to connect to a remote solr server */
  protected URL solrServerUrl = null;

  /** Dublin core service */
  protected DublinCoreCatalogService dcService;

  /** Whether indexing is synchronous or asynchronous */
  protected boolean synchronousIndexing;

  /** Executor used for asynchronous indexing */
  protected ExecutorService indexingExecutor;

  /**
   * No-argument constructor for OSGi declarative services.
   */
  public SchedulerServiceSolrIndex() {
  }

  /**
   * Constructs Solr index with specified storage directory.
   */
  public SchedulerServiceSolrIndex(String storageDirectory) {
    solrRoot = storageDirectory;
  }

  /**
   * OSGi callback for setting Dublin core service.
   * 
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Callback from the OSGi environment on component registration. Retrieves location of the solr index.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null) {
      if (solrRoot == null)
        throw new IllegalStateException("Storage dir must be set");
      // default to synchronous indexing
      synchronousIndexing = true;
    } else {
      String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
      if (solrServerUrlConfig != null) {
        try {
          solrServerUrl = new URL(solrServerUrlConfig);
        } catch (MalformedURLException e) {
          throw new IllegalStateException("Unable to connect to solr at " + solrServerUrlConfig, e);
        }
      } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
        solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
      } else {
        String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
        if (storageDir == null)
          throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
        solrRoot = PathSupport.concat(storageDir + SOLR_ROOT_SUFFIX, "series");
      }

      Object syncIndexingConfig = cc.getProperties().get("synchronousIndexing");
      if ((syncIndexingConfig != null) && ((syncIndexingConfig instanceof Boolean))) {
        synchronousIndexing = ((Boolean) syncIndexingConfig).booleanValue();
      } else {
        synchronousIndexing = true;
      }
    }
    activate();
  }

  /**
   * OSGi callback for deactivation.
   * 
   * @param cc
   *          the component context
   */
  public void deactivate(ComponentContext cc) {
    deactivate();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#activate()
   */
  @Override
  public void activate() {
    // Set up the solr server
    if (solrServerUrl != null) {
      solrServer = SolrServerFactory.newRemoteInstance(solrServerUrl);
    } else {
      try {
        setupSolr(new File(solrRoot));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      } catch (SolrServerException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      }
    }

    // set up indexing
    if (this.synchronousIndexing) {
      logger.debug("Events will be added to the search index synchronously");
    } else {
      logger.debug("Events will be added to the search index asynchronously");
      indexingExecutor = Executors.newSingleThreadExecutor();
    }
  }

  /**
   * Prepares the embedded solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  public void setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.debug("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.debug("solr search index found at {}", solrConfigDir);
    } else {
      logger.debug("solr config directory doesn't exist.  Creating {}", solrConfigDir);
      FileUtils.forceMkdir(solrConfigDir);
    }

    // Make sure there is a configuration in place
    copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);

    // Test for the existence of a data directory
    File solrDataDir = new File(solrRoot, "data");
    if (!solrDataDir.exists()) {
      FileUtils.forceMkdir(solrDataDir);
    }

    // Test for the existence of the index. Note that an empty index directory will prevent solr from
    // completing normal setup.
    File solrIndexDir = new File(solrDataDir, "index");
    if (solrIndexDir.exists() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    solrServer = SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#deactivate()
   */
  @Override
  public void deactivate() {
    SolrServerFactory.shutdown(solrServer);
  }

  // TODO: generalize this method
  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = SchedulerServiceSolrIndex.class.getResourceAsStream(classpath);
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying " + classpath + " to " + file);
      fos = new FileOutputStream(file);
      IOUtils.copy(in, fos);
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#index(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public void index(DublinCoreCatalog dc) throws SchedulerServiceDatabaseException {

    // check if we are updating
    // if that's the case retrieve CA properties and add them
    SolrDocument retrievedDoc = retrieveDocumentById(dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    final SolrInputDocument doc = createDocument(dc);
    if (retrievedDoc != null && retrievedDoc.containsKey(SolrFields.CA_PROPERTIES)) {
      doc.setField(SolrFields.CA_PROPERTIES, retrievedDoc.getFirstValue(SolrFields.CA_PROPERTIES));
    }

    if (synchronousIndexing) {
      try {
        synchronized (solrServer) {
          solrServer.add(doc);
          solrServer.commit();
        }
      } catch (Exception e) {
        throw new SchedulerServiceDatabaseException("Unable to index event", e);
      }
    } else {
      indexingExecutor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            synchronized (solrServer) {
              solrServer.add(doc);
              solrServer.commit();
            }
          } catch (Exception e) {
            logger.warn("Unable to index event {}: {}", doc.getFieldValue(SolrFields.ID_KEY), e.getMessage());
          }
        }
      });
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#index(java.lang.String, java.util.Properties)
   */
  @Override
  public void index(String eventId, Properties captureAgentProperties) throws NotFoundException,
          SchedulerServiceDatabaseException {
    SolrDocument result = retrieveDocumentById(eventId);
    if (result == null) {
      logger.warn("No event exists with event ID {}", eventId);
      throw new NotFoundException("Event with ID " + eventId + " does not exist.");
    }

    String serializedCA;
    try {
      serializedCA = serializeProperties(captureAgentProperties);
    } catch (IOException e) {
      throw new SchedulerServiceDatabaseException(e);
    }
    final SolrInputDocument doc = ClientUtils.toSolrInputDocument(result);
    doc.setField(SolrFields.CA_PROPERTIES, serializedCA);
    doc.setField(SolrFields.LAST_MODIFIED, new Date());

    if (synchronousIndexing) {
      try {
        synchronized (solrServer) {
          solrServer.add(doc);
          solrServer.commit();
        }
      } catch (Exception e) {
        throw new SchedulerServiceDatabaseException("Unable to index event capture agent metadata", e);
      }
    } else {
      indexingExecutor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            synchronized (solrServer) {
              solrServer.add(doc);
              solrServer.commit();
            }
          } catch (Exception e) {
            logger.warn("Unable to index event {} capture agent metadata: {}", doc.getFieldValue(SolrFields.ID_KEY),
                    e.getMessage());
          }
        }
      });
    }
  }

  /**
   * Creates solr document for inserting into solr index.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be stored in index
   * @return {@link SolrInputDocument} created out of Dublin core
   */
  protected SolrInputDocument createDocument(DublinCoreCatalog dc) {
    final SolrInputDocument doc = new SolrInputDocument();

    doc.setField(SolrFields.ID_KEY, dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    try {
      doc.setField(SolrFields.XML_KEY, serializeDublinCore(dc));
    } catch (IOException e1) {
      throw new IllegalArgumentException(e1);
    }

    // single valued fields
    if (dc.hasValue(DublinCore.PROPERTY_TITLE)) {
      doc.setField(SolrFields.TITLE_KEY, dc.getFirst(DublinCore.PROPERTY_TITLE));
    }
    if (dc.hasValue(DublinCore.PROPERTY_CREATED)) {
      Temporal temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_CREATED).get(0));
      temporal.fold(new Temporal.Match<Void>() {
        @Override
        public Void period(DCMIPeriod period) {
          doc.setField(SolrFields.CREATED_KEY, period.getStart());
          return null;
        }

        @Override
        public Void instant(Date instant) {
          doc.setField(SolrFields.CREATED_KEY, instant);
          return null;
        }

        @Override
        public Void duration(long duration) {
          throw new IllegalArgumentException("Dublin core dc:created is neither a date nor a period");
        }
      });
    }
    if (dc.hasValue(DublinCore.PROPERTY_AVAILABLE)) {
      Temporal temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_AVAILABLE).get(0));
      temporal.fold(new Temporal.Match<Void>() {
        @Override
        public Void period(DCMIPeriod period) {
          if (period.hasStart()) {
            doc.setField(SolrFields.AVAILABLE_FROM_KEY, period.getStart());
          }
          if (period.hasEnd()) {
            doc.setField(SolrFields.AVAILABLE_TO_KEY, period.getEnd());
          }
          return null;
        }

        @Override
        public Void instant(Date instant) {
          doc.setField(SolrFields.AVAILABLE_FROM_KEY, instant);
          return null;
        }

        @Override
        public Void duration(long duration) {
          throw new IllegalArgumentException("Dublin core field dc:available is neither a date nor a period");
        }
      });
    }
    if (dc.hasValue(DublinCore.PROPERTY_TEMPORAL)) {
      Temporal temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_TEMPORAL).get(0));
      temporal.fold(new Temporal.Match<Void>() {
        @Override
        public Void period(DCMIPeriod period) {
          if (period.hasStart()) {
            doc.setField(SolrFields.STARTS_KEY, SolrUtils.serializeDate(period.getStart()));
          }
          if (period.hasEnd()) {
            doc.setField(SolrFields.ENDS_KEY, SolrUtils.serializeDate(period.getEnd()));
          }
          return null;
        }

        @Override
        public Void instant(Date instant) {
          doc.setField(SolrFields.STARTS_KEY, instant);
          return null;
        }

        @Override
        public Void duration(long duration) {
          throw new IllegalArgumentException("Dublin core field dc:temporal is neither a date nor a period");
        }
      });
    }

    // multivalued fields
    addMultiValuedFieldToSolrDocument(doc, SolrFields.SUBJECT_KEY, dc.get(DublinCore.PROPERTY_SUBJECT));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.CREATOR_KEY, dc.get(DublinCore.PROPERTY_CREATOR));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.PUBLISHER_KEY, dc.get(DublinCore.PROPERTY_PUBLISHER));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.CONTRIBUTOR_KEY, dc.get(DublinCore.PROPERTY_CONTRIBUTOR));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.ABSTRACT_KEY, dc.get(DublinCore.PROPERTY_ABSTRACT));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.DESCRIPTION_KEY, dc.get(DublinCore.PROPERTY_DESCRIPTION));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.LANGUAGE_KEY, dc.get(DublinCore.PROPERTY_LANGUAGE));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.RIGHTS_HOLDER_KEY, dc.get(DublinCore.PROPERTY_RIGHTS_HOLDER));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.SPATIAL_KEY, dc.get(DublinCore.PROPERTY_SPATIAL));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.IS_PART_OF_KEY, dc.get(DublinCore.PROPERTY_IS_PART_OF));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.REPLACES_KEY, dc.get(DublinCore.PROPERTY_REPLACES));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.TYPE_KEY, dc.get(DublinCore.PROPERTY_TYPE));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.ACCESS_RIGHTS_KEY, dc.get(DublinCore.PROPERTY_ACCESS_RIGHTS));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.LICENSE_KEY, dc.get(DublinCore.PROPERTY_LICENSE));

    // add timestamp
    doc.setField(SolrFields.LAST_MODIFIED, new Date());

    return doc;
  }

  /**
   * Add field to solr document that can contain multiple values. For sorting field, those values are concatenated and
   * multivalued field delimiter is used.
   * 
   * @param doc
   *          {@link SolrInputDocument} for fields to be added to
   * @param solrField
   *          name of the solr field to add value. For sorting field "_sort" is appended
   * @param dcValues
   *          List of Dublin core values to be added to solr document
   */
  private void addMultiValuedFieldToSolrDocument(SolrInputDocument doc, String solrField, List<DublinCoreValue> dcValues) {
    if (!dcValues.isEmpty()) {
      List<String> values = new LinkedList<String>();
      StringBuilder builder = new StringBuilder();
      values.add(dcValues.get(0).getValue());
      builder.append(dcValues.get(0).getValue());
      for (int i = 1; i < dcValues.size(); i++) {
        values.add(dcValues.get(i).getValue());
        builder.append(SOLR_MULTIVALUED_DELIMETER);
        builder.append(dcValues.get(i).getValue());
      }
      doc.addField(solrField, values);
      doc.setField(solrField + "_sort", builder.toString());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#count()
   */
  @Override
  public long count() throws SchedulerServiceDatabaseException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery("*:*"));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new SchedulerServiceDatabaseException(e);
    }
  }

  /**
   * Retrieves SolrDocument by specified ID. If such document does not exist, null is returned.
   * 
   * @param id
   *          ID of document to be retrieved
   * @return matching SolrDocument or null
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  private SolrDocument retrieveDocumentById(String id) throws SchedulerServiceDatabaseException {
    String solrQueryString = SolrFields.ID_KEY + ":" + ClientUtils.escapeQueryChars(id);
    SolrQuery q = new SolrQuery(solrQueryString);
    QueryResponse response;
    try {
      response = solrServer.query(q);
    } catch (SolrServerException e) {
      logger.error("Could not perform event retrieval: {}", e);
      throw new SchedulerServiceDatabaseException(e);
    }
    return response.getResults().isEmpty() ? null : response.getResults().get(0);
  }

  /**
   * Appends query parameters to a solr query
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append(key);
    sb.append(":");
    sb.append(ClientUtils.escapeQueryChars(value));
    return sb;
  }

  /**
   * Appends query parameters to a solr query in a way that they are found even though they are not treated as a full
   * word in solr.
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder appendFuzzy(StringBuilder sb, String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append("(");
    sb.append(key).append(":").append(ClientUtils.escapeQueryChars(value));
    sb.append(" OR ");
    sb.append(key).append(":*").append(ClientUtils.escapeQueryChars(value)).append("*");
    sb.append(")");
    return sb;
  }

  /**
   * Appends query parameters to a solr query
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, Date startDate, Date endDate) {
    if (StringUtils.isBlank(key) || (startDate == null && endDate == null)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    if (startDate == null)
      startDate = new Date(0);
    if (endDate == null)
      endDate = new Date(Long.MAX_VALUE);
    sb.append(key);
    sb.append(":");
    sb.append(SolrUtils.serializeDateRange(startDate, endDate));
    return sb;
  }

  /**
   * Builds a solr search query from a {@link org.opencastproject.series.api.SeriesQuery}.
   * 
   * @param query
   *          the series query
   * @return the solr query string
   */
  protected String buildSolrQueryString(SchedulerQuery query) {
    StringBuilder sb = new StringBuilder();
    append(sb, SolrFields.ID_KEY, query.getIdentifier());
    append(sb, SolrFields.IS_PART_OF_KEY, query.getSeriesId());
    appendFuzzy(sb, SolrFields.TITLE_KEY, query.getTitle());
    appendFuzzy(sb, SolrFields.FULLTEXT_KEY, query.getText());
    appendFuzzy(sb, SolrFields.CREATOR_KEY, query.getCreator());
    appendFuzzy(sb, SolrFields.CONTRIBUTOR_KEY, query.getContributor());
    append(sb, SolrFields.LANGUAGE_KEY, query.getLanguage());
    append(sb, SolrFields.LICENSE_KEY, query.getLicense());
    appendFuzzy(sb, SolrFields.SUBJECT_KEY, query.getSubject());
    appendFuzzy(sb, SolrFields.ABSTRACT_KEY, query.getAbstract());
    appendFuzzy(sb, SolrFields.DESCRIPTION_KEY, query.getDescription());
    appendFuzzy(sb, SolrFields.PUBLISHER_KEY, query.getPublisher());
    append(sb, SolrFields.SPATIAL_KEY, query.getSpatial());
    appendFuzzy(sb, SolrFields.RIGHTS_HOLDER_KEY, query.getRightsHolder());
    appendFuzzy(sb, SolrFields.SUBJECT_KEY, query.getSubject());
    append(sb, SolrFields.CREATED_KEY, query.getCreatedFrom(), query.getCreatedTo());
    append(sb, SolrFields.STARTS_KEY, query.getStartsFrom(), query.getStartsTo());
    append(sb, SolrFields.ENDS_KEY, query.getEndsFrom(), query.getEndsTo());
    
    if (query.getIdsList() != null) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("(");
      List<String> ids = query.getIdsList();
      for (int i = 0; i < ids.size(); i++) {
        String id = ids.get(i);
        if (StringUtils.isNotEmpty(id)) {
          sb.append(SolrFields.ID_KEY);
          sb.append(":");
          sb.append(ClientUtils.escapeQueryChars(id));
        }
        if (i < ids.size() - 1) {
          sb.append(" OR ");
        }
      }
      sb.append(")");
    }

    // If we're looking for anything, set the query to a wildcard search
    if (sb.length() == 0) {
      sb.append("*:*");
    }

    logger.debug("Solr query: " + sb.toString());
    return sb.toString();
  }

  /**
   * Returns the search index' field name that corresponds to the sort field.
   * 
   * @param sort
   *          the sort field
   * @return the field name in the search index
   */
  protected String getSortField(SchedulerQuery.Sort sort) {
    switch (sort) {
    case ABSTRACT:
      return SolrFields.ABSTRACT_KEY;
    case ACCESS:
      return SolrFields.ACCESS_RIGHTS_KEY;
    case AVAILABLE_FROM:
      return SolrFields.AVAILABLE_FROM_KEY;
    case AVAILABLE_TO:
      return SolrFields.AVAILABLE_TO_KEY;
    case CONTRIBUTOR:
      return SolrFields.CONTRIBUTOR_KEY;
    case CREATED:
      return SolrFields.CREATED_KEY;
    case CREATOR:
      return SolrFields.CREATOR_KEY;
    case DESCRIPTION:
      return SolrFields.DESCRIPTION_KEY;
    case IS_PART_OF:
      return SolrFields.IS_PART_OF_KEY;
    case LANGUAGE:
      return SolrFields.LANGUAGE_KEY;
    case LICENCE:
      return SolrFields.LICENSE_KEY;
    case PUBLISHER:
      return SolrFields.PUBLISHER_KEY;
    case REPLACES:
      return SolrFields.REPLACES_KEY;
    case RIGHTS_HOLDER:
      return SolrFields.RIGHTS_HOLDER_KEY;
    case SPATIAL:
      return SolrFields.SPATIAL_KEY;
    case SUBJECT:
      return SolrFields.SUBJECT_KEY;
    case TITLE:
      return SolrFields.TITLE_KEY;
    case TYPE:
      return SolrFields.TYPE_KEY;
    case EVENT_START:
      return SolrFields.STARTS_KEY;
    default:
      throw new IllegalArgumentException("No mapping found between sort field and index");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.scheduler.impl.SchedulerServiceIndex#search(org.opencastproject.scheduler.api.SchedulerQuery)
   */
  @Override
  public List<DublinCoreCatalog> search(SchedulerQuery query) throws SchedulerServiceDatabaseException {
    SolrQuery solrQuery = new SolrQuery();
    if (query == null) {
      query = new SchedulerQuery();
    }
    String solrQueryString = buildSolrQueryString(query);
    solrQuery.setQuery(solrQueryString);
    solrQuery.setRows(Integer.MAX_VALUE);

    if (query.getSort() != null) {
      SolrQuery.ORDER order = query.isSortAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
      solrQuery.addSortField(getSortField(query.getSort()) + "_sort", order);
    }

    if (!SchedulerQuery.Sort.EVENT_START.equals(query.getSort())) {
      solrQuery.addSortField(getSortField(SchedulerQuery.Sort.EVENT_START) + "_sort", SolrQuery.ORDER.asc);
    }

    List<DublinCoreCatalog> resultList;

    try {
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList items = response.getResults();

      resultList = new LinkedList<DublinCoreCatalog>();

      // Iterate through the results
      for (SolrDocument doc : items) {
        DublinCoreCatalog dc = parseDublinCore((String) doc.get(SolrFields.XML_KEY));
        resultList.add(dc);
      }
    } catch (Exception e) {
      throw new SchedulerServiceDatabaseException(e);
    }
    return resultList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#delete(java.lang.String)
   */
  @Override
  public void delete(final String id) throws SchedulerServiceDatabaseException {
    if (synchronousIndexing) {
      try {
        synchronized (solrServer) {
          solrServer.deleteById(id);
          solrServer.commit();
        }
      } catch (Exception e) {
        throw new SchedulerServiceDatabaseException(e);
      }
    } else {
      indexingExecutor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            synchronized (solrServer) {
              solrServer.deleteById(id);
              solrServer.commit();
            }
          } catch (Exception e) {
            logger.warn("Could not delete from index event {}: {}", id, e.getMessage());
          }
        }
      });
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#getDublinCore(java.lang.String)
   */
  @Override
  public DublinCoreCatalog getDublinCore(String eventId) throws SchedulerServiceDatabaseException, NotFoundException {
    SolrDocument result = retrieveDocumentById(eventId);
    if (result == null) {
      logger.info("No event exists with ID {}", eventId);
      throw new NotFoundException("Event with ID " + eventId + " does not exist");
    } else {
      String dcXML = (String) result.get(SolrFields.XML_KEY);
      DublinCoreCatalog dc;
      try {
        dc = parseDublinCore(dcXML);
      } catch (IOException e) {
        logger.error("Could not parse Dublin core: {}", e);
        throw new SchedulerServiceDatabaseException(e);
      }
      return dc;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#getCaptureAgentProperties(java.lang.String)
   */
  @Override
  public Properties getCaptureAgentProperties(String eventId) throws SchedulerServiceDatabaseException,
          NotFoundException {
    SolrDocument result = retrieveDocumentById(eventId);
    if (result == null) {
      logger.info("No event exists with ID {}", eventId);
      throw new NotFoundException("Event with ID " + eventId + " does not exist");
    } else {
      String serializedCA = (String) result.get(SolrFields.CA_PROPERTIES);
      Properties caProperties = null;
      if (serializedCA != null) {
        try {
          caProperties = parseProperties(serializedCA);
        } catch (IOException e) {
          logger.error("Could not parse capture agent properties: {}", e);
          throw new SchedulerServiceDatabaseException(e);
        }
      }
      return caProperties;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceIndex#getLastModifiedDate(java.lang.String)
   */
  @Override
  public Date getLastModifiedDate(SchedulerQuery filter) throws SchedulerServiceDatabaseException {
    String solrQueryString = buildSolrQueryString(filter);
    SolrQuery q = new SolrQuery(solrQueryString);
    q.addSortField(SolrFields.LAST_MODIFIED + "_sort", SolrQuery.ORDER.desc);
    q.setRows(1);
    QueryResponse response;
    try {
      response = solrServer.query(q);
    } catch (SolrServerException e) {
      logger.error("Could not complete query request: {}", e);
      throw new SchedulerServiceDatabaseException(e);
    }
    if (response.getResults().isEmpty()) {
      logger.info("No events scheduled for {}", filter);
      return null;
    }

    return (Date) response.getResults().get(0).get(SolrFields.LAST_MODIFIED);
  }

  /**
   * Clears the index of all series instances.
   */
  public void clear() throws SchedulerServiceDatabaseException {
    if (synchronousIndexing) {
      try {
        synchronized (solrServer) {
          solrServer.deleteByQuery("*:*");
          solrServer.commit();
        }
      } catch (Exception e) {
        throw new SchedulerServiceDatabaseException(e);
      }
    } else {
      indexingExecutor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            synchronized (solrServer) {
              solrServer.deleteByQuery("*:*");
              solrServer.commit();
            }
          } catch (Exception e) {
            logger.warn("Could not clear index: {}", e.getMessage());
          }
        }
      });
    }
  }

  /**
   * Serializes Dublin core and returns serialized string.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   * @return String representation of serialized Dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /**
   * Parses Dublin core stored as string.
   * 
   * @param dcXML
   *          string representation of Dublin core
   * @return parsed {@link DublinCoreCatalog}
   * @throws IOException
   *           if parsing fails
   */
  private DublinCoreCatalog parseDublinCore(String dcXML) throws IOException {
    DublinCoreCatalog dc = dcService.load(new ByteArrayInputStream(dcXML.getBytes("UTF-8")));
    return dc;
  }

  /**
   * Serializes Properties to String.
   * 
   * @param caProperties
   *          Properties to be serialized
   * @return serialized properties
   * @throws IOException
   *           if serialization fails
   */
  private String serializeProperties(Properties caProperties) throws IOException {
    StringWriter writer = new StringWriter();
    caProperties.store(writer, "Capture Agent specific data");
    return writer.toString();
  }

  /**
   * Parses Properties represented as String.
   * 
   * @param serializedProperties
   *          properties to be parsed.
   * @return parsed properties
   * @throws IOException
   *           if parsing fails
   */
  private Properties parseProperties(String serializedProperties) throws IOException {
    Properties caProperties = new Properties();
    caProperties.load(new StringReader(serializedProperties));
    return caProperties;
  }
}
