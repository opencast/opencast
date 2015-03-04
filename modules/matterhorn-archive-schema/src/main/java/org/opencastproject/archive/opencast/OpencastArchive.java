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
package org.opencastproject.archive.opencast;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.ArchiveBase;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDbException;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.archive.opencast.solr.SolrIndexManager;
import org.opencastproject.archive.opencast.solr.SolrRequester;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.solr.client.solrj.SolrServerException;

import java.util.Date;
import java.util.List;

/** Opencast specific implementation of the archive. */
public final class OpencastArchive extends ArchiveBase<OpencastResultSet> {
  private final SolrRequester solrRequester;
  private final SolrIndexManager solrIndex;

  public OpencastArchive(SolrIndexManager solrIndex, SolrRequester solrRequester, SecurityService secSvc,
          AuthorizationService authSvc, OrganizationDirectoryService orgDir, ServiceRegistry svcReg,
          WorkflowService workflowSvc, Workspace workspace, ArchiveDb persistence, ElementStore elementStore,
          String systemUserName, MessageSender messageSender, MessageReceiver messageReceiver) {
    super(secSvc, authSvc, orgDir, svcReg, workflowSvc, workspace, persistence, elementStore, systemUserName,
            messageSender, messageReceiver);
    this.solrIndex = solrIndex;
    this.solrRequester = solrRequester;
  }

  @Override
  public synchronized void add(final MediaPackage mp) throws ArchiveException {
    if (mp.getCatalogs(MediaPackageElements.EPISODE).length == 0)
      throw new ArchiveException("Archived Mediapackage didn't contain a necessary " + MediaPackageElements.EPISODE
              + " catalog.");
    super.add(mp);
  }

  @Override
  protected void index(MediaPackage mp, AccessControlList acl, Date timestamp, Version version) {
    try {
      solrIndex.add(mp, acl, timestamp, version);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected void index(MediaPackage mediaPackage, AccessControlList acl, Version version, boolean deleted,
          Date modificationDate, boolean latestVersion) {
    try {
      solrIndex.add(mediaPackage, acl, version, deleted, modificationDate, latestVersion);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected boolean indexDelete(String mediaPackageId, Date timestamp) {
    try {
      return solrIndex.delete(mediaPackageId, timestamp);
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected OpencastResultSet indexFind(Query q) {
    try {
      if (q instanceof OpencastQuery)
        return solrRequester.find((OpencastQuery) q);
      else
        return solrRequester.find(OpencastQueryBuilder.query(q));
    } catch (SolrServerException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected long indexSize() {
    try {
      return solrIndex.count();
    } catch (ArchiveDbException e) {
      throw new ArchiveException(e);
    }
  }

  @Override
  protected OpencastResultSet newResultSet(final List<? extends ResultItem> rs, final String query,
          final long totalSize, final long offset, final long limit, final long searchTime) {
    return new OpencastResultSet() {
      @SuppressWarnings("unchecked")
      @Override
      public List<OpencastResultItem> getItems() {
        return (List<OpencastResultItem>) rs;
      }

      @Override
      public String getQuery() {
        return query;
      }

      @Override
      public long getTotalSize() {
        return totalSize;
      }

      @Override
      public long getLimit() {
        return limit;
      }

      @Override
      public long getOffset() {
        return offset;
      }

      @Override
      public long getSearchTime() {
        return searchTime;
      }
    };
  }

}
