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

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.search.ConfluenceIndexer;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.UserDetailsManager;
import com.atlassian.crowd.model.user.UserWithAttributes;
import com.atlassian.user.User;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProfileProvider implements DisposableBean {

    @Nullable
    private static ProfileProvider c_instance;

    @Nonnull
    public static ProfileProvider profileProvider() {
        final ProfileProvider result = c_instance;
        if (result == null) {
            throw new IllegalStateException("There is currently no instance registered.");
        }
        return result;
    }

    @Nonnull
    private final UserAccessor _userAccessor;
    @Nonnull
    private final BandanaManager _bandanaManager;
    @Nonnull
    private final UserDetailsManager _userDetailsManager;
    @Nonnull
    private final PersonalInformationManager _personalInformationManager;
    @Nonnull
    private final ConfluenceIndexer _confluenceIndexer;

    public ProfileProvider(@Nonnull UserAccessor userAccessor, @Nonnull BandanaManager bandanaManager, @Nonnull UserDetailsManager userDetailsManager, @Nonnull PersonalInformationManager personalInformationManager, @Nonnull ConfluenceIndexer confluenceIndexer) {
        _userAccessor = userAccessor;
        _bandanaManager = bandanaManager;
        _userDetailsManager = userDetailsManager;
        _personalInformationManager = personalInformationManager;
        _confluenceIndexer = confluenceIndexer;
        c_instance = this;
    }

    @Nonnull
    public Profile provideFor(@Nonnull User user) {
        return new Profile(user, _bandanaManager, _userDetailsManager, _personalInformationManager, _confluenceIndexer, _userAccessor);
    }

    @Nonnull
    public Profile provideFor(@Nonnull UserWithAttributes user) {
        return new Profile(user.getName(), user.getDisplayName(), user.getEmailAddress(), _bandanaManager, _userDetailsManager, _personalInformationManager, _confluenceIndexer, _userAccessor);
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
