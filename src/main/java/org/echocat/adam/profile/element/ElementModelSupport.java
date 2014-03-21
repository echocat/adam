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

package org.echocat.adam.profile.element;

import org.echocat.adam.localization.LocalizedSupport;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public abstract class ElementModelSupport extends LocalizedSupport implements ElementModel {

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof ElementModel)) {
            result = false;
        } else {
            // noinspection OverlyStrongTypeCast
            result = getId().equals(((ElementModel)o).getId());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("element:").append(getId()).append('{');
        sb.append("type:").append(getType());
        if (isStandard()) {
            sb.append(", standard");
        }
        if (isSearchable()) {
            sb.append(", searchable");
        }
        sb.append('}');
        return sb.toString();
    }

}

