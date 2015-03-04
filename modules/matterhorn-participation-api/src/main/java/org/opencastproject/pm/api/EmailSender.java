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
package org.opencastproject.pm.api;

import java.util.Collection;

/** Send PMM emails. */
public interface EmailSender {
  /**
   * Send emails based on <code>message</code> to the recipients referring to the given recordings and update object
   * associations.
   */
  void sendMessagesForRecordings(Collection<Recording> recording, Collection<Person> recipients, Message message,
          boolean store);

  String renderInvitationBody(Message template, Collection<Recording> recording, Person receiver);
}
