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

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.actions.EditMyProfileAction;
import org.echocat.adam.profile.element.ElementModel;

import javax.annotation.Nonnull;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;
import static org.echocat.adam.profile.ProfileModelProvider.profileModelProvider;
import static org.echocat.adam.profile.ProfileProvider.profileProvider;

@SuppressWarnings("deprecation")
public class ExtendedEditMyProfileAction extends EditMyProfileAction {

    private Map<String, String[]> _parameters;

    @Override
    public String doEdit() throws Exception {
        final ConfluenceUser user = getUser();
        final Profile profile = user != null ? profileProvider().provideFor(user) : null;
        final String result = super.doEdit();
        if ("success".equals(result)) {
            updateFields(profile);
            profile.reIndex();
        }
        return result;
    }

    private void updateFields(@Nonnull Profile profile) {
        for (final Group group : profileModelProvider().get()) {
            for (final ElementModel elementModel : group) {
                if (!elementModel.isStandard()) {
                    final String id = elementModel.getId();
                    final String[] plainValues = _parameters.get(id);
                    final String plainValue = plainValues != null && plainValues.length > 0 ? join(plainValues, ' ') : null;
                    if (plainValue != null && elementModel.getAccess().checkEdit(AuthenticatedUserThreadLocal.get(), profile).isEditAllowed()) {
                        profile.setValue(elementModel, plainValue);
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setParameters(Map parameters) {
        //noinspection unchecked
        _parameters = parameters;
        super.setParameters(parameters);
    }
}
