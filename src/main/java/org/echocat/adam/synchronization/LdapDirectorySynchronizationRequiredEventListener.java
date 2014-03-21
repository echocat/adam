/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * echocat Adam, Copyright (c) 2014 echocat
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *
 * *** END LICENSE BLOCK *****
 ****************************************************************************************/

package org.echocat.adam.synchronization;

import com.atlassian.confluence.event.events.security.LoginEvent;
import com.atlassian.event.api.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class LdapDirectorySynchronizationRequiredEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(LdapDirectorySynchronizationRequiredEventListener.class);

    @Nonnull
    private final LdapDirectorySynchronizer _synchronizer;

    public LdapDirectorySynchronizationRequiredEventListener(@Nonnull LdapDirectorySynchronizer synchronizer) {
        _synchronizer = synchronizer;
    }

    @EventListener
    public void handleLogin(@Nonnull LoginEvent event) {
        final String username = event.getUsername();
        try {
            _synchronizer.synchronize(username);
        } catch (final Exception e) {
            LOG.warn("The user '" + username + " could not be synchronized. This means that this user could be out of date.", e);
        }
    }

}