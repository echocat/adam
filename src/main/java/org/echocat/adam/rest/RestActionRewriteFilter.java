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

package org.echocat.adam.rest;

import com.atlassian.core.filters.AbstractHttpFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.echocat.jomon.runtime.CollectionUtils.asImmutableSet;

public class RestActionRewriteFilter extends AbstractHttpFilter {

    protected static final Pattern EXTRACT_ACTION_AND_ARGUMENTS = Pattern.compile("(?:|/[^/]+)/rest/mobile/[^/]+(/[^/]+)(.*)");

    public static final Set<String> OVERWRITTEN_ACTIONS = asImmutableSet(
        "/profile"
    );

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        final String requestURI = request.getRequestURI();
        final Matcher matcher = EXTRACT_ACTION_AND_ARGUMENTS.matcher(requestURI);
        if (matcher.matches()) {
            final String action = matcher.group(1);
            if (OVERWRITTEN_ACTIONS.contains(action)) {
                final String target = request.getContextPath() + "/rest/adam/latest" + action + matcher.group(2);
                response.sendRedirect(target);
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

}
