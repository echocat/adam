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

package org.echocat.adam.configuration.access.viewedit;

import org.echocat.adam.configuration.access.view.Anonymous;
import org.echocat.adam.configuration.access.view.ViewAccessSupport;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;

@XmlType(name = "viewEditAccess", namespace = SCHEMA_NAMESPACE)
public class ViewEditAccess extends ViewAccessSupport<Default, Anonymous, Owner, Administrator, Group> {

    @Override
    @XmlElement(name = "default", namespace = SCHEMA_NAMESPACE, type = Default.class)
    public void setDefault(@Nullable Default all) {
        super.setDefault(all);
    }

    @Override
    @XmlElement(name = "anonymous", namespace = SCHEMA_NAMESPACE, type = Anonymous.class)
    public void setAnonymous(@Nullable Anonymous anonymous) {
        super.setAnonymous(anonymous);
    }

    @Override
    @XmlElement(name = "owner", namespace = SCHEMA_NAMESPACE, type = Owner.class)
    public void setOwner(@Nullable Owner owner) {
        super.setOwner(owner);
    }

    @Override
    @XmlElement(name = "administrator", namespace = SCHEMA_NAMESPACE, type = Administrator.class)
    public void setAdministrator(@Nullable Administrator administrator) {
        super.setAdministrator(administrator);
    }

    @Override
    @XmlElement(name = "group", namespace = SCHEMA_NAMESPACE, type = Group.class)
    public void setGroups(@Nullable List<Group> groups) {
        super.setGroups(groups);
    }

}
