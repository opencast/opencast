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

package org.opencastproject.messages;

import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Strings;

import org.apache.commons.lang.StringUtils;

import java.util.List;

/** Mail abstraction. */
public final class Mail {
  private final EmailAddress sender;
  private final Option<EmailAddress> replyTo;
  private final List<EmailAddress> recipients;
  private final String subject;
  private final String body;

  public Mail(EmailAddress sender, Option<EmailAddress> replyTo, List<EmailAddress> recipients, String subject,
          String body) {
    this.sender = sender;
    this.replyTo = replyTo;
    this.recipients = recipients;
    this.subject = subject;
    this.body = body;
  }

  public EmailAddress getSender() {
    return sender;
  }

  public Option<EmailAddress> getReplyTo() {
    return replyTo;
  }

  public List<EmailAddress> getRecipients() {
    return recipients;
  }

  public String getBody() {
    return body;
  }

  public String getSubject() {
    return subject;
  }

  @Override
  public String toString() {
    List<String> recipientsAsString = Monadics.mlist(recipients).map(toString).value();
    return "From: "
            + sender
            + "\n"
            + "To: "
            + StringUtils.join(recipientsAsString, ",")
            + "\n"
            + replyTo.bind(Strings.<EmailAddress> asString()).map(Strings.prepend("ReplyTo: "))
                    .map(Strings.append("\n")).getOrElse("") + "Subject: " + subject + "\n\n" + body;
  }

  private final Function<EmailAddress, String> toString = new Function<EmailAddress, String>() {
    @Override
    public String apply(EmailAddress a) {
      return a.getName() + " <" + a.getAddress() + ">";
    }

  };
}
