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

import javax.annotation.Nonnull;

public interface Template {

    @Nonnull
    public String getSource();

    @Nonnull
    public TemplateFormat getFormat();

    public static class Simple implements Template {

        @Nonnull
        public static Template simpleTemplateFor(@Nonnull String elementId) {
            return new Simple(elementId);
        }

        @Nonnull
        private final String _elementId;

        public Simple(@Nonnull String elementId) {
            _elementId = elementId;
        }

        @Nonnull
        @Override
        public String getSource() {
            return "$!{" + _elementId + "}";
        }

        @Nonnull
        @Override
        public TemplateFormat getFormat() {
            return TemplateFormat.velocity;
        }

    }

}
