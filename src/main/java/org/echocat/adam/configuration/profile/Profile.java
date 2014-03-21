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

package org.echocat.adam.configuration.profile;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlType(name = "profile", namespace = SCHEMA_NAMESPACE)
public class Profile {

    @Nullable
    private List<Group> _groups;

    @Nullable
    @XmlElement(name = "group", namespace = SCHEMA_NAMESPACE)
    public List<Group> getGroups() {
        return _groups;
    }

    public void setGroups(@Nullable List<Group> groups) {
        _groups = groups;
    }

}
