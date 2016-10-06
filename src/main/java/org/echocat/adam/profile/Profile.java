/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * echocat Adam, Copyright (c) 2014-2016 echocat
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
import com.atlassian.confluence.user.PersonalInformation;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.UserDetailsManager;
import com.atlassian.user.User;
import org.echocat.adam.profile.element.ElementModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;

import static com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext.GLOBAL_CONTEXT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Profile implements User {

    private static final Logger LOG = LoggerFactory.getLogger(Profile.class);

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

    @Nonnull
    private final String _name;
    @Nullable
    private String _fullName;
    @Nullable
    private String _email;

    public Profile(@Nonnull User user, @Nonnull BandanaManager bandanaManager, @Nonnull UserDetailsManager userDetailsManager, @Nonnull PersonalInformationManager personalInformationManager, @Nonnull ConfluenceIndexer confluenceIndexer, @Nonnull UserAccessor userAccessor) {
        this(user.getName(), user.getFullName(), user.getEmail(), bandanaManager, userDetailsManager, personalInformationManager, confluenceIndexer, userAccessor);
    }

    public Profile(@Nonnull String name, @Nullable String fullName, @Nullable String email, @Nonnull BandanaManager bandanaManager, @Nonnull UserDetailsManager userDetailsManager, @Nonnull PersonalInformationManager personalInformationManager, @Nonnull ConfluenceIndexer confluenceIndexer, @Nonnull UserAccessor userAccessor) {
        _userAccessor = userAccessor;
        _name = name;
        _fullName = fullName;
        _email = email;
        _bandanaManager = bandanaManager;
        _userDetailsManager = userDetailsManager;
        _personalInformationManager = personalInformationManager;
        _confluenceIndexer = confluenceIndexer;
    }

    public void reIndex() {
        _confluenceIndexer.index(getPersonalInformation());
    }

    public void setValue(@Nonnull ElementModel of, @Nullable String to) {
        try {
            final String id = of.getId();
            if (ElementModel.FULL_NAME_ELEMENT_ID.equals(id)) {
                _fullName = to;
                _userAccessor.saveUser(this);
            } else if (ElementModel.EMAIL_ELEMENT_ID.equals(id)) {
                _email = to;
                _userAccessor.saveUser(this);
            } else if (ElementModel.PERSONAL_INFORMATION_ELEMENT_ID.equals(id)) {
                setPersonalInformationBody(to);
            } else {
                setStandardValue(of, to);
            }
        } catch (final NullPointerException e) {
            if (("Unable to find user mapping for " + getName()).equals(e.getMessage())) {
                LOG.warn("Could not modify " + getName() + ". If this is caused by a rename of a user you can safely ignore this message.");
            } else {
                throw e;
            }
        }
    }

    @Nullable
    public String getValue(@Nonnull ElementModel of) {
        final String result;
        final String id = of.getId();
        if (ElementModel.FULL_NAME_ELEMENT_ID.equals(id)) {
            result = getFullName();
        } else if (ElementModel.EMAIL_ELEMENT_ID.equals(id)) {
            result = getEmail();
        } else if (ElementModel.PERSONAL_INFORMATION_ELEMENT_ID.equals(of.getId())) {
            result = getPersonalInformationBody();
        } else {
            result = getStandardValue(of);
        }
        return result;
    }

    @Nonnull
    public PersonalInformation getPersonalInformation() {
        return _personalInformationManager.getOrCreatePersonalInformation(this);
    }

    @Nullable
    public String getValueForSearchIndex(@Nonnull ElementModel of) {
        return getValue(of);
    }

    protected void setPersonalInformationBody(@Nullable String to) {
        _personalInformationManager.savePersonalInformation(this, to != null ? to : "", getFullName());
    }

    @Nullable
    protected String getPersonalInformationBody() {
        // noinspection deprecation
        final PersonalInformation information = _personalInformationManager.getPersonalInformation(this);
        final String body = information.getBodyAsString();
        return body == null || isEmpty(body.trim()) ? null : body;
    }

    protected void setStandardValue(@Nonnull ElementModel of, @Nullable String to) {
        if (isEmpty(to)) {
            _userDetailsManager.removeProperty(this, of.getId());
        } else {
            _userDetailsManager.setStringProperty(this, of.getId(), to);
        }
        //noinspection deprecation
        removeValueFromOldLocationIfNeeded(of);
    }

    @Nullable
    protected String getStandardValue(@Nonnull ElementModel of) {
        String result = _userDetailsManager.getStringProperty(this, of.getId());
        if (result == null) {
            //noinspection deprecation
            result = getValueFromOldLocation(of);
        }
        return result;
    }

    @Deprecated
    protected void removeValueFromOldLocationIfNeeded(@Nonnull ElementModel of) {
        _bandanaManager.removeValue(GLOBAL_CONTEXT, keyFor(of));
    }

    @Nullable
    @Deprecated
    protected String getValueFromOldLocation(@Nonnull ElementModel of) {
        final Object value = _bandanaManager.getValue(GLOBAL_CONTEXT, keyFor(of));
        final String result = value != null ? value.toString() : null;
        if (result != null) {
            setStandardValue(of, result);
            //noinspection deprecation
            removeValueFromOldLocationIfNeeded(of);
        }
        return result;
    }

    @Nonnull
    protected String keyFor(@Nonnull ElementModel elementModel) {
        return Profile.class.getPackage().getName() + "." + elementModel.getId() + "." + getName();
    }

    @Nullable
    @Override
    public String getFullName() {
        return _fullName;
    }

    @Nonnull
    @Override
    public String getName() {
        return _name;
    }

    @Nullable
    @Override
    public String getEmail() {
        return _email;
    }

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof Profile)) {
            result = false;
        } else {
            result = getName().equals(((Principal) o).getName());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getName());
        final String fullName = getFullName();
        final String email = getEmail();
        if (!isEmpty(fullName) || !isEmpty(email)) {
            sb.append('(');
            if (isEmpty(email)) {
                if (!isEmpty(fullName)) {
                    sb.append(fullName);
                }
            } else {
                if (isEmpty(fullName)) {
                    sb.append(email);
                } else {
                    sb.append(fullName).append(" <").append(email).append('>');
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

}
