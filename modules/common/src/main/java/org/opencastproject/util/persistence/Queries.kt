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

import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Monadics
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple

import org.joda.time.base.AbstractInstant

import java.util.Date

import javax.persistence.EntityManager
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException
import javax.persistence.Query
import javax.persistence.TemporalType
import javax.persistence.TypedQuery

/** JPA query constructors.  */
// CHECKSTYLE:OFF
object Queries {

    // -------------------------------------------------------------------------------------------------------------------

    /** Named queries with support for named parameters.  */
    val named: TypedQueriesBase<Tuple<String, *>> = object : TypedQueriesBase<Tuple<String, *>>() {
        override fun query(em: EntityManager, q: String, vararg params: Tuple<String, *>): Query {
            return setParams(em.createNamedQuery(q), *params)
        }

        override fun <A> query(em: EntityManager, q: String, type: Class<A>, vararg params: Tuple<String, *>): TypedQuery<A> {
            return setParams(em.createNamedQuery(q, type), *params)
        }
    }

    /** JPQL queries with support for named parameters.  */
    val jpql: TypedQueriesBase<Tuple<String, *>> = object : TypedQueriesBase<Tuple<String, *>>() {
        override fun query(em: EntityManager, q: String, vararg params: Tuple<String, *>): Query {
            return setParams(em.createQuery(q), *params)
        }

        override fun <A> query(em: EntityManager, q: String, type: Class<A>, vararg params: Tuple<String, *>): TypedQuery<A> {
            return setParams(em.createQuery(q, type), *params)
        }
    }

    /** Native SQL queries. Only support positional parameters.  */
    val sql: QueriesBase<Any> = object : QueriesBase<Any>() {
        override fun query(em: EntityManager, q: String, vararg params: Any): Query {
            return setParams(em.createNativeQuery(q), *params)
        }
    }

    /** [javax.persistence.EntityManager.find] as a function wrapping the result into an Option.  */
    fun <A> find(clazz: Class<A>, primaryKey: Any): Function<EntityManager, Option<A>> {
        return object : Function<EntityManager, Option<A>>() {
            override fun apply(em: EntityManager): Option<A> {
                return option(em.find(clazz, primaryKey))
            }
        }
    }

