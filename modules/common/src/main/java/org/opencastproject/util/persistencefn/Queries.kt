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

package org.opencastproject.util.persistencefn

import com.entwinemedia.fn.Stream.`$`

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Monoid
import com.entwinemedia.fn.P2
import com.entwinemedia.fn.Unit
import com.entwinemedia.fn.data.Opt

import org.joda.time.base.AbstractInstant

import java.util.Date

import javax.persistence.EntityManager
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException
import javax.persistence.Query
import javax.persistence.TemporalType
import javax.persistence.TypedQuery

/**
 * JPA query constructors.
 */
object Queries {

    // -------------------------------------------------------------------------------------------------------------------

    /** Named queries with support for named parameters.  */
    val named: TypedQueriesBase<P2<String, *>> = object : TypedQueriesBase<P2<String, *>>() {
        override fun query(em: EntityManager, q: String, vararg params: P2<String, *>): Query {
            return setParams(em.createNamedQuery(q), *params)
        }

        override fun <A> query(em: EntityManager, q: String, type: Class<A>, vararg params: P2<String, *>): TypedQuery<A> {
            return setParams(em.createNamedQuery(q, type), *params)
        }
    }

    /** JPQL queries with support for named parameters.  */
    val jpql: TypedQueriesBase<P2<String, *>> = object : TypedQueriesBase<P2<String, *>>() {
        override fun query(em: EntityManager, q: String, vararg params: P2<String, *>): Query {
            return setParams(em.createQuery(q), *params)
        }

        override fun <A> query(em: EntityManager, q: String, type: Class<A>, vararg params: P2<String, *>): TypedQuery<A> {
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
    fun <A> find(clazz: Class<A>, primaryKey: Any): Fn<EntityManager, Opt<A>> {
        return object : Fn<EntityManager, Opt<A>>() {
            override fun apply(em: EntityManager): Opt<A> {
                return Opt.nul(em.find(clazz, primaryKey))
            }
        }
    }

    /** [javax.persistence.EntityManager.persist] as a function.  */
    fun <A> persist(a: A): Fn<EntityManager, A> {
        return object : Fn<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                em.persist(a)
                return a
            }
        }
    }

