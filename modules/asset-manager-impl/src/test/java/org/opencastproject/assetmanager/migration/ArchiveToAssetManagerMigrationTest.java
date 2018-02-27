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
package org.opencastproject.assetmanager.migration;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.fns.Strings.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.AbstractAssetManagerTestBase;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.persistencefn.Queries;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.P2;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.fns.Strings;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.persistence.EntityManager;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test the migration of {@link org.opencastproject.archive.api.Archive} tables
 * to {@link org.opencastproject.assetmanager.api.AssetManager} tables on a MySQL database.
 * <p>
 * The test is set to "ignore" since it requires some system properties to be set.
 * <ul>
 * <li>-Dtest-database-url, JDBC URL</li>
 * <li>-Dtest-database-user, defaults to 'matterhorn'</li>
 * <li>-Dtest-database-password, defaults to 'matterhorn'</li>
 * </ul>
 */
@RunWith(JUnitParamsRunner.class)
@Ignore
public class ArchiveToAssetManagerMigrationTest extends AbstractAssetManagerTestBase {
  private static final Logger logger = LoggerFactory.getLogger(ArchiveToAssetManagerMigrationTest.class);

  @Test
  @Parameters
  public void testMigration(String testData, Stream<String> seriesIds) throws Exception {
    // setup database and apply the DDL migration
    penv.tx(runStatements("/mysql-reset.sql"));
    penv.tx(runStatements("/mysql-archive-schema.sql"));
    // insert test data since it makes a difference to migrate a database with or without data
    penv.tx(runStatements(testData));
    // schema migration pre-processing
    penv.tx(runStatements("/mysql-migration-1.sql"));
    // data migration
    penv.tx(runStatements("/mysql-migration-2.sql"));
    // schema migration post-processing and cleanup
    penv.tx(runStatements("/mysql-migration-3.sql"));
    //
    // compare actual and expected DDL
    final String showCreateTables =
            $("oc_assets_snapshot", "oc_assets_asset", "oc_assets_properties", "oc_assets_version_claim", "SEQUENCE")
                    .map(Strings.wrap("SHOW CREATE TABLE ", ""))
                    .bind(findAll)
                    .map(take(1))
                    .mkString(";\n\n")
                    .concat(";");
    logger.info("+ " + showCreateTables);
    assertEquals("Migrated database should be equal to DDL in Ansible playbook",
                 IoSupport.loadFileFromClassPathAsString("/mysql-assetmanager-schema.sql").get().trim(),
                 showCreateTables);
    // check sequence table migration
    assertTrue("Sequence table should be updated",
               penv.tx(Queries.sql.findSingle("SELECT * FROM SEQUENCE WHERE SEQ_NAME='seq_oc_assets_asset'")).isSome());
    assertTrue("Sequence table should be updated",
               penv.tx(Queries.sql.findSingle("SELECT * FROM SEQUENCE WHERE SEQ_NAME='seq_oc_assets_snapshot'")).isSome());

    //
    // if the migration completed successfully the asset manager should be able to run some operations
    final AQueryBuilder q = am.createQuery();
    {
      // run series checks
      for (String seriesId : seriesIds) {
        final RichAResult r = enrich(q.select(q.snapshot()).where(q.seriesId().eq(seriesId)).run());
        assertTrue(r.getSize() > 0);
        for (Snapshot e : r.getSnapshots()) {
          assertEquals(seriesId, e.getMediaPackage().getSeries());
        }
      }
    }
    // add some media packages
    final String[] mp = createAndAddMediaPackagesSimple(5, 1, 1);
    for (String id : mp) {
      am.setProperty(p.agent.mk(id, "agent"));
    }
    {
      final RichAResult r = enrich(q.select(q.snapshot(), q.properties()).run());
      assertEquals(59, r.getSize());
      assertEquals(59, r.countSnapshots());
      assertEquals(5, r.countProperties());
    }
  }

