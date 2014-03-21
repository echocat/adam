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

package org.echocat.adam.template;

import javax.xml.bind.annotation.XmlTransient;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@XmlTransient
public abstract class TemplateSupport implements Template {

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof Template)) {
            result = false;
        } else {
            result = getFormat().equals(((Template)o).getFormat()) && getSource().equals(((Template)o).getSource());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getFormat().hashCode() * getSource().hashCode();
    }


    @Override
    public String toString() {
        final String source = getSource();
        final String trimmedSource = source.replaceAll("\\s+", " ").trim();
        final String shorterSource = abbreviate(trimmedSource, 50);
        return "template{" + getFormat() + ":" + shorterSource + "}";
    }
}