    /** [javax.persistence.EntityManager.merge] as a function.  */
    fun <A> merge(a: A): Fn<EntityManager, A> {
        return object : Fn<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return em.merge(a)
            }
        }
    }

    /**
     * [javax.persistence.EntityManager.contains] as a function.
     * The function returns true if the entity is a managed instance belonging to the current persistence context.
     */
    fun contains(a: Any): Fn<EntityManager, Boolean> {
        return object : Fn<EntityManager, Boolean>() {
            override fun apply(em: EntityManager): Boolean? {
                return em.contains(a)
            }
        }
    }

    /**
     * Convenience for `EntityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(a)`.
     * The function returns the ID or null if the object does not have an ID yet, i.e. is not yet persisted.
     */
    fun getId(em: EntityManager, a: Any): Any? {
        return em.entityManagerFactory.persistenceUnitUtil.getIdentifier(a)
    }

    /**
     * [.getId] as a function.
     */
    fun <A> getId(a: Any): Fn<EntityManager, A> {
        return object : Fn<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return getId(em, a) as A?
            }
        }
    }

    /**
     * Like [javax.persistence.EntityManager.remove] but the entity is allowed
     * to be detached. It will be merged into the current persistence context if necessary.
     */
    fun remove(em: EntityManager, a: Any) {
        if (em.contains(a)) {
            em.remove(a)
        } else {
            em.remove(em.merge(a))
        }
    }

    /** [.remove] as a function.  */
    fun remove(a: Any): Fn<EntityManager, Unit> {
        return object : Fn<EntityManager, Unit>() {
            override fun apply(em: EntityManager): Unit {
                remove(em, a)
                return Unit.unit
            }
        }
    }

    /**
     * If the object does not have an ID, persist it. If it does, try to update it.
     */
    fun <A> persistOrUpdate(em: EntityManager, a: A): A {
        val id = getId(em, a)
        if (id == null) {
            // object does not have an ID
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

    /**
     * [.persistOrUpdate] as a function.
     */
    fun <A> persistOrUpdate(a: A): Fn<EntityManager, A> {
        return object : Fn<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return persistOrUpdate(em, a)
            }
        }
    }

    /**
     * Set a list of named parameters on a query.
     *
     *
     * Values of type [java.util.Date] and [org.joda.time.base.AbstractInstant]
     * are recognized and set as a timestamp ([javax.persistence.TemporalType.TIMESTAMP].
     */
    fun <A : Query> setParams(q: A, vararg params: P2<String, *>): A {
        for (p in params) {
            val value = p.get2()
            if (value is Date) {
                q.setParameter(p.get1(), value, TemporalType.TIMESTAMP)
            }
            if (value is AbstractInstant) {
                q.setParameter(p.get1(), value.toDate(), TemporalType.TIMESTAMP)
            } else {
                q.setParameter(p.get1(), p.get2())
            }
        }
        return q
    }

    /**
     * Set a list of positional parameters on a query.
     *
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

    /**
     * Compose multiple queries `qs` and sum up their results.
     *
     * @param m monoid to sum up results
     * @param qs list of queries
     * @return the sum of the results
     */
    @SafeVarargs
    fun <A> sum(m: Monoid<A>, vararg qs: Fn<EntityManager, A>): Fn<EntityManager, A> {
        return object : Fn<EntityManager, A>() {
            override fun apply(em: EntityManager): A {
                return `$`(*qs).map(object : Fn<Fn<EntityManager, A>, A>() {
                    override fun apply(q: Fn<EntityManager, A>): A {
                        return q.apply(em)
                    }
                }).sum(m)
            }
        }
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
                               vararg params: P2<String, *>): TypedQuery<A>

        /** Find multiple entities.  */
        fun <A> findAll(em: EntityManager,
                        q: String,
                        type: Class<A>,
                        vararg params: P2<String, *>): List<A> {
            return query(em, q, type, *params).resultList
        }

        /** [.findAll] as a function.  */
        fun <A> findAll(q: String,
                        type: Class<A>,
                        vararg params: P2<String, *>): Fn<EntityManager, List<A>> {
            return object : Fn<EntityManager, List<A>>() {
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
        fun update(q: String, vararg params: P): Fn<EntityManager, Boolean> {
            return object : Fn<EntityManager, Boolean>() {
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
                           vararg params: P): Opt<A> {
            try {
                return Opt.some(query(em, q, *params).singleResult as A)
            } catch (e: NoResultException) {
                return Opt.none()
            } catch (e: NonUniqueResultException) {
                return Opt.none()
            }

        }

        /** [.findSingle] as a function.  */
        fun <A> findSingle(q: String,
                           vararg params: P): Fn<EntityManager, Opt<A>> {
            return object : Fn<EntityManager, Opt<A>>() {
                override fun apply(em: EntityManager): Opt<A> {
                    return findSingle(em, q, *params)
                }
            }
        }

        /** Run a SELECT query and return only the first result item.  */
        fun <A> findFirst(em: EntityManager,
                          q: String,
                          vararg params: P): Opt<A> {
            try {
                return Opt.some(query(em, q, *params).setMaxResults(1).singleResult as A)
            } catch (e: NoResultException) {
                return Opt.none()
            } catch (e: NonUniqueResultException) {
                return Opt.none()
            }

        }

        /** [.findSingle] as a function.  */
        fun <A> findFirst(q: String,
                          vararg params: P): Fn<EntityManager, Opt<A>> {
            return object : Fn<EntityManager, Opt<A>>() {
                override fun apply(em: EntityManager): Opt<A> {
                    return findFirst(em, q, *params)
                }
            }
        }

        /** Run a COUNT(x) query.  */
        fun count(em: EntityManager, q: String, vararg params: P): Long {
            return query(em, q, *params).singleResult as Long
        }

        /** [.count] as a function.  */
        fun count(q: String, vararg params: P): Fn<EntityManager, Long> {
            return object : Fn<EntityManager, Long>() {
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
                        vararg params: P): Fn<EntityManager, List<A>> {
            return object : Fn<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, *params)
                }
            }
        }

        /** Find multiple objects with optional pagination.  */
        fun <A> findAll(em: EntityManager,
                        q: String,
                        offset: Opt<Int>,
                        limit: Opt<Int>,
                        vararg params: P): List<A> {
            val query = query(em, q, *params)
            for (x in offset) query.firstResult = x!!
            for (x in limit) query.maxResults = x!!
            return query.resultList as List<A>
        }

        fun <A> findAll(q: String,
                        offset: Opt<Int>,
                        limit: Opt<Int>,
                        vararg params: P): Fn<EntityManager, List<A>> {
            return object : Fn<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, offset, limit, *params)
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
                        vararg params: P): Fn<EntityManager, List<A>> {
            return object : Fn<EntityManager, List<A>>() {
                override fun apply(em: EntityManager): List<A> {
                    return findAll(em, q, offset, limit, *params)
                }
            }
        }
    }
}
