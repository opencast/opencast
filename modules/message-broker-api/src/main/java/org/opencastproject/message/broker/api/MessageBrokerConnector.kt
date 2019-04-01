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

package org.opencastproject.message.broker.api

interface MessageBrokerConnector {

    /**
     * Return if there is a connection to the message broker.
     *
     * @return If the connection to the message broker is active.
     */
    val isConnected: Boolean

    /**
     * Try reconnecting if there was no reconnection attempt in the immediate past.
     *
     * @return If the re-connection attempt was successful.
     */
    fun reconnect(): Boolean

}
