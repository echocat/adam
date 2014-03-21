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

package org.echocat.adam.configuration.report;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlType(name = "reportFilter", namespace = SCHEMA_NAMESPACE)
public class Filter {

    @Nullable
    private List<Group> _includingGroups;
    @Nullable
    private List<Group> _excludingGroups;

    @Nullable
    @XmlElement(name = "includingGroup", namespace = SCHEMA_NAMESPACE)
    public List<Group> getIncludingGroups() {
        return _includingGroups;
    }

    public void setIncludingGroups(@Nullable List<Group> includingGroups) {
        _includingGroups = includingGroups;
    }

    @Nullable
    @XmlElement(name = "excludingGroup", namespace = SCHEMA_NAMESPACE)
    public List<Group> getExcludingGroups() {
        return _excludingGroups;
    }

    public void setExcludingGroups(@Nullable List<Group> excludingGroups) {
        _excludingGroups = excludingGroups;
    }

}