  private Object parametersForTestMigration() {
    return $a(
            // test data set ETH
            $a("/mysql-archive-test-data-eth.sql",
               // series IDs
               $("6255c2c3-3cbc-4178-bc56-a836828a00e9",
                 "9054382b-84d2-41a3-96d9-83fafb0187ea",
                 "bcf76afe-81bb-453e-ba44-84aa7da3bb93",
                 "d72f201a-7240-49f4-be84-2be8e6aa4ab4",
                 "df2ecc72-74d8-489c-b026-93f1068bcbcc",
                 "d47137c3-4443-4e19-ac91-be0d3e6fa5cf",
                 "229d85e9-9b63-4e0c-b2c7-ea100bf71d09",
                 "1627a55a-dcff-453c-bf5c-49ed08779508",
                 "c2e23a6c-df2c-4a62-8e88-ea57973f4ce8",
                 "d47137c3-4443-4e19-ac91-be0d3e6fa5cf",
                 "63257b6b-170f-415a-967e-63483e7b099b",
                 "3ce79c93-16e2-4f85-a801-6ba50496ad39",
                 "28dc4731-16f3-4f9c-83f7-46977cf107a6",
                 "4e4e8a62-accf-4d23-83b0-4205c937206a",
                 "61aecb82-c768-4425-9bf6-14bbd8f30d2d",
                 "a6081e43-579a-47a8-81ac-90e05e2751c8",
                 "d690e96c-5a51-466c-aa76-f719983441ed",
                 "c7b29d08-5961-47aa-bf4f-e5ea6f169e0f",
                 "44a5a6a5-3ab3-41e9-b44d-a32140ff2895",
                 "099c0a5d-9db3-48f4-80da-c04f72bdbd7f",
                 "63257b6b-170f-415a-967e-63483e7b099b",
                 "52ac1672-b92e-4b78-9d8d-9a7a3b6af9c2",
                 "bcf76afe-81bb-453e-ba44-84aa7da3bb93",
                 "3ea70247-d36a-4062-9275-7bd0c1fae6c0",
                 "379a5d41-9433-49aa-af68-e52e28a37439",
                 "3af61206-199f-43e8-8429-3f4beb454b8b",
                 "61cb1e7b-5119-48b7-af90-1c98ed3631b9",
                 "c7b29d08-5961-47aa-bf4f-e5ea6f169e0f",
                 "d1a94a70-cea5-4361-b4dd-9991f42f0995",
                 "edeb17d5-fc71-4382-8b38-3aa4306e8b70",
                 "77e37ca2-faac-4220-b47c-e62233258b97",
                 "12a91df9-43e6-4bb6-8012-841add7e2c3d",
                 "cdd28f8e-4dee-407b-8935-8f0502b6638c",
                 "7c6d5e1a-6f38-4792-b85e-a845af5e7fad",
                 "63257b6b-170f-415a-967e-63483e7b099b",
                 "43f52c96-73ed-41ad-b171-901df1b19575",
                 "f254ad0a-a2e5-476d-bfb0-8da90224ecff",
                 "0aae8ad7-c477-460b-83c1-27f6617d2f84",
                 "ab72f2cc-bb4a-4f74-b063-2ece174416e7",
                 "2e2d5120-f2f5-46c9-9c75-96c90b610bcb",
                 "d2ff202b-ae12-4ddf-a43e-a33a8ded49fe",
                 "5e421af6-1fa7-487b-a3b8-b472661a926c",
                 "c5965f27-f700-4a0e-8608-f79b260fa9f3",
                 "099416b6-bcad-4e7d-bf8e-38a90a23e53a",
                 "08075abd-df47-4719-840b-2a32c15e5530")),
            // test data set 2
            $a("/mysql-archive-test-data-2.sql",
               // series IDs
               $("f4a7f0e4-fc4f-46b5-bdbc-4eade7b49146",
                 "da36a1d593b325b4ff91509fa058264f",
                 "8b5666fba8228a26ebdbae6c2bcaae1a",
                 "3a0c24228bd614d94252ac42fa879be3",
                 "2C444DC641EAE69AE102F147B6E9D505",
                 "V_841a8dee9896b4be50680fe21c5d3a5f",
                 "2C444DC641EAE69AE102F147B6E9D956",
                 "C24D3664B85FAE65E7A6ABC2EEE717D1",
                 "2C444DC641EAE69AE102F147B6E9D79E",
                 "V_fc0d286d87c76d62cabe7bff590699eb",
                 "bebe2c15a1a931911a65cb0003576109",
                 "a309e0443ed822d3d9506cc2c6f269ec",
                 "7aa741c5978dc777b916366cb5efd8b7",
                 "7d810947260ced5af4c370e21011365f",
                 "349F8D69FDA3DEB3E86B16CFF9B7045B",
                 "9bbb092844a6c1adac41e4da8fcebeaf",
                 "V_eb16d184e6a92bac2b99d049d69762ca",
                 "3bf1ef11-0fa6-4def-9dfb-45f1c7b763a8",
                 "V_b9e41816cfb21c305ae1f168dd6ace8b")));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Test
  public void testMh01StageMigration() {
    penv.tx(runStatements("/test_mh01_stage_migration.sql"));
    // schema migration pre-processing
    penv.tx(runStatements("/mysql-migration-1.sql"));
    // data migration
    penv.tx(runStatements("/mysql-migration-2.sql"));
    // schema migration post-processing and cleanup
    penv.tx(runStatements("/mysql-migration-3.sql"));
  }

  private Fn<EntityManager, Unit> runStatements(String classPathResource) {
    return runStatements(readStatements(classPathResource));
  }

  private Fn<EntityManager, Unit> runStatements(final Stream<String> statements) {
    return new Fn<EntityManager, Unit>() {
      @Override public Unit apply(EntityManager em) {
        for (String statement : statements) {
          logger.info("+++ " + statement);
          Queries.sql.update(em, statement);
        }
        return Unit.unit;
      }
    };
  }

  private Fn<String, List<Object[]>> findAll = new Fn<String, List<Object[]>>() {
    @Override public List<Object[]> apply(String statement) {
      return penv.tx(Queries.sql.<Object[]>findAll(statement));
    }
  };

  private <A> Fn<A[], A> take(final int index) {
    return new Fn<A[], A>() {
      @Override public A apply(A[] as) {
        return as[index];
      }
    };
  }

  private Stream<String> readStatements(String classPathResource) {
    for (String file : IoSupport.loadFileFromClassPathAsString(classPathResource)) {
      return $(file.split("\\n"))
              .map(Strings.trim)
              .filter((startsWith("--").or(startsWith("#")).or(isBlank)).not())
              .foldl(Products.E.p2("", Stream.<String>empty()), new Fn2<P2<String, Stream<String>>, String, P2<String, Stream<String>>>() {
                @Override public P2<String, Stream<String>> apply(P2<String, Stream<String>> statements, String line) {
                  if (line.endsWith(";")) {
                    return Products.E.p2("", statements.get2().append($(statements.get1() + "\n" + line)));
                  } else {
                    return Products.E.p2(statements.get1() + "\n" + line, statements.get2());
                  }
                }
              }).get2();
    }
    throw new RuntimeException("Cannot read SQL file " + classPathResource);
  }

  private Pred<String> startsWith(final String a) {
    return new Pred<String>() {
      @Override public Boolean apply(String s) {
        return s.startsWith(a);
      }
    };
  }
}
