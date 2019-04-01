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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.util.persistence

import org.opencastproject.util.data.Function

import javax.persistence.EntityManager

/** Persistence environment to perform a transaction.  */
abstract class PersistenceEnv {
    /** Run code inside a transaction.  */
    abstract fun <A> tx(transactional: Function<EntityManager, A>): A

    /** [.tx] as a function.  */
    fun <A> tx(): Function<Function<EntityManager, A>, A> {
        return object : Function<Function<EntityManager, A>, A>() {
            override fun apply(transactional: Function<EntityManager, A>): A {
                return tx(transactional)
            }
        }
    }

    /** Close the environment and free all associated resources.  */
    abstract fun close()
}
