/*
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
package org.opencastproject.oaipmh.server;

import static java.lang.String.format;
import static org.opencastproject.oaipmh.OaiPmhUtil.toOaiRepresentation;
import static org.opencastproject.oaipmh.OaiPmhUtil.toUtc;
import static org.opencastproject.oaipmh.persistence.QueryBuilder.queryRepo;
import static org.opencastproject.oaipmh.server.Functions.addDay;
import static org.opencastproject.oaipmh.server.Functions.asDate;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.OaiPmhUtil;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinition;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinitionFilter;
import org.opencastproject.oaipmh.persistence.OaiPmhSetDefinitionImpl;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An OAI-PMH protocol compliant repository.
 * <p>
 * Currently supported:
 * <ul>
 * <li></li>
 * </ul>
 * <p>
 * Currently <em>not</em> supported:
 * <ul>
 * <li><a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#deletion">deletions</a></li>
 * <li><a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Set">sets</a></li>
 * <li>&lt;about&gt; containers in records, see section <a
 * href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Record">2.5. Record</a></li>
 * <li>
 * resumption tokens do not report about their expiration date; see section <a
 * href="http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl">3.5. Flow Control</a></li>
 * </ul>
 */
// todo - malformed date parameter must produce a BadArgument error - if a date parameter has a finer granularity than
//        supported by the repository this must produce a BadArgument error
public abstract class OaiPmhRepository implements ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhRepository.class);
  private static final OaiDcMetadataProvider OAI_DC_METADATA_PROVIDER = new OaiDcMetadataProvider();
  private static final String OAI_NS = OaiPmhConstants.OAI_2_0_XML_NS;

  private static final String CONF_KEY_SET_PREFIX = "set.";
  private static final String CONF_KEY_SET_SETSPEC_SUFFIX = ".setSpec";
  private static final String CONF_KEY_SET_NAME_SUFFIX = ".name";
  private static final String CONF_KEY_SET_DESCRIPTION_SUFFIX = ".description";
  private static final String CONF_KEY_SET_FILTER_INFIX = ".filter.";
  private static final String CONF_KEY_SET_FILTER_FLAVOR_SUFFIX = ".flavor";
  private static final String CONF_KEY_SET_FILTER_CONTAINS_SUFFIX = ".contains";
  private static final String CONF_KEY_SET_FILTER_CONTAINSNOT_SUFFIX = ".containsnot";
  private static final String CONF_KEY_SET_FILTER_MATCH_SUFFIX = ".match";


  public abstract Granularity getRepositoryTimeGranularity();

  /** Display name of the OAI-PMH repository. */
  public abstract String getRepositoryName();

  /** Repository ID. */
  public abstract String getRepositoryId();

  public abstract OaiPmhDatabase getPersistence();

  public abstract String getAdminEmail();

  private List<OaiPmhSetDefinition> sets = new ArrayList<>();

  /**
   * Parse service configuration file.
   *
   * @param properties
   *        Service configuration as dictionary
   * @throws ConfigurationException
   *        If there is a problem within get configuration
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }

    // Wipe set configuration in case some got removed
    sets = new ArrayList<>();
    List<String> confKeys = EnumerationUtils.toList(properties.keys());
    for (String confKey : confKeys) {
      if (confKey.startsWith(CONF_KEY_SET_PREFIX) && confKey.endsWith(CONF_KEY_SET_SETSPEC_SUFFIX)) {
        String confKeyPrefix = confKey.replace(CONF_KEY_SET_SETSPEC_SUFFIX, "");
        String setSpec = (String) properties.get(confKey);
        String setSpecName = (String) properties.get(confKeyPrefix + CONF_KEY_SET_NAME_SUFFIX);
        String setDescription = null;
        if (confKey.contains(confKeyPrefix + CONF_KEY_SET_DESCRIPTION_SUFFIX)) {
          setDescription = (String) properties.get(confKeyPrefix + CONF_KEY_SET_DESCRIPTION_SUFFIX);
        }
        try {
          OaiPmhSetDefinitionImpl setDefinition = OaiPmhSetDefinitionImpl.build(setSpec, setSpecName, setDescription);
          List<String> confKeyFilterNames = confKeys.stream()
              .filter(key -> key.startsWith(confKeyPrefix + CONF_KEY_SET_FILTER_INFIX)
                  && key.endsWith(CONF_KEY_SET_FILTER_FLAVOR_SUFFIX))
              .map(key -> key.replace(confKeyPrefix + CONF_KEY_SET_FILTER_INFIX, "")
                  .replace(CONF_KEY_SET_FILTER_FLAVOR_SUFFIX, ""))
              .distinct().collect(Collectors.toList());
          for (String filterName : confKeyFilterNames) {
            String setSpecFilterFlavor = (String) properties
                .get(confKeyPrefix + CONF_KEY_SET_FILTER_INFIX + filterName + CONF_KEY_SET_FILTER_FLAVOR_SUFFIX);
            List<String> confKeyCriteria = confKeys.stream()
                .filter(key -> key.startsWith(confKeyPrefix + CONF_KEY_SET_FILTER_INFIX + filterName)
                    && (key.endsWith(CONF_KEY_SET_FILTER_CONTAINS_SUFFIX)
                    || key.endsWith(CONF_KEY_SET_FILTER_CONTAINSNOT_SUFFIX)
                    || key.endsWith(CONF_KEY_SET_FILTER_MATCH_SUFFIX)))
                .distinct().collect(Collectors.toList());
            for (String confKeyCriterion : confKeyCriteria) {
              String criterion = null;
              if (confKeyCriterion.endsWith(CONF_KEY_SET_FILTER_CONTAINS_SUFFIX)) {
                criterion = OaiPmhSetDefinitionFilter.CRITERION_CONTAINS;
              } else if (confKeyCriterion.endsWith(CONF_KEY_SET_FILTER_CONTAINSNOT_SUFFIX)) {
                criterion = OaiPmhSetDefinitionFilter.CRITERION_CONTAINSNOT;
              } else if (confKeyCriterion.endsWith(CONF_KEY_SET_FILTER_MATCH_SUFFIX)) {
                criterion = OaiPmhSetDefinitionFilter.CRITERION_MATCH;
              } else {
                logger.warn("Configuration key {} not valid.", confKeyCriterion);
                continue;
              }
              setDefinition.addFilter(filterName, setSpecFilterFlavor, criterion,
                  (String) properties.get(confKeyCriterion));
            }
          }
          if (setDefinition.getFilters().isEmpty()) {
            logger.warn("No filter criteria defined for OAI-PMH set definition {}.", setDefinition.getSetSpec());
          } else {
            sets.add(setDefinition);
            logger.debug("OAI-PMH set difinition {} initialized.", setDefinition.getSetSpec());
          }
        } catch (IllegalArgumentException e) {
          logger.warn("Unable to parse OAI-PMH set definition for setSpec {}.", setSpec, e);
        }
      }
    }
  }

  /**
   * Save a query.
   *
   * @return a resumption token
   */
  public abstract String saveQuery(ResumableQuery query);

  /** Get a saved query. */
  public abstract Optional<ResumableQuery> getSavedQuery(String resumptionToken);

  /** Maximum number of items returned by the list queries ListIdentifiers, ListRecords and ListSets. */
  public abstract int getResultLimit();

  /**
   * Return a list of available metadata providers. Please do not expose the default provider for the
   * mandatory oai_dc format since this is automatically added when calling {@link #getMetadataProviders()}.
   *
   * @see #getMetadataProviders()
   */
  public abstract List<MetadataProvider> getRepositoryMetadataProviders();

  /** Return the current date. Used in implementation instead of new Date(); to facilitate unit testing. */
  public Date currentDate() {
    return new Date();
  }

  /** Return a list of all available metadata providers. The <code>oai_dc</code> format is always included. */
  public final List<MetadataProvider> getMetadataProviders() {
    return Stream.concat(
        Stream.of(OAI_DC_METADATA_PROVIDER),
        getRepositoryMetadataProviders().stream()
    ).toList();
  }

  /** Add an item to the repository. */
  public void addItem(MediaPackage mp) {
    getPersistence().search(queryRepo(getRepositoryId()).build());
    try {
      getPersistence().store(mp, getRepositoryId());
    } catch (OaiPmhDatabaseException e) {
      chuck(e);
    }
  }

  /** Create an OAI-PMH response based on the given request params. */
  public XmlGen selectVerb(Params p) {
    if (p.isVerbListIdentifiers()) {
      return handleListIdentifiers(p);
    } else if (p.isVerbListRecords()) {
      return handleListRecords(p);
    } else if (p.isVerbGetRecord()) {
      return handleGetRecord(p);
    } else if (p.isVerbIdentify()) {
      return handleIdentify(p);
    } else if (p.isVerbListMetadataFormats()) {
      return handleListMetadataFormats(p);
    } else if (p.isVerbListSets()) {
      return handleListSets(p);
    } else {
      return createErrorResponse(
              "badVerb", Optional.<String>empty(), p.getRepositoryUrl(), "Illegal OAI verb or verb is missing.");
    }
  }

  /** Return the metadata provider for a given metadata prefix. */
  public Optional<MetadataProvider> getMetadataProvider(final String metadataPrefix) {
    return getMetadataProviders().stream()
        .filter(metadataProvider -> metadataProvider.getMetadataFormat().getPrefix().equals(metadataPrefix))
        .findFirst();
  }

  /** {@link #getMetadataProvider(String)} as a function. */
  private final Function<String, Optional<MetadataProvider>> getMetadataProvider = new Function<String, Optional<MetadataProvider>>() {
    @Override public Optional<MetadataProvider> apply(String metadataPrefix) {
      return getMetadataProvider(metadataPrefix);
    }
  };

  /** Create the "GetRecord" response. */
  private XmlGen handleGetRecord(final Params p) {
    if (p.getIdentifier().isEmpty() || p.getMetadataPrefix().isEmpty()) {
      return createBadArgumentResponse(p);
    } else {
      for (final MetadataProvider metadataProvider : p.getMetadataPrefix().flatMap(mp -> getMetadataProvider.apply(mp)).stream().toList()) {
        if (p.getSet().isPresent() && !sets.stream().anyMatch(
            setDef -> StringUtils.equals(setDef.getSetSpec(), p.getSet().get()))) {
          // If there is no set specification, immediately return a no result response
          return createNoRecordsMatchResponse(p);
        }
        final SearchResult res = getPersistence()
                .search(queryRepo(getRepositoryId()).mediaPackageId(p.getIdentifier())
                                                    .setDefinitions(sets)
                                                    .setSpec(p.getSet().orElse(null)).build());
        final List<SearchResultItem> items = res.getItems();
        switch (items.size()) {
          case 0:
            return createIdDoesNotExistResponse(p);
          case 1:
            final SearchResultItem item = items.get(0);
            return new OaiVerbXmlGen(OaiPmhRepository.this, p) {
              @Override
              public Element create() {
                // create the metadata for this item
                Element metadata = metadataProvider.createMetadata(OaiPmhRepository.this, item, p.getSet());
                return oai(request($a("identifier", p.getIdentifier().get()), metadataPrefixAttr(p)),
                           verb(record(item, metadata)));
              }
            };
          default:
            throw new RuntimeException("ERROR: Search index contains more than one item with id "
                                               + p.getIdentifier());
        }
      }
      // no metadata provider found
      return createCannotDisseminateFormatResponse(p);
    }
  }

  /** Handle the "Identify" request. */
  /*<Identify >
    <repositoryName>Test OAI Repository</repositoryName>
    <baseURL>http://localhost/oaipmh</baseURL>
    <protocolVersion>2.0</protocolVersion>
    <adminEmail>admin@localhost.org</adminEmail>
    <earliestDatestamp>2010-01-01</earliestDatestamp>
    <deletedRecord>transient</deletedRecord>
    <granularity>YYYY-MM-DD</granularity>
  </Identify>*/
  private XmlGen handleIdentify(final Params p) {
    return new OaiVerbXmlGen(this, p) {
      @Override
      public Element create() {
        return oai(
                request(),
                verb($eTxt("repositoryName", OAI_NS, getRepositoryName()),
                     $eTxt("baseURL", OAI_NS, p.getRepositoryUrl()),
                     $eTxt("protocolVersion", OAI_NS, "2.0"),
                     $eTxt("adminEmail", OAI_NS, getAdminEmail()),
                     $eTxt("earliestDatestamp", OAI_NS, "2010-01-01"),
                     $eTxt("deletedRecord", OAI_NS, "transient"),
                     $eTxt("granularity", OAI_NS, toOaiRepresentation(getRepositoryTimeGranularity()))));
      }
    };
  }

  private XmlGen handleListMetadataFormats(final Params p) {
    if (p.getIdentifier().isPresent()) {
      final SearchResult res = getPersistence().search(queryRepo(getRepositoryId()).mediaPackageId(p.getIdentifier().get()).build());
      if (res.getItems().size() != 1)
        return createIdDoesNotExistResponse(p);
    }
    return new OaiVerbXmlGen(this, p) {
      @Override
      public Element create() {
        final List<Node> metadataFormats = getMetadataProviders().stream()
            .map(metadataProvider -> (Node) metadataFormat(metadataProvider.getMetadataFormat()))
            .toList();
        return oai(request($aSome("identifier", p.getIdentifier())), verb(metadataFormats));
      }
    };
  }

  private XmlGen handleListRecords(final Params p) {
    final ListItemsEnv env = new ListItemsEnv() {
      @Override
      protected ListXmlGen respond(ListGenParams listParams) {
        return new ListXmlGen(listParams) {
          @Override
          protected List<Node> createContent(final Optional<String> set) {
            return params.getResult().getItems().stream()
                .map(new java.util.function.Function<SearchResultItem, Node>() {
                  @Override
                  public Node apply(SearchResultItem item) {
                    logger.debug("Requested set: {}", set);
                    final Element metadata = params.getMetadataProvider().createMetadata(OaiPmhRepository.this, item, set);
                    return record(item, metadata);
                  }
                })
                .toList();
          }
        };
      }
    };
    return env.apply(p);
  }

  private XmlGen handleListIdentifiers(final Params p) {
    final ListItemsEnv env = new ListItemsEnv() {
      @Override
      protected ListXmlGen respond(ListGenParams listParams) {
        // create XML response
        return new ListXmlGen(listParams) {
          protected List<Node> createContent(Optional<String> set) {
            return params.getResult().getItems().stream()
                .map(item -> (Node) header(item))
                .toList();
          }
        };
      }
    };
    return env.apply(p);
  }

  /** Create the "ListSets" response. Since the list is short the complete list is returned at once. */
  private XmlGen handleListSets(final Params p) {
    return new OaiVerbXmlGen(this, p) {
      @Override
      public Element create() {
        if (sets.isEmpty()) {
          return createNoSetHierarchyResponse(p).create();
        }
        List<Node> setNodes = new LinkedList<>();
        sets.forEach(set -> {
          String setSpec = set.getSetSpec();
          String name = set.getName();
          String description = set.getDescription();
          if (StringUtils.isNotBlank(description)) {
            setNodes.add($e("set", $eTxt("setSpec", setSpec), $eTxt("setName", name),
                $e("setDescription", dc($eTxt("dc:description", description)))));
          } else {
            setNodes.add($e("set", $eTxt("setSpec", setSpec), $eTxt("setName", name)));
          }
        });
        return oai(request(), verb(setNodes));
      }
    };
  }

  // --

  private XmlGen createCannotDisseminateFormatResponse(Params p) {
    return createErrorResponse(
            OaiPmhConstants.ERROR_CANNOT_DISSEMINATE_FORMAT, p.getVerb(), p.getRepositoryUrl(),
            "The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository.");
  }

  private XmlGen createIdDoesNotExistResponse(Params p) {
    return createErrorResponse(
            OaiPmhConstants.ERROR_ID_DOES_NOT_EXIST, p.getVerb(), p.getRepositoryUrl(),
            format("The requested id %s does not exist in the repository.",
                   p.getIdentifier().orElse("?")));
  }

  private XmlGen createBadArgumentResponse(Params p) {
    return createErrorResponse(OaiPmhConstants.ERROR_BAD_ARGUMENT, p.getVerb(), p.getRepositoryUrl(),
                               "The request includes illegal arguments or is missing required arguments.");
  }

  private XmlGen createBadResumptionTokenResponse(Params p) {
    return createErrorResponse(
            OaiPmhConstants.ERROR_BAD_RESUMPTION_TOKEN, p.getVerb(), p.getRepositoryUrl(),
            "The value of the resumptionToken argument is either invalid or expired.");
  }

  private XmlGen createNoRecordsMatchResponse(Params p) {
    return createErrorResponse(
            OaiPmhConstants.ERROR_NO_RECORDS_MATCH, p.getVerb(), p.getRepositoryUrl(),
            "The combination of the values of the from, until, and set arguments results in an empty list.");
  }

  private XmlGen createNoSetHierarchyResponse(Params p) {
    return createErrorResponse(
            OaiPmhConstants.ERROR_NO_SET_HIERARCHY, p.getVerb(), p.getRepositoryUrl(),
            "This repository does not support sets");
  }

  private XmlGen createErrorResponse(
          final String code, final Optional<String> verb, final String repositoryUrl, final String msg) {
    return new OaiXmlGen(this) {
      @Override
      public Element create() {
        return oai($e("request",
                      OaiPmhConstants.OAI_2_0_XML_NS,
                      $aSome("verb", verb),
                      $txt(repositoryUrl)),
                   $e("error", OaiPmhConstants.OAI_2_0_XML_NS, $a("code", code), $cdata(msg)));
      }
    };
  }

  // --

  /**
   * Convert a date to the repository supported time granularity.
   *
   * @return the converted date or null if d is null
   */
  String toSupportedGranularity(Date d) {
    return toUtc(d, getRepositoryTimeGranularity());
  }

  // CHECKSTYLE:OFF
  final Function<Date, String> toSupportedGranularity = new Function<Date, String>() {
    @Override
    public String apply(Date date) {
      return toSupportedGranularity(date);
    }
  };
  // CHECKSTYLE:ON

  private final Function<Date, Date> granulate = new Function<Date, Date>() {
    @Override
    public Date apply(Date date) {
      return granulate(getRepositoryTimeGranularity(), date);
    }
  };

  /** "Cut" a date to the repositories supported granularity. Cutting behaves similar to the mathematical floor function. */
  public static Date granulate(Granularity g, Date d) {
    switch (g) {
      case SECOND: {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(OaiPmhUtil.newDateFormat().getTimeZone());
        c.setTime(d);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
      }
      case DAY: {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(OaiPmhUtil.newDateFormat().getTimeZone());
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
      }
      default:
        return unexhaustiveMatch();
    }
  }

  static class BadArgumentException extends RuntimeException {
  }

  /**
   * Environment for the list verbs ListIdentifiers and ListRecords. Handles the boilerplate of getting, validating and
   * providing the parameters, creating error responses etc. Call {@link #apply(Params)} to create the XML. Also use
   * {@link org.opencastproject.oaipmh.server.OaiPmhRepository.ListItemsEnv.ListXmlGen} for the XML generation.
   */
  abstract class ListItemsEnv {
    ListItemsEnv() {
    }

    /** Create the regular response from the given parameters. */
    protected abstract ListXmlGen respond(ListGenParams params);

    /** Call this method to create the XML. */
    public XmlGen apply(final Params p) {
      // check parameters
      if (p.getSet().isPresent() && sets.isEmpty()) {
        return createNoSetHierarchyResponse(p);
      }
      final boolean resumptionTokenExists = p.getResumptionToken().isPresent();
      final boolean otherParamExists = p.getMetadataPrefix().isPresent() || p.getFrom().isPresent() || p.getUntil().isPresent()
              || p.getSet().isPresent();

      if (resumptionTokenExists && otherParamExists || !resumptionTokenExists && !otherParamExists)
        return createBadArgumentResponse(p);
      final Optional<Date> from = p.getFrom().map(asDate::apply).map(granulate::apply);

      final Function<Date, Date> untilAdjustment = getRepositoryTimeGranularity() == Granularity.DAY ? addDay(1)
              : org.opencastproject.util.data.functions.Functions.<Date>identity();
      final Optional<Date> untilGranularity = p.getUntil().map(asDate::apply).map(granulate::apply).map(untilAdjustment::apply);
      if (from.isPresent() && untilGranularity.isPresent()) {
        Date fromDate = from.get();
        Date untilDate = untilGranularity.get();
        if (!fromDate.before(untilDate)) {
          return createBadArgumentResponse(p);
        }
      }
      if (otherParamExists && p.getMetadataPrefix().isEmpty())
        return createBadArgumentResponse(p);
      // <- params are ok

      final Optional<Date> until = Optional.of(untilGranularity.orElseGet(() -> currentDate()));

      final String metadataPrefix = p.getResumptionToken()
          .flatMap(getMetadataPrefixFromToken::apply)
          .orElseGet(() -> getMetadataPrefix(p).apply());

      final List<MetadataProvider> metadataProviders;
      if (p.getResumptionToken().isPresent()) {
        metadataProviders = getMetadataProviderFromToken.apply(p.getResumptionToken().get())
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
      } else {
        metadataProviders = Collections.singletonList(getMetadataProvider.curry(metadataPrefix).apply().orElseThrow(() ->
            new IllegalStateException("No MetadataProvider found for fallback")
        ));
      }

      for (MetadataProvider metadataProvider : metadataProviders) {
        try {
          final SearchResult result;
          @SuppressWarnings("unchecked")
          final Optional<String>[] set = new Optional[]{p.getSet()};

          if (!resumptionTokenExists) {
            // start a new query
            if (p.getSet().isPresent() && !sets.stream().anyMatch(
                setDef -> StringUtils.equals(setDef.getSetSpec(), p.getSet().get()))) {
              // If there is no set specification, immediately return a no result response
              return createNoRecordsMatchResponse(p);
            }
            result = getPersistence().search(
                queryRepo(getRepositoryId())
                    .setDefinitions(sets)
                    .setSpec(p.getSet().orElse(null))
                    .modifiedAfter(from)
                    .modifiedBefore(until)
                    .limit(getResultLimit()).build());
          } else {
            // resume query
            ResumableQuery rq = getSavedQuery(p.getResumptionToken().get())
                .orElseThrow(BadResumptionTokenException::new);
            set[0] = rq.getSet();
            result = getPersistence().search(
                queryRepo(getRepositoryId())
                    .setDefinitions(sets)
                    .setSpec(rq.getSet().orElse(null))
                    .modifiedAfter(rq.getLastResult())
                    .modifiedBefore(rq.getUntil())
                    .limit(getResultLimit())
                    .subsequentRequest(true).build());
          }

          if (result.size() > 0) {
            return respond(new ListGenParams(
                OaiPmhRepository.this,
                result,
                metadataProvider,
                metadataPrefix,
                p.getResumptionToken(),
                from,
                until.get(),
                set[0],
                p));
          } else {
            return createNoRecordsMatchResponse(p);
          }

        } catch (BadResumptionTokenException e) {
          return createBadResumptionTokenResponse(p);
        }
      }
      // no metadata provider found
      return createCannotDisseminateFormatResponse(p);
    }

    /** Get a metadata prefix from a resumption token. */
    private final Function<String, Optional<String>> getMetadataPrefixFromToken = new Function<String, Optional<String>>() {
      @Override
      public Optional<String> apply(String token) {
        return getSavedQuery(token).map(resumableQuery -> resumableQuery.getMetadataPrefix());
      }
    };

    /** Get a metadata provider from a resumption token. */
    private final Function<String, Optional<MetadataProvider>> getMetadataProviderFromToken = new Function<String, Optional<MetadataProvider>>() {
      @Override
      public Optional<MetadataProvider> apply(String token) {
        return getSavedQuery(token).flatMap(resumableQuery -> getMetadataProvider(resumableQuery.getMetadataPrefix()));
      }
    };

    /** Get the metadata prefix lazily. */
    private Function0<String> getMetadataPrefix(final Params p) {
      return new Function0<String>() {
        @Override
        public String apply() {
          return p.getMetadataPrefix().orElse(OaiPmhConstants.OAI_DC_METADATA_FORMAT.getPrefix());
        }
      };
    }

    /** OAI XML response generation environment for list responses. */
    abstract class ListXmlGen extends OaiVerbXmlGen {

      protected final ListGenParams params;

      ListXmlGen(ListGenParams p) {
        super(p.getRepository(), p.getParams());
        this.params = p;
      }

      /** Implement to create your content. Gets placed as children of the verb node. */
      protected abstract List<Node> createContent(Optional<String> set);

      @Override
      public Element create() {
        final List<Node> content = new ArrayList<Node>(createContent(params.getSet()));
        if (content.size() == 0)
          return createNoRecordsMatchResponse(params.getParams()).create();
        content.add(resumptionToken(params.getResumptionToken(), params.getMetadataPrefix(), params.getResult(),
                                    params.getUntil(), params.getSet()));
        return oai(
                request($a("metadataPrefix", params.getMetadataPrefix()),
                        $aSome("from", params.getFrom().map(toSupportedGranularity::apply)),
                        $aSome("until", Optional.of(toSupportedGranularity(params.getUntil()))),
                        $aSome("set", params.getSet())), verb(content));
      }
    }

    private class BadResumptionTokenException extends RuntimeException {
    }
  }
}

