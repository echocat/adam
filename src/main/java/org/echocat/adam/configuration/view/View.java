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

package org.echocat.adam.configuration.view;

import org.echocat.adam.configuration.IdEnabled;
import org.echocat.adam.configuration.access.view.ViewAccess;
import org.echocat.adam.configuration.localization.Localized;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import static org.echocat.adam.configuration.ConfigurationContants.SCHEMA_NAMESPACE;
import static org.echocat.adam.view.View.DEFAULT_ID;

@XmlType(name = "view", namespace = SCHEMA_NAMESPACE)
public class View extends Localized {

    public View() {
        setId(DEFAULT_ID);
    }

    @Nullable
    private List<Element> _elements;
    @Nullable
    private ViewAccess _access;

    @Nullable
    @XmlElement(name = "element", namespace = SCHEMA_NAMESPACE)
    public List<Element> getElements() {
        return _elements;
    }

    public void setElements(@Nullable List<Element> elements) {
        _elements = elements;
    }

    @Nullable
    @XmlElement(name = "access", namespace = SCHEMA_NAMESPACE)
    public ViewAccess getAccess() {
        return _access;
    }

    public void setAccess(@Nullable ViewAccess access) {
        _access = access;
    }

    @XmlType(name = "viewElement", namespace = SCHEMA_NAMESPACE)
    public static class Element extends IdEnabled {}

}
