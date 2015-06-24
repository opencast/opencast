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


package org.opencastproject.archive.api.concept;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.query.ResultSetBase;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.util.List;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/* --- *
* New EpisodeService API
* --- */

/** Episode service. Implementations vary over the set of supported metadata. */
interface EpisodeService {
  ResultSet find(Query q);
  void add(MediaPackage mp);
  void delete(String mpId);
  List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                       UriRewriter rewriteUri,
                                       Query q) throws ArchiveException;
}

abstract class ResultSet extends ResultSetBase<ResultItem> {
}

/** Minimum result item. */
interface ResultItem {
  MediaPackage getMediaPackage();
  String getOrganizationId();
}

/** A query to the episode service. */
interface Query {
  Option<String> getMediaPackageId();
  Option<String> getSeriesId();
  boolean isOnlyLastVersion();
}

/* -- *
* Base implementation
* -- */

abstract class EpisodeServiceBase implements EpisodeService {
  // provide abstract methods for all schema related functions
}

class QueryBuilder implements Query {
  private final Query q;

  QueryBuilder(Query q) {
    this.q = q;
  }

  /**
   * Access the underlying query via this getter. Subclasses should override the method with a covariant version.
   */
  protected Query q() {
    return q;
  }

  @Override public Option<String> getMediaPackageId() {
    return q().getMediaPackageId();
  }

  public QueryBuilder mediaPackageId(final String a) {
    return new QueryBuilder(this) {
      @Override public Option<String> getMediaPackageId() {
        return some(a);
      }
    };
  }

  @Override public Option<String> getSeriesId() {
    return q().getSeriesId();
  }

  public QueryBuilder seriesId(final String a) {
    return new QueryBuilder(this) {
      @Override public Option<String> getSeriesId() {
        return some(a);
      }
    };
  }

  @Override public boolean isOnlyLastVersion() {
    return q().isOnlyLastVersion();
  }

  public QueryBuilder onlyLastVersion(final boolean a) {
    return new QueryBuilder(this) {
      @Override public boolean isOnlyLastVersion() {
        return a;
      }
    };
  }
}

final class Queries {
  private Queries() {
  }

  static final Query ZERO = new Query() {
    @Override public Option<String> getMediaPackageId() {
      return none();
    }

    @Override public Option<String> getSeriesId() {
      return none();
    }

    @Override public boolean isOnlyLastVersion() {
      return false;
    }
  };

  static QueryBuilder query() {
    return new QueryBuilder(ZERO);
  }
}

/* --- *
* Toolbox
* --- */

/* --- *
* ETH specific implementation
* --- */

final class Eth {
  private Eth() {
  }

  public static EthResultSet resultSet(final List<EthResultItem> items,
                                       final String query,
                                       final long totalSize,
                                       final long limit,
                                       final long offset,
                                       final long searchTime) {
    return new EthResultSet() {
      @Override public List<EthResultItem> getItems() {
        return items;
      }

      @Override public String getQuery() {
        return query;
      }

      @Override public long getTotalSize() {
        return totalSize;
      }

      @Override public long getLimit() {
        return limit;
      }

      @Override public long getOffset() {
        return offset;
      }

      @Override public long getSearchTime() {
        return searchTime;
      }
    };
  }

  public static EthResultItem resultItem(final MediaPackage mp,
                                         final String orgId,
                                         final DublinCore dc) {
    return new EthResultItem() {
      @Override public DublinCore getDublinCore() {
        return dc;
      }

      @Override public MediaPackage getMediaPackage() {
        return mp;
      }

      @Override public String getOrganizationId() {
        return orgId;
      }
    };
  }

  public static EthQuery query(final Query q) {
    return new EthQuery() {
      @Override public Option<String> getTitle() {
        return none();
      }

      @Override public Option<String> getCreator() {
        return none();
      }

      @Override public Option<String> getMediaPackageId() {
        return q.getMediaPackageId();
      }

      @Override public Option<String> getSeriesId() {
        return q.getSeriesId();
      }

      @Override public boolean isOnlyLastVersion() {
        return q.isOnlyLastVersion();
      }
    };
  }

  public static EthQueryBuilder query() {
    return new EthQueryBuilder(ZERO);
  }

  public static final EthQuery ZERO = new EthQuery() {
    @Override public Option<String> getTitle() {
      return none();
    }

    @Override public Option<String> getCreator() {
      return none();
    }

    @Override public Option<String> getMediaPackageId() {
      return none();
    }

    @Override public Option<String> getSeriesId() {
      return none();
    }

    @Override public boolean isOnlyLastVersion() {
      return false;
    }
  };
}

interface EthQuery extends Query {
  Option<String> getTitle();
  Option<String> getCreator();


}

class EthQueryBuilder extends QueryBuilder implements EthQuery {
  EthQueryBuilder(EthQuery q) {
    super(q);
  }

  @Override protected EthQuery q() {
    return (EthQuery) super.q();
  }

  @Override public Option<String> getTitle() {
    return q().getTitle();
  }

  public EthQueryBuilder title(final String a) {
    return new EthQueryBuilder(this) {
      @Override public Option<String> getTitle() {
        return some(a);
      }
    };
  }

  @Override public Option<String> getCreator() {
    return q().getCreator();
  }

  public EthQueryBuilder creator(final String a) {
    return new EthQueryBuilder(this) {
      @Override public Option<String> getCreator() {
        return some(a);
      }
    };
  }
}


abstract class EthResultSet extends ResultSet {
  // specify the concrete item type
  @Override public abstract List<EthResultItem> getItems();
}

interface EthResultItem extends ResultItem {
  DublinCore getDublinCore();
}

interface DublinCore {
}

/** ETH specific implementation of the episode service. */
class EthEpisodeService implements EpisodeService {
  @SuppressWarnings("unchecked")
  @Override public EthResultSet find(Query q) {
    System.out.println("eth find query");
    return find(Eth.query(q));
  }

  public EthResultSet find(EthQuery q) {
    System.out.println("eth find ethquery ");
    // transform q into a solr query for example and to the actual search
    return Eth.resultSet(list(Eth.resultItem(null, null, null)),
                         "query", 20, 10, 0, 10);
  }

  @Override public void add(MediaPackage mp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override public void delete(String mpId) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override public List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                                        UriRewriter rewriteUri,
                                                        Query q) throws ArchiveException {
    return applyWorkflow(workflow, rewriteUri, Eth.query(q));
  }

  public List<WorkflowInstance> applyWorkflow(ConfiguredWorkflow workflow,
                                              UriRewriter rewriteUri,
                                              EthQuery q) throws ArchiveException {
    return null;
  }
}

class EthEpisodeServiceRestEndpoint {
  private EthEpisodeService es;

  public Object find(String mediapackageId, String title) {
    return toJaxb(es.find(Eth.query().title(title)));
  }

  private Object toJaxb(EthResultSet rs) {
    return null;
  }
}

/* --- *
* Episode service client
* --- */

class Client {
  void run(EpisodeService es) {
    for (ResultItem item : es.find(Queries.query().mediaPackageId("mpid")).getItems()) {
      System.out.println(item);
    }
  }

  void run(EthEpisodeService es) {
    for (EthResultItem item : es.find(Eth.query().title("title")).getItems()) {
      System.out.println(item);
    }
  }
}
