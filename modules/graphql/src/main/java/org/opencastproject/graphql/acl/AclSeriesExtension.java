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

package org.opencastproject.graphql.acl;

import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.graphql.series.GqlSeries;
import org.opencastproject.graphql.type.output.GqlAccessControlList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;

@GraphQLTypeExtension(GqlSeries.class)
public class AclSeriesExtension {

  private static final Logger logger = LoggerFactory.getLogger(AclSeriesExtension.class);

  private final GqlSeries series;

  public AclSeriesExtension(GqlSeries series) {
    this.series = series;
  }

  @GraphQLField
  @GraphQLNonNull
  public GqlAccessControlList acl() {
    return new GqlAccessControlList(getAclFromSeries(series.getSeries()));
  }

  protected static AccessControlList getAclFromSeries(Series series) {
    AccessControlList activeAcl = new AccessControlList();
    try {
      if (series.getAccessPolicy() != null) {
        activeAcl = AccessControlParser.parseAcl(series.getAccessPolicy());
      }
    } catch (Exception e) {
      logger.error("Unable to parse access policy", e);
    }
    return activeAcl;
  }

}
