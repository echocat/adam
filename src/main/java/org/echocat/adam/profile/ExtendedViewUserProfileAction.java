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

import com.atlassian.confluence.user.actions.ViewUserProfileAction;
import com.atlassian.plugin.web.WebInterfaceManager;

import static org.echocat.adam.extensions.ExtendedWebInterfaceManager.extendedWebInterfaceManager;

@SuppressWarnings("deprecation")
public class ExtendedViewUserProfileAction extends ViewUserProfileAction {

    @Override
    public String execute() {
        final String result = super.execute();
        return result;
    }

    @Override
    public WebInterfaceManager getWebInterfaceManager() {
        return extendedWebInterfaceManager();
    }

}
