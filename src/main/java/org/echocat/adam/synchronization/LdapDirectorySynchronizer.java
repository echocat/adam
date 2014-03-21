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

import com.atlassian.crowd.directory.RemoteDirectory;
import com.atlassian.crowd.embedded.api.CrowdDirectoryService;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.model.user.UserWithAttributes;
import com.atlassian.crowd.search.EntityDescriptor;
import com.atlassian.crowd.search.builder.Combine;
import com.atlassian.crowd.search.builder.QueryBuilder;
import com.atlassian.crowd.search.query.entity.EntityQuery;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.google.common.base.Predicate;
import org.echocat.adam.directory.DirectoryHelper;
import org.echocat.adam.directory.ExtendingLDAPDirectoryInstanceLoader;
import org.echocat.adam.profile.Profile;
import org.echocat.adam.profile.ProfileProvider;
import org.echocat.adam.profile.element.ElementModel;
import org.echocat.adam.profile.element.ElementRenderer;
import org.echocat.jomon.runtime.concurrent.StopWatch;
import org.echocat.jomon.runtime.iterators.ChainedIterator;
import org.echocat.jomon.runtime.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.atlassian.crowd.embedded.api.DirectoryType.CONNECTOR;
import static org.apache.commons.lang3.StringUtils.join;
import static org.echocat.jomon.runtime.CollectionUtils.*;
import static org.echocat.jomon.runtime.iterators.IteratorUtils.filter;

