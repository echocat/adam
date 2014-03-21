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

package org.echocat.adam.configuration.access.view;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

@XmlTransient
public abstract class ViewAccessSupport<DF extends Default, AN extends Anonymous, OW extends Owner, AD extends Administrator, GR extends Group> {

    @Nullable
    private DF _default;
    @Nullable
    private AN _anonymous;
    @Nullable
    private OW _owner;
    @Nullable
    private AD _administrator;
    @Nullable
    private List<GR> _groups;

    @Nullable
    public DF getDefault() {
        return _default;
    }

    public void setDefault(@Nullable DF aDefault) {
        _default = aDefault;
    }

    @Nullable
    public AN getAnonymous() {
        return _anonymous;
    }

    public void setAnonymous(@Nullable AN anonymous) {
        _anonymous = anonymous;
    }

    @Nullable
    public OW getOwner() {
        return _owner;
    }

    public void setOwner(@Nullable OW owner) {
        _owner = owner;
    }

    @Nullable
    public AD getAdministrator() {
        return _administrator;
    }

    public void setAdministrator(@Nullable AD administrator) {
        _administrator = administrator;
    }

    @Nullable
    public List<GR> getGroups() {
        return _groups;
    }

    public void setGroups(@Nullable List<GR> groups) {
        _groups = groups;
    }

    public boolean hasAnyRule() {
        return _anonymous != null
            || _owner != null
            || _administrator != null
            || (_groups != null && !_groups.isEmpty());
    }

}
