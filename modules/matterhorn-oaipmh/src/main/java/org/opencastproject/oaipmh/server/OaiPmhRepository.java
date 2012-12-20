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

package org.opencastproject.oaipmh.server;

import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.solr.Schema;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.opencastproject.oaipmh.OaiPmhUtil.toOaiRepresentation;
import static org.opencastproject.oaipmh.OaiPmhUtil.toUtc;
import static org.opencastproject.oaipmh.OaiPmhUtil.toUtcDay;
import static org.opencastproject.oaipmh.OaiPmhUtil.toUtcSecond;
import static org.opencastproject.oaipmh.server.Functions.isAfter;
import static org.opencastproject.util.data.Collections.find;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/**
 * An OAI-PMH protocol compliant repository.
 * <p/>
 * Currently supported:
 * <ul>
 * <li></li>
 * </ul>
 * <p/>
 * Currently <em>not</em> supported:
 * <ul>
 * <li><a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#deletion">deletions</a></li>
 * <li><a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Set">sets</a></li>
 * <li>&lt;about&gt; containers in records, see section
 * <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Record">2.5. Record</a></li>
 * <li>
 * resumption tokens do not report about their expiration date;
 * see section <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl">3.5. Flow Control</a>
 * </li>
 * </ul>
 * <p/>
 * todo
 * - malformed date parameter must produce a BadArgument error
 * - if a date parameter has a finer granularity than supported by the repository this must produce a BadArgument error
 */
public abstract class OaiPmhRepository {

  private static final Logger logger = LoggerFactory.getLogger(OaiPmhRepository.class);

  public abstract Granularity getRepositoryTimeGranularity();

  public abstract String getBaseUrl();

  public abstract String getRepositoryName();

  /**
   * Return a solr based search service. Services backed by other systems are currently
   * not supported since the {@linkplain org.opencastproject.search.api.SearchQuery search query api}
   * is not capable of creating the needed queries so that native solr queries are generated.
   */
  public abstract SearchService getSearchService();

  public abstract String getAdminEmail();

  /**
   * Save a query.
   *
   * @return a resumption token
   */
  public abstract String saveQuery(ResumableQuery query);

  /**
   * Get a saved query.
   */
  public abstract Option<ResumableQuery> getSavedQuery(String resumptionToken);

  /**
   * Maximum number of items returned by the list queries ListIdentifiers, ListRecords and ListSets.
   */
  public abstract int getResultLimit();

  /**
   * Return a list of all available metadata providers. The list must contain at least a
   * provider for the <code>oai_dc</code> metadata prefix.
   */
  public abstract List<MetadataProvider> getMetadataProviders();