public class LdapDirectorySynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(LdapDirectorySynchronizer.class);

    @Nonnull
    private final ExtendingLDAPDirectoryInstanceLoader _loader;
    @Nonnull
    private final CrowdDirectoryService _crowdDirectoryService;
    @Nonnull
    private final TransactionTemplate _transactionTemplate;
    @Nonnull
    private final DirectoryHelper _directoryHelper;
    @Nonnull
    private final ProfileProvider _profileProvider;
    @Nonnull
    private final ElementRenderer _elementRenderer;

    @Autowired
    public LdapDirectorySynchronizer(
        @Nonnull ExtendingLDAPDirectoryInstanceLoader loader,
        @Nonnull CrowdDirectoryService crowdDirectoryService,
        @Nonnull TransactionTemplate transactionTemplate,
        @Nonnull DirectoryHelper directoryHelper,
        @Nonnull ProfileProvider profileProvider,
        @Nonnull ElementRenderer elementRenderer) {
        _loader = loader;
        _crowdDirectoryService = crowdDirectoryService;
        _transactionTemplate = transactionTemplate;
        _directoryHelper = directoryHelper;
        _profileProvider = profileProvider;
        _elementRenderer = elementRenderer;
    }

    @Nonnull
    protected Iterator<UserWithAttributes> userIterator(@Nullable Iterable<String> specifiedUserNames) throws Exception {
        final EntityQuery<UserWithAttributes> query = queryFor(specifiedUserNames);
        final Iterator<Directory> directories = configuredDirectoriesIterator();
        final Iterator<RemoteDirectory> remoteDirectories = remoteDirectoriesIteratorFor(directories);
        final Iterator<UserWithAttributes> users = usersIteratorFor(remoteDirectories, query);
        return uniqueUsersIteratorFor(users);
    }

    @Nonnull
    protected Iterator<UserWithAttributes> uniqueUsersIteratorFor(@Nonnull Iterator<UserWithAttributes> original) {
        final Set<String> alreadyHandledUsers = new HashSet<>();
        return filter(original, new Predicate<UserWithAttributes>() {
            @Override
            public boolean apply(@Nullable UserWithAttributes input) {
                final boolean result;
                if (input != null) {
                    final String name = input.getName();
                    if (alreadyHandledUsers.contains(name)) {
                        alreadyHandledUsers.add(name);
                        result = false;
                    } else {
                        result = true;
                    }
                } else {
                    result = false;
                }
                return result;
            }
        });
    }

    @Nonnull
    protected EntityQuery<UserWithAttributes> queryFor(@Nullable Iterable<String> userNames) {
        final EntityQuery<UserWithAttributes> query;
        if (userNames != null) {
            final List<SearchRestriction> restrictionList = new ArrayList<>();
            for (final String userName : userNames) {
                final SearchRestriction searchRestriction = new TermRestriction<>(UserTermKeys.USERNAME, MatchMode.EXACTLY_MATCHES, userName);
                restrictionList.add(searchRestriction);
            }
            query = QueryBuilder.queryFor(UserWithAttributes.class, EntityDescriptor.user()).with(Combine.anyOf(restrictionList)).returningAtMost(-1);
        } else {
            query = QueryBuilder.queryFor(UserWithAttributes.class, EntityDescriptor.user()).returningAtMost(-1);
        }
        return query;
    }

    @Nonnull
    protected Iterator<Directory> configuredDirectoriesIterator() {
        return filter(_crowdDirectoryService.findAllDirectories().iterator(), new Predicate<Directory>() {
            @Override
            public boolean apply(@Nullable Directory directory) {
                final boolean result;
                if (directory != null && (directory.isActive()) && CONNECTOR.equals(directory.getType())) {
                    result = true;
                } else if (directory != null && LOG.isDebugEnabled()) {
                    result = false;
                    final String active = directory.isActive() ? "Activated" : "Deactivated";
                    LOG.debug(active + " directory with name \"" + directory.getName() + "\" of type " + directory.getType() + " will be ignored.");
                } else {
                    result = false;
                }
                return result;
            }
        });
    }

    @Nonnull
    private Iterator<RemoteDirectory> remoteDirectoriesIteratorFor(@Nonnull Iterator<Directory> directories) {
        return new ChainedIterator<Directory, RemoteDirectory>(directories) {
            @Nullable
            @Override
            protected Iterator<RemoteDirectory> nextIterator(@Nullable Directory directory) {
                final RemoteDirectory remoteDirectory = directory != null ? findRemoteDirectoryOf(directory) : null;
                return remoteDirectory != null ? asSingletonIterator(remoteDirectory) : null;
            }
        };
    }

    @Nonnull
    protected Iterator<UserWithAttributes> usersIteratorFor(@Nonnull Iterator<RemoteDirectory> remoteDirectories, @Nonnull final EntityQuery<UserWithAttributes> query) {
        return new ChainedIterator<RemoteDirectory, UserWithAttributes>(remoteDirectories) {
            @Nullable
            @Override
            protected Iterator<UserWithAttributes> nextIterator(@Nullable RemoteDirectory directory) {
                final Iterator<UserWithAttributes> result;
                if (directory != null) {
                    try {
                        result = directory.searchUsers(query).iterator();
                    } catch (final OperationFailedException e) {
                        throw new RuntimeException("Could not query users using " + query + " on " + directory + ".", e);
                    }
                } else {
                    result = null;
                }
                return result;
            }
        };
    }

    @Nonnull
    public Result synchronize() throws Exception {
        return synchronize((Iterable<String>) null);
    }

    @Nonnull
    public Result synchronize(@Nullable String... specifiedUserNames) throws Exception {
        return synchronize(specifiedUserNames != null ? asImmutableList(specifiedUserNames) : null);
    }

    @Nonnull
    public Result synchronize(@Nullable Iterable<String> specifiedUserNames) throws Exception {
        final Set<String> attributeKeys = _directoryHelper.getAllAttributeKeys();
        final List<ElementModel> elementModels = _directoryHelper.getAllElementModels();
        final StopWatch stopWatch = new StopWatch();
        int numberSynchronizedUsers = 0;

        final Iterator<UserWithAttributes> users = userIterator(specifiedUserNames);
        while (users.hasNext()) {
            final UserWithAttributes user = users.next();
            if (synchronize(elementModels, user)) {
                numberSynchronizedUsers++;
            }
        }

        return new Result(numberSynchronizedUsers, attributeKeys, stopWatch.getCurrentDuration());
    }

    protected boolean synchronize(@Nonnull Iterable<ElementModel> elementModels, @Nonnull UserWithAttributes ofUser) {
        synchronized (_transactionTemplate) {
            return _transactionTemplate.execute(new UserTransactionCallback(elementModels, ofUser));
        }
    }

    protected boolean synchronizeSafe(@Nonnull Iterable<ElementModel> elementModels, @Nonnull UserWithAttributes ofUser) {
        boolean result = true;
        try {
            final Profile profile = _profileProvider.provideFor(ofUser);
            for (final ElementModel elementModel : elementModels) {
                try {
                    final String value = extract(elementModel, profile, ofUser);
                    if (value != null) {
                        profile.setValue(elementModel, value);
                    }
                } catch (final Exception e) {
                    LOG.warn("It was not possible to set element " + elementModel.getId() + " at user '" + profile.getName() + "'.", e);
                }
            }
            profile.reIndex();
        } catch (final Exception e) {
            LOG.warn("An error occurred while persisting the data for user '" + ofUser.getName() + "'. There are no changes made on this user.", e);
            result = false;
        }
        return result;
    }

    @Nullable
    protected String extract(@Nonnull ElementModel elementModel, @Nonnull Profile profile, @Nonnull UserWithAttributes from) {
        final Map<String, Object> context = new HashMap<>();
        for (final String key : from.getKeys()) {
            final Set<String> values = from.getValues(key);
            if (!isEmpty(values)) {
                context.put(key, join(values, " "));
            }
        }
        return _elementRenderer.renderContent(elementModel, profile, context);
    }

    @Nullable
    protected RemoteDirectory findRemoteDirectoryOf(@Nonnull Directory dir) {
        return _loader.canLoad(dir.getImplementationClass()) ? _loader.getDirectory(dir) : null;
    }

    public class UserTransactionCallback implements TransactionCallback<Boolean> {

        @Nonnull
        private final Iterable<ElementModel> _elements;
        @Nonnull
        private final UserWithAttributes _user;

        public UserTransactionCallback(@Nonnull Iterable<ElementModel> elements, @Nonnull UserWithAttributes ofUser) {
            _elements = elements;
            _user = ofUser;
        }

        @Override
        public Boolean doInTransaction() {
            return synchronizeSafe(_elements, _user);
        }

    }

    public static class Result {

        @Nonnegative
        private final int _numberOfSynchronizedUsers;
        @Nonnull
        private final Set<String> _synchronizedAttributes;
        @Nonnull
        private final Duration _duration;

        public Result(@Nonnegative int numberOfSynchronizedUsers, @Nonnull Set<String> synchronizedAttributes, @Nonnull Duration duration) {
            _numberOfSynchronizedUsers = numberOfSynchronizedUsers;
            _synchronizedAttributes = synchronizedAttributes;
            _duration = duration;
        }

        @Nonnegative
        public int getNumberOfSynchronizedUsers() {
            return _numberOfSynchronizedUsers;
        }

        @Nonnull
        public Set<String> getSynchronizedAttributes() {
            return _synchronizedAttributes;
        }

        @Nonnull
        public Duration getDuration() {
            return _duration;
        }

        @Override
        public String toString() {
            return "Synchronized " + _numberOfSynchronizedUsers + " user(s) with attributes " + _synchronizedAttributes + " in " + _duration + ".";
        }

    }
}