/** Parameter holder for the list generator. */
final class ListGenParams {
  private final OaiPmhRepository repository;
  private final SearchResult result;
  private final MetadataProvider metadataProvider;
  private final String metadataPrefix;
  private final Optional<String> resumptionToken;
  private final Optional<Date> from;
  private final Date until;
  private final Optional<String> set;
  private final Params params;

  // CHECKSTYLE:OFF
  ListGenParams(OaiPmhRepository repository,
                SearchResult result, MetadataProvider metadataProvider,
                String metadataPrefix, Optional<String> resumptionToken,
                Optional<Date> from, Date until,
                Optional<String> set,
                Params params) {
    this.repository = repository;
    this.result = result;
    this.metadataProvider = metadataProvider;
    this.resumptionToken = resumptionToken;
    this.metadataPrefix = metadataPrefix;
    this.from = from;
    this.until = until;
    this.set = set;
    this.params = params;
  }
  // CHECKSTYLE:ON

  public OaiPmhRepository getRepository() {
    return repository;
  }

  public SearchResult getResult() {
    return result;
  }

  public MetadataProvider getMetadataProvider() {
    return metadataProvider;
  }

  public Optional<String> getResumptionToken() {
    return resumptionToken;
  }

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public Optional<Date> getFrom() {
    return from;
  }

  public Date getUntil() {
    return until;
  }

  public Optional<String> getSet() {
    return set;
  }

  /** The request parameters. */
  public Params getParams() {
    return params;
  }
}
