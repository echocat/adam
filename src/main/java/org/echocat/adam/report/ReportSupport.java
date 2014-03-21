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

package org.echocat.adam.report;

import org.echocat.adam.localization.LocalizedSupport;

public abstract class ReportSupport extends LocalizedSupport implements Report {

    @Override
    public boolean equals(Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (!(o instanceof Report)) {
            result = false;
        } else {
            // noinspection OverlyStrongTypeCast
            result = getId().equals(((Report) o).getId());
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
        sb.append("report:").append(getId()).append('{');
        sb.append("columns:");
        boolean first = true;
        for (final Column column : this) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(column.getId());
        }
        sb.append('}');
        return sb.toString();
    }


}
