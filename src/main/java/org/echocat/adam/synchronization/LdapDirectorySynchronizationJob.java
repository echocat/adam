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

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.quartz.jobs.AbstractJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static org.echocat.adam.synchronization.LdapDirectorySynchronizer.Result;

public class LdapDirectorySynchronizationJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(LdapDirectorySynchronizationJob.class);

    @Nonnull
    private final LdapDirectorySynchronizer _synchronizer;
    @Nonnull
    private final EventPublisher _eventPublisher;

    public LdapDirectorySynchronizationJob(@Nonnull LdapDirectorySynchronizer synchronizer, @Nonnull EventPublisher eventPublisher) {
        _synchronizer = synchronizer;
        _eventPublisher = eventPublisher;
    }

    @Override
    public void doExecute(@Nonnull JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOG.info("Synchronization of all LDAP users started.");
        try {
            final Result result = _synchronizer.synchronize();
            _eventPublisher.publish(new EventImpl(this));
            LOG.info("Synchronization of all LDAP users finished. Result: " + result);
        } catch (final Exception e) {
            LOG.info("Synchronization of all LDAP users failed.", e);
        }
    }

    public static class EventImpl extends ConfluenceEvent {

        private static final long serialVersionUID = 9834873498324L;

        public EventImpl(Object src) {
            super(src);
        }
    }
}