  /**
   * OAI-PMH repositories are required to support at least the Dublin Core metadata element set version 1.1.
   */
  public static final MetadataFormat OAI_DC_METADATA_FORMAT = new MetadataFormat() {
    @Override
    public String getPrefix() {
      return "oai_dc";
    }

    @Override
    public URL getSchema() {
      try {
        return new URL("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public URI getNamespace() {
      try {
        return new URI("http://www.openarchives.org/OAI/2.0/oai_dc/");
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  };

  /**
   * Create an OAI-PMH response based on the given request params.
   */
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
      return createErrorResponse("badVerb", "Illegal OAI verb or verb is missing.");
    }
  }

  public Option<MetadataProvider> getMetadataProvider(final String metadataPrefix) {
    return find(getMetadataProviders(), new Predicate<MetadataProvider>() {
      @Override
      public Boolean apply(MetadataProvider metadataProvider) {
        return metadataProvider.getMetadataFormat().getPrefix().equals(metadataPrefix);
      }
    });
  }

  /**
   * Create the "GetRecord" response.
   */
  private XmlGen handleGetRecord(final Params p) {
    if (p.getIdentifier().isNone() || p.getMetadataPrefix().isNone()) {
      return createBadArgumentResponse();
    } else {
      return getMetadataProvider(p.getMetadataPrefix().getOrElse(""))
          .fold(new Option.Match<MetadataProvider, XmlGen>() {
            @Override
            public XmlGen some(final MetadataProvider metadataProvider) {
              // query search index with identifier
              SearchResult res = getSearchService().getByQuery(new SearchQuery().withId(p.getIdentifier().get()));
              SearchResultItem[] items = res.getItems();
              switch (items.length) {
                case 0:
                  return createIdDoesNotExistResponse(p.getIdentifier().get());
                case 1:
                  final SearchResultItem item = items[0];
                  return new OaiVerbXmlGen(OaiPmhRepository.this, p.getVerb().get()) {
                    @Override
                    public Element create() {
                      // create the metadata for this item
                      Element metadata = metadataProvider.createMetadata(OaiPmhRepository.this, item);
                      return oai(
                          request(
                              $a("identifier", p.getIdentifier().get()),
                              metadataPrefixAttr(p)),
                          verb(record(item, metadata)));
                    }
                  };
                default:
                  throw new RuntimeException("ERROR: Search index contains more than one item with id " + p.getIdentifier());
              }
            }

            @Override
            public XmlGen none() {
              return createCannotDisseminateFormatResponse();
            }
          });
    }
  }

  /**
   * Handle the "Identify" request.
   */
  private XmlGen handleIdentify(final Params p) {
    return new OaiVerbXmlGen(this, p.getVerb().get()) {
      @Override
      public Element create() {
        return oai(
            request(),
            verb(
                $eTxt("repositoryName", getRepositoryName()),
                $eTxt("protocolVersion", "2.0"),
                $eTxt("adminEmail", getAdminEmail()),
                // todo
                $eTxt("earliestDatestamp", "2010-01-01"),
                $eTxt("deletedRecord", "no"),
                $eTxt("granularity", toOaiRepresentation(getRepositoryTimeGranularity())))
        );
      }
    };
  }

  private XmlGen handleListMetadataFormats(final Params p) {
    if (p.getIdentifier().isSome()) {
      SearchResult res = getSearchService().getByQuery(new SearchQuery().withId(p.getIdentifier().get()));
      if (res.getItems().length != 1)
        return createIdDoesNotExistResponse(p.getIdentifier().get());
    }
    return new OaiVerbXmlGen(this, p.getVerb().get()) {
      @Override
      public Element create() {
        List<Node> metadataFormats = map(getMetadataProviders(), __(Node.class), new Function<MetadataProvider, Node>() {
          @Override
          public Node apply(MetadataProvider metadataProvider) {
            return metadataFormat(metadataProvider.getMetadataFormat());
          }
        });
        return oai(
            request($aSome("identifier", p.getIdentifier())),
            verb(metadataFormats));
      }
    };
  }

  private XmlGen handleListRecords(final Params p) {
    ListItemsEnv env = new ListItemsEnv() {
      @Override
      protected ListXmlGen respond(ListGenParams listParams) {
        return new ListXmlGen(listParams, p.getVerb().get()) {
          @Override
          protected List<Node> createContent() {
            return map(_(params.getResult().getItems()), __(Node.class), new Function<SearchResultItem, Node>() {
              @Override
              public Node apply(SearchResultItem item) {
                Element metadata = params.getMetadataProvider().createMetadata(OaiPmhRepository.this, item);
                return record(item, metadata);
              }
            });
          }
        };
      }
    };
    return env.apply(p);
  }

  private XmlGen handleListIdentifiers(final Params p) {
    ListItemsEnv env = new ListItemsEnv() {
      @Override
      protected ListXmlGen respond(ListGenParams listParams) {
        // create XML response
        return new ListXmlGen(listParams, p.getVerb().get()) {
          @Override
          protected List<Node> createContent() {
            return map(_(params.getResult().getItems()), __(Node.class), new Function<SearchResultItem, Node>() {
              @Override
              public Node apply(SearchResultItem item) {
                return header(item);
              }
            });
          }
        };
      }
    };
    return env.apply(p);
  }

  /**
   * Create the "ListSets" response. Since the list is short the complete list is
   * returned at once.
   */
  private XmlGen handleListSets(final Params p) {
    return new OaiVerbXmlGen(this, p.getVerb().get()) {
      @Override
      public Element create() {
        // define sets
        List<List<String>> sets = _(
            _("series", "Collection of all series", "This set contains objects describing a series of episodes."),
            _("episode", "Collection of all episodes", "This set contains episodes."),
            _("episode:audio", "Audio episodes collection", "This set contains audio-only episodes."),
            _("episode:video", "Video episodes collection", "This set contains video episodes."));
        // map to nodes
        List<Node> setNodes = map(sets, __(Node.class), new Function<List<String>, Node>() {
          @Override
          public Node apply(List<String> strings) {
            return $e("set",
                $eTxt("setSpec", strings.get(0)),
                $eTxt("setName", strings.get(1)),
                $e("setDescription",
                    dc($eTxt("dc:description", strings.get(2)))));
          }
        });
        return oai(
            request(),
            verb(setNodes));
      }
    };
  }

  // --

  private XmlGen createCannotDisseminateFormatResponse() {
    return createErrorResponse("cannotDisseminateFormat",
        "The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository.");
  }

  private XmlGen createIdDoesNotExistResponse(String identifier) {
    return createErrorResponse("idDoesNotExist", "The requested id does not exist in the repository.");
  }

  private XmlGen createBadArgumentResponse() {
    return createErrorResponse("badArgument", "The request includes illegal arguments or is missing required arguments.");
  }

  private XmlGen createBadResumptionTokenResponse() {
    return createErrorResponse("badResumptionToken", "The value of the resumptionToken argument is either invalid or expired.");
  }

  private XmlGen createNoRecordsMatchResponse() {
    return createErrorResponse("noRecordsMatch", "The combination of the values of the from, until, and set arguments results in an empty list.");
  }

  private XmlGen createErrorResponse(final String code, final String msg) {
    return new OaiXmlGen(this) {
      @Override
      public Element create() {
        return oai(
            $e("request",
                $txt(getBaseUrl())),
            $e("error",
                $a("code", code),
                $cdata(msg))
        );
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

  /**
   * Convert a date into a string suitable for the use as the start date in solr date range queries
   * depending on the repositories date granularity.
   */
  private final Function<Date, String> toSolrDateRangeStart = new Function<Date, String>() {
    @Override
    public String apply(Date date) {
      switch (getRepositoryTimeGranularity()) {
        case SECOND:
          return toUtcSecond(date);
        case DAY:
          // solr needs the full date and time, but if the repository only supports
          // day granularity we need to use this hack
          return toUtcDay(date) + "T00:00:00Z";
        default:
          throw new RuntimeException("bug");
      }
    }
  };

  /**
   * Convert a date into a string suitable for the use as the end date in solr date range queries
   * depending on the repositories date granularity.
   */
  private final Function<Date, String> toSolrDateRangeEnd = new Function<Date, String>() {
    @Override
    public String apply(Date date) {
      switch (getRepositoryTimeGranularity()) {
        case SECOND:
          return toUtcSecond(date);
        case DAY:
          // solr needs the full date and time, but if the repository only supports
          // day granularity we need to use this hack
          Calendar c = Calendar.getInstance();
          c.setTime(date);
          c.add(Calendar.DAY_OF_MONTH, 1);
          return toUtcDay(c.getTime()) + "T00:00:00Z";
        default:
          throw new RuntimeException("bug");
      }
    }
  };

  private static final Function<String, Option<String>> convSetSpecToSolrQuery = new Function<String, Option<String>>() {
    @Override
    public Option<String> apply(String setSpec) {
      if ("series".equals(setSpec)) {
        return some(Schema.OC_MEDIATYPE + ":" + SearchResultItem.SearchResultItemType.Series);
      }
      if (setSpec.startsWith("episode")) {
        return some(Schema.OC_MEDIATYPE + ":" + SearchResultItem.SearchResultItemType.AudioVisual);
      }
      return none();
    }
  };

  static class BadArgumentException extends RuntimeException {
  }

  /**
   * Environment for the list verbs ListIdentifiers and ListRecords. Handles the boilerplate
   * of getting, validating and providing the parameters, creating error responses etc.
   * Call {@link #apply(Params)} to create the XML.
   * Also use {@link org.opencastproject.oaipmh.server.OaiPmhRepository.ListItemsEnv.ListXmlGen}
   * for the XML generation.
   */
  abstract class ListItemsEnv {

    ListItemsEnv() {
    }

    /**
     * Create the regular response from the given parameters.
     */
    protected abstract ListXmlGen respond(ListGenParams params);

    /**
     * Call this method to create the XML.
     */
    public XmlGen apply(final Params p) {
      // check parameters
      final boolean resumptionTokenExists = p.getResumptionToken().isSome();
      final boolean otherParamExists =
          p.getMetadataPrefix().isSome() || p.getFrom().isSome() || p.getUntil().isSome() || p.getSet().isSome();

      if (resumptionTokenExists && otherParamExists || !resumptionTokenExists && !otherParamExists)
        return createBadArgumentResponse();
      final Option<Date> from = p.getFrom().map(Functions.asDate);
      final Option<Date> until = p.getUntil().map(Functions.asDate);
      if (from.map(isAfter(until)).getOrElse(false))
        return createBadArgumentResponse();
      if (otherParamExists && p.getMetadataPrefix().isNone())
        return createBadArgumentResponse();
      // <- params are ok

      final String metadataPrefix = p.getResumptionToken()
          .flatMap(getMetadataPrefixFromToken)
          .getOrElse(getMetadataPrefix(p));
      final Option<MetadataProvider> metadataProvider = p.getResumptionToken()
          .flatMap(getMetadataProviderFromToken)
          .orElse(getMetadataProvider(metadataPrefix));

      return metadataProvider.fold(new Option.Match<MetadataProvider, XmlGen>() {
        @Override
        public XmlGen some(MetadataProvider metadataProvider) {
          try {
            final SearchResult result;
            if (!resumptionTokenExists) {
              // start a new query
              final List<String> queryFragments = new ArrayList<String>();
              if (from.isSome() || until.isSome()) {
                queryFragments.add(String.format("%s:[%s TO %s]",
                    Schema.OC_MODIFIED,
                    from.map(toSolrDateRangeStart).getOrElse("*"),
                    until.map(toSolrDateRangeEnd).getOrElse("*")));
              }
              p.getSet().flatMap(convSetSpecToSolrQuery).map(Functions.appendTo(queryFragments));
              result = getSearchService().getByQuery(mkString(queryFragments, " "), getResultLimit(), 0);
            } else {
              // resume query
              result = getSavedQuery(p.getResumptionToken().get()).fold(new Option.Match<ResumableQuery, SearchResult>() {
                @Override
                public SearchResult some(ResumableQuery rq) {
                  return getSearchService().getByQuery(rq.getQuery(), rq.getLimit(), rq.getOffset() + rq.getLimit());
                }

                @Override
                public SearchResult none() {
                  // no resumable query found
                  throw new BadResumptionTokenException();
                }
              });
            }
            if (result.size() > 0) {
              return respond(new ListGenParams(
                  OaiPmhRepository.this,
                  result,
                  metadataProvider,
                  metadataPrefix,
                  p.getResumptionToken(),
                  from,
                  until,
                  p.getSet()));
            } else {
              return createNoRecordsMatchResponse();
            }
          } catch (BadResumptionTokenException e) {
            return createBadResumptionTokenResponse();
          }
        }

        /**
         * There's no metadata provider registered for that prefix.
         */
        @Override
        public XmlGen none() {
          return createCannotDisseminateFormatResponse();
        }
      });
    }

    /**
     * Get a metadata prefix from a resumption token.
     */
    private final Function<String, Option<String>> getMetadataPrefixFromToken = new Function<String, Option<String>>() {
      @Override
      public Option<String> apply(String token) {
        return getSavedQuery(token).map(new Function<ResumableQuery, String>() {
          @Override
          public String apply(ResumableQuery resumableQuery) {
            return resumableQuery.getMetadataPrefix();
          }
        });
      }
    };

    /**
     * Get a metadata provider from a resumption token.
     */
    private final Function<String, Option<MetadataProvider>> getMetadataProviderFromToken = new Function<String, Option<MetadataProvider>>() {
      @Override
      public Option<MetadataProvider> apply(String token) {
        return getSavedQuery(token).flatMap(new Function<ResumableQuery, Option<MetadataProvider>>() {
          @Override
          public Option<MetadataProvider> apply(ResumableQuery resumableQuery) {
            return getMetadataProvider(resumableQuery.getMetadataPrefix());
          }
        });
      }
    };

    /**
     * Get the metadata prefix lazily.
     */
    private Function0<String> getMetadataPrefix(final Params p) {
      return new Function0<String>() {
        @Override
        public String apply() {
          return p.getMetadataPrefix().get();
        }
      };
    }

    /**
     * OAI XML response generation environment for list responses.
     */
    abstract class ListXmlGen extends OaiVerbXmlGen {

      protected final ListGenParams params;

      ListXmlGen(ListGenParams p, String verb) {
        super(p.getRepository(), verb);
        this.params = p;
      }

      /**
       * Implement to create your content. Gets placed as children of the verb node.
       */
      protected abstract List<Node> createContent();

      @Override
      public Element create() {
        List<Node> content = createContent();
        content.add(resumptionToken(params.getResumptionToken(), params.getMetadataPrefix(), params.getResult()));
        return oai(
            request(
                $a("metadataPrefix", params.getMetadataPrefix()),
                $aSome("from", params.getFrom().map(toSupportedGranularity)),
                $aSome("until", params.getUntil().map(toSupportedGranularity)),
                $aSome("set", params.getSet())),
            verb(content)
        );
      }
    }

    private class BadResumptionTokenException extends RuntimeException {
    }
  }
}

/**
 * Parameter holder for the list generator.
 */
final class ListGenParams {

  private final OaiPmhRepository repository;

  private final SearchResult result;

  private final MetadataProvider metadataProvider;

  private final String metadataPrefix;

  private final Option<String> resumptionToken;

  private final Option<Date> from;

  private final Option<Date> until;

  private final Option<String> set;

  // CHECKSTYLE:OFF
  ListGenParams(OaiPmhRepository repository, SearchResult result, MetadataProvider metadataProvider,
                String metadataPrefix,
                Option<String> resumptionToken, Option<Date> from, Option<Date> until, Option<String> set) {
    this.repository = repository;
    this.result = result;
    this.metadataProvider = metadataProvider;
    this.resumptionToken = resumptionToken;
    this.metadataPrefix = metadataPrefix;
    this.from = from;
    this.until = until;
    this.set = set;
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

  public Option<String> getResumptionToken() {
    return resumptionToken;
  }

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public Option<Date> getFrom() {
    return from;
  }

  public Option<Date> getUntil() {
    return until;
  }

  public Option<String> getSet() {
    return set;
  }
}


