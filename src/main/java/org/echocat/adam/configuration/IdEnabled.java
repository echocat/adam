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

package org.echocat.adam.configuration;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAttribute;

import static java.util.UUID.randomUUID;

public abstract class IdEnabled implements org.echocat.jomon.runtime.util.IdEnabled<String> {

    @Nonnull
    private String _id = randomUUID().toString();

    @Override
    @Nonnull
    @XmlAttribute(name = "id", required = true)
    public String getId() {
        return _id;
    }

    public void setId(@Nonnull String id) {
        _id = id;
    }

}
