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

package org.echocat.adam.access;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.user.EntityException;
import com.atlassian.user.GroupManager;
import com.atlassian.user.User;
import org.echocat.adam.configuration.access.view.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.atlassian.confluence.security.Permission.ADMINISTER;
import static com.atlassian.confluence.security.PermissionManager.TARGET_SYSTEM;
import static org.echocat.adam.access.ViewAccess.Visibility.isBestVisibility;
import static org.echocat.adam.access.ViewAccess.Visibility.isBetterVisibility;
import static org.echocat.adam.access.ViewEditAccess.Editeditability.isBestEditeditability;
import static org.echocat.adam.access.ViewEditAccess.Editeditability.isBetterEditeditability;

public class AccessProvider {

    @Nonnull
    private final PermissionManager _permissionManager;
    @Nonnull
    private final GroupManager _groupManager;

    @Nonnull
    private final ViewEditAccess _viewEditAccess = new DummyAccess();
    @Nonnull
    private final ViewAccess _viewAccess = new DummyAccess();

    public AccessProvider(@Nonnull PermissionManager permissionManager, @Nonnull GroupManager groupManager) {
        _permissionManager = permissionManager;
        _groupManager = groupManager;
    }

    @Nonnull
    public ViewEditAccess provideFor(@Nullable org.echocat.adam.configuration.access.viewedit.ViewEditAccess source) {
        return source != null ? new ViewEditAccessImpl(source) : new DummyAccess();
    }

    @Nonnull
    public ViewAccess provideFor(@Nullable org.echocat.adam.configuration.access.view.ViewAccess source) {
        return source != null ? new ViewAccessImpl(source) : new DummyAccess();
    }

    @Nonnull
    public ViewAccess allowAllViewEditAccess() {
        return _viewEditAccess;
    }

    @Nonnull
    public ViewAccess allowAllViewAccess() {
        return _viewAccess;
    }

    protected class DummyAccess implements ViewEditAccess {

        @Nonnull
        @Override
        public Visibility checkView(@Nonnull User forUser, @Nullable User target) {
            return Visibility.allowed;
        }

        @Nonnull
        @Override
        public Editeditability checkEdit(@Nonnull User forUser, @Nullable User target) {
            return Editeditability.allowed;
        }

    }

    protected class ViewEditAccessImpl extends ViewAccessImpl implements ViewEditAccess {

        @Nonnull
        private final org.echocat.adam.configuration.access.viewedit.ViewEditAccess _original;

        public ViewEditAccessImpl(@Nonnull org.echocat.adam.configuration.access.viewedit.ViewEditAccess original) {
            super(original);
            _original = original;
        }

        @Nonnull
        @Override
        public Editeditability checkEdit(@Nullable User forUser, @Nullable User target) {
            Editeditability result = null;
            if (forUser == null) {
                result = Editeditability.forbidden;
            } else {
                final List<org.echocat.adam.configuration.access.viewedit.Group> groups = _original.getGroups();
                if (groups != null) {
                    final Set<String> groupMemberships = getGroupMembershipsOf(forUser);
                    for (final Iterator<org.echocat.adam.configuration.access.viewedit.Group> i = groups.iterator(); !isBestEditeditability(result) && i.hasNext();) {
                        final org.echocat.adam.configuration.access.viewedit.Group group = i.next();
                        if (group != null) {
                            final String groupName = group.getName();
                            if (groupMemberships.contains(groupName)) {
                                final Editeditability candidate = group.getEdit();
                                if (isBetterEditeditability(candidate, result)) {
                                    result = candidate;
                                }
                            }
                        }
                    }
                }
                if (!isBestEditeditability(result) && isEqual(forUser, target)) {
                    final org.echocat.adam.configuration.access.viewedit.Owner owner = _original.getOwner();
                    if (owner != null) {
                        final Editeditability candidate = owner.getEdit();
                        if (isBetterEditeditability(candidate, result)) {
                            result = candidate;
                        }
                    }
                }
                if (!isBestEditeditability(result) && _permissionManager.hasPermission(forUser, ADMINISTER, TARGET_SYSTEM)) {
                    final org.echocat.adam.configuration.access.viewedit.Administrator administrator = _original.getAdministrator();
                    if (administrator != null) {
                        final Editeditability candidate = administrator.getEdit();
                        if (isBetterEditeditability(candidate, result)) {
                            result = candidate;
                        }
                    }
                }
            }
            if (result == null) {
                final org.echocat.adam.configuration.access.viewedit.Default all = _original.getDefault();
                if (all != null) {
                    result = all.getEdit();
                }
            }
            if (result == null) {
                result = _original.hasAnyRule() ? Editeditability.forbidden : Editeditability.allowed;
            }
            return result;
        }

    }

    protected class ViewAccessImpl implements ViewAccess {

        @Nonnull
        private final ViewAccessSupport<?, ?, ?, ?, ?> _original;

        public ViewAccessImpl(@Nonnull ViewAccessSupport<?, ?, ?, ?, ?> original) {
            _original = original;
        }

        @Nonnull
        @Override
        public Visibility checkView(@Nullable User forUser, @Nullable User target) {
            Visibility result = null;
            if (forUser == null) {
                final Anonymous anonymous = _original.getAnonymous();
                if (anonymous != null) {
                    result = anonymous.getView();
                }
            } else {
                final List<? extends Group> groups = _original.getGroups();
                if (groups != null) {
                    final Set<String> groupMemberships = getGroupMembershipsOf(forUser);
                    for (final Iterator<? extends Group> i = groups.iterator(); !isBestVisibility(result) && i.hasNext();) {
                        final Group group = i.next();
                        if (group != null) {
                            final String groupName = group.getName();
                            if (groupMemberships.contains(groupName)) {
                                final Visibility candidate = group.getView();
                                if (isBetterVisibility(candidate, result)) {
                                    result = candidate;
                                }
                            }
                        }
                    }
                }
                if (!isBestVisibility(result) && isEqual(forUser, target)) {
                    final Owner owner = _original.getOwner();
                    if (owner != null) {
                        final Visibility candidate = owner.getView();
                        if (isBetterVisibility(candidate, result)) {
                            result = candidate;
                        }
                    }
                }
                if (!isBestVisibility(result) && _permissionManager.hasPermission(forUser, ADMINISTER, TARGET_SYSTEM)) {
                    final Administrator administrator = _original.getAdministrator();
                    if (administrator != null) {
                        final Visibility candidate = administrator.getView();
                        if (isBetterVisibility(candidate, result)) {
                            result = candidate;
                        }
                    }
                }
            }
            if (result == null) {
                final Default all = _original.getDefault();
                if (all != null) {
                    result = all.getView();
                }
            }
            if (result == null) {
                result = _original.hasAnyRule() ? Visibility.forbidden : Visibility.allowed;
            }
            return result;
        }

        protected boolean isEqual(@Nullable User left, @Nullable User right) {
            final boolean result;
            if (left == null || right == null) {
                result = false;
            } else {
                result = left.getName().equals(right.getName());
            }
            return result;
        }

        @Nonnull
        protected Set<String> getGroupMembershipsOf(@Nonnull User user) {
            final Set<String> result = new HashSet<>();
            try {
                for (final com.atlassian.user.Group group : _groupManager.getGroups(user)) {
                    result.add(group.getName());
                }
            } catch (final EntityException e) {
                throw new RuntimeException("Could not determinate the groups of " + user + ".", e);
            }
            return result;
        }

    }

}
