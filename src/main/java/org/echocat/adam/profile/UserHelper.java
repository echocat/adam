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

package org.echocat.adam.profile;

import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.security.SpacePermissionManager;

import javax.annotation.Nonnull;

import static com.atlassian.confluence.user.AuthenticatedUserThreadLocal.isAnonymousUser;

public class UserHelper {

    public static final SpacePermission VIEW_USER_PROFILE_PERMISSION = new SpacePermission("VIEWUSERPROFILES");

    @Nonnull
    private final SpacePermissionManager _spacePermissionManager;

    public UserHelper(SpacePermissionManager spacePermissionManager) {
        _spacePermissionManager = spacePermissionManager;
    }

    public boolean isProfileViewPermitted() {
        return !isAnonymousUser() || _spacePermissionManager.permissionExists(VIEW_USER_PROFILE_PERMISSION);
    }

}
