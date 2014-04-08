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

package org.echocat.adam.profile.mobile;

import com.atlassian.core.filters.AbstractHttpFilter;
import com.atlassian.plugin.webresource.WebResourceManager;
import org.echocat.jomon.runtime.CollectionUtils;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static com.atlassian.plugin.webresource.UrlMode.AUTO;

@SuppressWarnings("deprecation")
public class HackJsFilter extends AbstractHttpFilter {

    @Nonnull
    private final WebResourceManager _webResourceManager;

    public HackJsFilter(@Nonnull WebResourceManager webResourceManager) {
        _webResourceManager = webResourceManager;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(request, response);
        final PrintWriter writer = response.getWriter();
        _webResourceManager.includeResources(CollectionUtils.asList("org.echocat.adam:mobileProfile"), writer, AUTO);
    }

}
