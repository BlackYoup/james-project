/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets.jsieve.delivery;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.transport.mailets.delivery.MailboxAppender;
import org.apache.james.transport.mailets.jsieve.Poster;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.mailet.MailAddress;

public class SievePoster implements Poster {

    private final MailboxAppender mailboxAppender;
    private final String folder;
    private final UsersRepository usersRepos;

    public SievePoster(MailboxAppender mailboxAppender, String folder, UsersRepository usersRepos) {
        this.mailboxAppender = mailboxAppender;
        this.folder = folder;
        this.usersRepos = usersRepos;
    }

    @Override
    public void post(String url, MimeMessage mail) throws MessagingException {
        int endOfScheme = url.indexOf(':');
        if (endOfScheme < 0) {
            throw new MessagingException("Malformed URI");
        } else {
            String scheme = url.substring(0, endOfScheme);
            if (scheme.equals("mailbox")) {
                handleMailboxProtocol(url, mail, endOfScheme);
            } else {
                throw new MessagingException("Unsupported protocol");
            }
        }
    }

    private void handleMailboxProtocol(String url, MimeMessage mail, int endOfScheme) throws MessagingException {
        int startOfUser = endOfScheme + 3;
        int endOfUser = url.indexOf('@', startOfUser);
        int startOfHost = endOfUser + 1;
        int endOfHost = url.indexOf('/', startOfHost);
        if (endOfUser < 0) {
            // TODO: When user missing, append to a default location
            throw new MessagingException("Shared mailbox is not supported");
        } else {
            String host = url.substring(startOfHost, endOfHost);
            String user = parseUser(url, startOfUser, endOfUser, host);
            String urlPath = parseUrlPath(url, endOfHost);

            mailboxAppender.appendAndUseSlashAsSeparator(mail, user, urlPath, folder);
        }
    }

    private String parseUrlPath(String url, int endOfHost) {
        int length = url.length();
        if (endOfHost + 1 == length) {
            return this.folder;
        } else {
            return url.substring(endOfHost, length);
        }
    }

    private String parseUser(String url, int startOfUser, int endOfUser, String host) throws MessagingException {
        // lowerCase the user - see
        // https://issues.apache.org/jira/browse/JAMES-1369
        String user = url.substring(startOfUser, endOfUser).toLowerCase();
        // Check if we should use the full email address as username
        try {
            return usersRepos.getUser(new MailAddress(user, host));
        } catch (UsersRepositoryException e) {
            throw new MessagingException("Unable to accessUsersRepository", e);
        }
    }
}