    /** [javax.persistence.EntityManager.persist] as a function.  */
    fun <A> persist(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                em.persist(a)
                return a
            }
        }
    }

    /** [javax.persistence.EntityManager.merge] as a function.  */
    fun <A> merge(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return em.merge(a)
            }
        }
    }

    /** [javax.persistence.EntityManager.remove] as a function.  */
    fun <A> remove(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A? {
                em.remove(a)
                return null
            }
        }
    }

    fun <A> persistOrUpdate(em: EntityManager, a: A): A {
        val id = em.entityManagerFactory.persistenceUnitUtil.getIdentifier(a)
        if (id == null) {
            em.persist(a)
            return a
        } else {
            val dto = em.find<out Any>(a.javaClass, id) as A
            if (dto == null) {
                em.persist(a)
                return a
            } else {
                return em.merge(a)
            }
        }
    }

    fun <A> persistOrUpdate(a: A): Function<EntityManager, A> {
        return object : Function<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return persistOrUpdate(em, a)
            }
        }
    }

    /**
     * Set a list of named parameters on a query.
     *
     * Values of type [java.util.Date] and [org.joda.time.base.AbstractInstant]
     * are recognized and set as a timestamp ([javax.persistence.TemporalType.TIMESTAMP].
     */
    fun <A : Query> setParams(q: A, vararg params: Tuple<String, *>): A {
        for (p in params) {
            val value = p.b
            if (value is Date) {
                q.setParameter(p.a, value, TemporalType.TIMESTAMP)
            }
            if (value is AbstractInstant) {
                q.setParameter(p.a, value.toDate(), TemporalType.TIMESTAMP)
            } else {
                q.setParameter(p.a, p.b)
            }
        }
        return q
    }

    /**
     * Set a list of positional parameters on a query.
     *
     * Values of type [java.util.Date] and [org.joda.time.base.AbstractInstant]
     * are recognized and set as a timestamp ([javax.persistence.TemporalType.TIMESTAMP].
     */
    fun <A : Query> setParams(q: A, vararg params: Any): A {
        for (i in params.indices) {
            val value = params[i]
            if (value is Date) {
                q.setParameter(i + 1, value, TemporalType.TIMESTAMP)
            }
            if (value is AbstractInstant) {
                q.setParameter(i + 1, value.toDate(), TemporalType.TIMESTAMP)
            } else {
                q.setParameter(i + 1, value)
            }
        }
        return q
    }

    // -------------------------------------------------------------------------------------------------------------------

    abstract class TypedQueriesBase<P> protected constructor() : QueriesBase<P>() {

        /**
         * Create a typed query from `q` with a list of parameters.
         * Values of type [java.util.Date] are recognized
         * and set as a timestamp ([javax.persistence.TemporalType.TIMESTAMP].
         */
        abstract fun <A> query(em: EntityManager,
                               queryName: String,
                               type: Class<A>,
                               vararg params: Tuple<String, *>): TypedQuery<A>

        /** Find multiple entities.  */
        fun <A> findAll(em: EntityManager,
                        q: String,
                        type: Class<A>,
                        vararg params: Tuple<String, *>): List<A> {
            return query(em, q, type, *params).resultList
        }

        /** [.findAll] as a function.  */
        fun <A> findAll(q: String,
                        type: Class<A>,
                        vararg params: Tuple<String, *>): Function<EntityManager, List<A>> {
            return object : Function<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, type, *params)
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------------

    abstract class QueriesBase<P> protected constructor() {

        /**
         * Create a query from `q` with a list of parameters.
         * Values of type [java.util.Date] are recognized
         * and set as a timestamp ([javax.persistence.TemporalType.TIMESTAMP].
         */
        abstract fun query(em: EntityManager, q: String, vararg params: P): Query

        /** Run an update (UPDATE or DELETE) query and ensure that at least one row got affected.  */
        fun update(em: EntityManager, q: String, vararg params: P): Boolean {
            return query(em, q, *params).executeUpdate() > 0
        }

        /** [.update] as a function.  */
        fun update(q: String, vararg params: P): Function<EntityManager, Boolean> {
            return object : Function<EntityManager, Boolean>() {
                override fun apply(em: EntityManager): Boolean? {
                    return update(em, q, *params)
                }
            }
        }

        /**
         * Run a SELECT query that should return a single result.
         *
         * @return some value if the query yields exactly one result, none otherwise
         */
        fun <A> findSingle(em: EntityManager,
                           q: String,
                           vararg params: P): Option<A> {
            try {
                return some(query(em, q, *params).singleResult as A)
            } catch (e: NoResultException) {
                return none()
            } catch (e: NonUniqueResultException) {
                return none()
            }

        }

        /** [.findSingle] as a function.  */
        fun <A> findSingle(q: String,
                           vararg params: P): Function<EntityManager, Option<A>> {
            return object : Function<EntityManager, Option<A>>() {
                override fun apply(em: EntityManager): Option<A> {
                    return findSingle(em, q, *params)
                }
            }
        }

        /** Run a SELECT query and return only the first result item.  */
        fun <A> findFirst(em: EntityManager,
                          q: String,
                          vararg params: P): Option<A> {
            try {
                return some(query(em, q, *params).setMaxResults(1).singleResult as A)
            } catch (e: NoResultException) {
                return none()
            } catch (e: NonUniqueResultException) {
                return none()
            }

        }

        /** [.findSingle] as a function.  */
        fun <A> findFirst(q: String,
                          vararg params: P): Function<EntityManager, Option<A>> {
            return object : Function<EntityManager, Option<A>>() {
                override fun apply(em: EntityManager): Option<A> {
                    return findFirst(em, q, *params)
                }
            }
        }

        /** Run a COUNT(x) query.  */
        fun count(em: EntityManager, q: String, vararg params: P): Long {
            return query(em, q, *params).singleResult as Long
        }

        /** [.count] as a function.  */
        fun count(q: String, vararg params: P): Function<EntityManager, Long> {
            return object : Function<EntityManager, Long>() {
                override fun apply(em: EntityManager): Long? {
                    return count(em, q, *params)
                }
            }
        }

        /** Find multiple entities.  */
        fun <A> findAll(em: EntityManager, q: String, vararg params: P): List<A> {
            return query(em, q, *params).resultList as List<A>
        }

        /** [.findAll] as a function.  */
        fun <A> findAll(q: String,
                        vararg params: P): Function<EntityManager, List<A>> {
            return object : Function<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, *params)
                }
            }
        }

        /** Find multiple entities and wrap the in the list monad.  */
        fun <A> findAllM(em: EntityManager, q: String, vararg params: P): Monadics.ListMonadic<A> {
            return mlist(this.findAll(em, q, *params))
        }

        /** [.findAllM] as a function.  */
        fun <A> findAllM(q: String,
                         vararg params: P): Function<EntityManager, Monadics.ListMonadic<A>> {
            return object : Function<EntityManager, Monadics.ListMonadic<A>>() {
                override fun apply(em: EntityManager): Monadics.ListMonadic<A> {
                    return findAllM(em, q, *params)
                }
            }
        }

        /** Find multiple objects with optional pagination.  */
        fun <A> findAll(em: EntityManager,
                        q: String,
                        offset: Option<Int>,
                        limit: Option<Int>,
                        vararg params: P): List<A> {
            val query = query(em, q, *params)
            for (x in offset) query.firstResult = x!!
            for (x in limit) query.maxResults = x!!
            return query.resultList as List<A>
        }

        /** [.findAll] as a function.  */
        fun <A> findAll(q: String,
                        offset: Option<Int>,
                        limit: Option<Int>,
                        vararg params: P): Function<EntityManager, List<A>> {
            return object : Function<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, offset, limit, *params)
                }
            }
        }

        /** Find multiple objects with optional pagination wrapped in the list monad.  */
        fun <A> findAllM(em: EntityManager,
                         q: String,
                         offset: Option<Int>,
                         limit: Option<Int>,
                         vararg params: P): Monadics.ListMonadic<A> {
            return mlist(this.findAll(em, q, offset, limit, *params))
        }

        /** [.findAllM] as a function.  */
        fun <A> findAllM(q: String,
                         offset: Option<Int>,
                         limit: Option<Int>,
                         vararg params: P): Function<EntityManager, Monadics.ListMonadic<A>> {
            return object : Function<EntityManager, Monadics.ListMonadic<A>>() {
                override fun apply(em: EntityManager): Monadics.ListMonadic<A> {
                    return findAllM(em, q, offset, limit, *params)
                }
            }
        }

        /** Find multiple objects with pagination.  */
        fun <A> findAll(em: EntityManager,
                        q: String,
                        offset: Int,
                        limit: Int,
                        vararg params: P): List<A> {
            val query = query(em, q, *params)
            query.firstResult = offset
            query.maxResults = limit
            return query.resultList as List<A>
        }

        fun <A> findAll(q: String,
                        offset: Int,
                        limit: Int,
                        vararg params: P): Function<EntityManager, List<A>> {
            return object : Function<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, offset, limit, *params)
                }
            }
        }

        /** Find multiple objects with pagination wrapped in the list monad.  */
        fun <A> findAllM(em: EntityManager,
                         q: String,
                         offset: Int,
                         limit: Int,
                         vararg params: P): Monadics.ListMonadic<A> {
            return mlist(this.findAll(em, q, offset, limit, *params))
        }

        /** [.findAllM] as a function.  */
        fun <A> findAllM(q: String,
                         offset: Int,
                         limit: Int,
                         vararg params: P): Function<EntityManager, Monadics.ListMonadic<A>> {
            return object : Function<EntityManager, Monadics.ListMonadic<A>>() {
                override fun apply(em: EntityManager): Monadics.ListMonadic<A> {
                    return findAllM(em, q, offset, limit, *params)
                }
            }
        }
    }
}
