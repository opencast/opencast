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
package org.opencastproject.archive.base;

import org.junit.Test;
import org.opencastproject.util.data.Option;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

public class QueryBuilderTest {
  @Test
  public void test() throws Exception {
    final QueryBuilder q1 = QueryBuilder.query().mediaPackageId("1");
    // order of method calls is not important anymore since every setter now returns a XyzQueryBuilder
    final XyzQueryBuilder q2 = XyzQueryBuilder.query().mediaPackageId("2").title("title");
  }
}

/**
 * Base builder for Xyz queries suitable for further inheritance. Implements fields and setters for
 * {@link XyzQuery} and sets default values. In order to create a concrete builder
 * subclass and fix type variable A the the subclass.
 */
abstract class XyzQueryBuilderBase<A extends XyzQueryBuilderBase<A>> extends QueryBuilderBase<A> implements XyzQuery {
  private Option<String> title = none();

  protected XyzQueryBuilderBase() {
  }

  @Override public Option<String> getTitle() {
    return title;
  }

  public A title(Option<String> title) {
    this.title = title;
    return self();
  }

  public A title(String title) {
    return title(some(title));
  }
}

/**
 * Builder for Xyz queries that fixes type variable A to a concrete implementation.
 * <p/>
 * Working with {@link XyzQueryBuilderBase} itself is not possible since
 * all setters of superclass {@link org.opencastproject.archive.base.QueryBuilderBase} then
 * return type {@link org.opencastproject.archive.base.QueryBuilderBase} instead of the desired Xyz version.
 */
final class XyzQueryBuilder extends XyzQueryBuilderBase<XyzQueryBuilder> {
  private XyzQueryBuilder() {
  }

  public static XyzQueryBuilder query() {
    return new XyzQueryBuilder();
  }
}
