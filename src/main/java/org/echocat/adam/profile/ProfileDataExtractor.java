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

import com.atlassian.bonnie.Searchable;
import com.atlassian.bonnie.search.Extractor;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.PersonalInformation;
import com.atlassian.user.EntityException;
import com.atlassian.user.GroupManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.echocat.adam.profile.element.ElementModel;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

import static org.apache.lucene.document.Field.Store.YES;
import static org.echocat.adam.profile.element.ElementModel.PERSONAL_INFORMATION_ELEMENT_ID;

public class ProfileDataExtractor implements Extractor {

    @Nonnull
    private final ProfileProvider _profileProvider;
    @Nonnull
    private final GroupProvider _groupProvider;
    @Nonnull
    private final GroupManager _groupManager;

    @Autowired
    public ProfileDataExtractor(@Nonnull ProfileProvider profileProvider, @Nonnull GroupProvider groupProvider, @Nonnull GroupManager groupManager) {
        _profileProvider = profileProvider;
        _groupProvider = groupProvider;
        _groupManager = groupManager;
    }

    @Override
    public void addFields(@Nonnull Document document, @Nonnull StringBuffer defaultSearchable, @Nonnull Searchable searchable) {
        if ((searchable instanceof PersonalInformation)) {
            try {
                addFields(document, defaultSearchable, (PersonalInformation) searchable);
            } catch (final RuntimeException e) {
                if (!e.getClass().getName().equals("org.springframework.osgi.service.importer.ServiceProxyDestroyedException")) {
                    throw e;
                }
            }
        }
    }

    public void addFields(@Nonnull Document document, @Nonnull StringBuffer defaultSearchable, @Nonnull PersonalInformation personalInformation) {
        final ConfluenceUser user = personalInformation.getUser();
        final Profile profile = _profileProvider.provideFor(user);
        for (final Group group : _groupProvider) {
            for (final ElementModel elementModel : group) {
                final String value = profile.getValueForSearchIndex(elementModel);
                if (value != null) {
                    if (canIndex(elementModel)) {
                        defaultSearchable.append(value);
                        defaultSearchable.append("\n");
                        document.add(new TextField("element." + elementModel.getId(), value, YES));
                    }
                }
            }
        }
        try {
            for (final com.atlassian.user.Group group : _groupManager.getGroups(user)) {
                document.add(new StringField("group", group.getName(), YES));
            }
        } catch (final EntityException e) {
            throw new RuntimeException("Could not get groups of " + user + ".", e);
        }
    }

    protected boolean canIndex(@Nonnull ElementModel elementModel) {
        return elementModel.isSearchable() && !PERSONAL_INFORMATION_ELEMENT_ID.equals(elementModel.getId());
    }
}
