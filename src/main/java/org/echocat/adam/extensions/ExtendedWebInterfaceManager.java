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

package org.echocat.adam.extensions;

import com.atlassian.confluence.plugin.descriptor.web.ConfluenceWebInterfaceManager;
import com.atlassian.confluence.plugin.descriptor.web.WebInterfaceContext;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.descriptors.DefaultWebPanelModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebPanelModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebSectionModuleDescriptor;
import com.atlassian.plugin.web.model.ResourceTemplateWebPanel;
import com.atlassian.plugin.web.model.WebPanel;
import org.echocat.jomon.runtime.reflection.ClassUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.echocat.jomon.runtime.CollectionUtils.asList;

public class ExtendedWebInterfaceManager implements WebInterfaceManager, ApplicationContextAware, DisposableBean {

    @SuppressWarnings("unchecked")
    private static final Class<? extends WebPanel> EXCEPTION_HANDLING_WEB_PANEL_CLASS = (Class<? extends WebPanel>) ClassUtils.findClass(ConfluenceWebInterfaceManager.class.getName() + "$ExceptionHandlingWebPanel");
    private static final Field EXCEPTION_HANDLING_WEB_PANEL_DELEGATE_FIELD = ClassUtils.getFieldOf(EXCEPTION_HANDLING_WEB_PANEL_CLASS, WebPanel.class, "delegate", false);
    @SuppressWarnings("unchecked")
    private static final Class<? extends WebPanel> CONTEXT_AWARE_WEB_PANEL_CLASS = (Class<? extends WebPanel>) ClassUtils.findClass(DefaultWebPanelModuleDescriptor.class.getName() + "$ContextAwareWebPanel");
    private static final Field CONTEXT_AWARE_WEB_PANEL_DELEGATE_FIELD = ClassUtils.getFieldOf(CONTEXT_AWARE_WEB_PANEL_CLASS, WebPanel.class, "delegate", false);
    private static final Field RESOURCE_FILENAME_FIELD = ClassUtils.getFieldOf(ResourceTemplateWebPanel.class, String.class, "resourceFilename", false);

    private static ExtendedWebInterfaceManager c_instance;

     @Nonnull
    public static ExtendedWebInterfaceManager extendedWebInterfaceManager() {
         final ExtendedWebInterfaceManager result = c_instance;
         if (result == null) {
             throw new IllegalStateException("There is no instance initialized.");
         }
         return result;
    }

    private ApplicationContext _applicationContext;

    public ExtendedWebInterfaceManager() {
        c_instance = this;
    }

    @Override
    public boolean hasSectionsForLocation(String s) {return getOriginal().hasSectionsForLocation(s);}

    @Override
    public List<WebSectionModuleDescriptor> getSections(String s) {return getOriginal().getSections(s);}

    @Override
    public List<WebSectionModuleDescriptor> getDisplayableSections(String s, Map<String, Object> stringObjectMap) {
        return getOriginal().getDisplayableSections(s, stringObjectMap);
    }

    public List<WebSectionModuleDescriptor> getDisplayableSections(String s, WebInterfaceContext context) {
        return getDisplayableSections(s, context.toMap());
    }

    @Override
    public List<WebItemModuleDescriptor> getItems(String s) {return getOriginal().getItems(s);}

    @Override
    public List<WebItemModuleDescriptor> getDisplayableItems(String s, Map<String, Object> stringObjectMap) {
        return getOriginal().getDisplayableItems(s, stringObjectMap);
    }

    public List<WebItemModuleDescriptor> getDisplayableItems(String key, WebInterfaceContext context) {
        return getDisplayableItems(key, context.toMap());
    }

    @Override
    public List<WebPanel> getWebPanels(String s) {return getOriginal().getWebPanels(s);}

    @Override
    public List<WebPanelModuleDescriptor> getWebPanelDescriptors(String s) {return getOriginal().getWebPanelDescriptors(s);}

    @Override
    public List<WebPanelModuleDescriptor> getDisplayableWebPanelDescriptors(String s, Map<String, Object> stringObjectMap) {
        return getOriginal().getDisplayableWebPanelDescriptors(s, stringObjectMap);
    }

    public List<WebPanelModuleDescriptor> getDisplayableWebPanelDescriptors(String s, WebInterfaceContext context) {
        return getDisplayableWebPanelDescriptors(s, context.toMap());
    }

    @Override
    public void refresh() {getOriginal().refresh();}

    @Override
    public WebFragmentHelper getWebFragmentHelper() {return getOriginal().getWebFragmentHelper();}

    public List<WebPanel> getDisplayableWebPanels(String location, WebInterfaceContext context) {
        return getDisplayableWebPanels(location, context.toMap());
    }

    @Override
    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context) {
        final List<WebPanel> result = asList(getOriginal().getDisplayableWebPanels(location, context));
        if ("atl.confluence.userprofile.info".equals(location)) {
            final Iterator<WebPanel> i = result.iterator();
            while (i.hasNext()) {
                WebPanel panel = i.next();
                while (panel != null && !(panel instanceof ResourceTemplateWebPanel)) {
                    panel = findDelegateIfPossibleOf(panel);
                }
                if (panel instanceof ResourceTemplateWebPanel) {
                    final ResourceTemplateWebPanel resourceTemplateWebPanel = (ResourceTemplateWebPanel) panel;
                    final String resourceFileName = getResourceFileNameOf(resourceTemplateWebPanel);
                    if ("/users/viewmyprofile-info.vm".equals(resourceFileName)) {
                        i.remove();
                    }
                }
            }
        }
        return result;
    }

    @Nullable
    protected WebPanel findDelegateIfPossibleOf(@Nonnull WebPanel panel) {
        final WebPanel result;
        if (CONTEXT_AWARE_WEB_PANEL_CLASS.isInstance(panel)) {
            try {
                result = (WebPanel) CONTEXT_AWARE_WEB_PANEL_DELEGATE_FIELD.get(panel);
            } catch (final IllegalAccessException e) {
                throw new RuntimeException("Could not access " + CONTEXT_AWARE_WEB_PANEL_DELEGATE_FIELD + " of " + panel + ".", e);
            }
        } else if (EXCEPTION_HANDLING_WEB_PANEL_CLASS.isInstance(panel)) {
            try {
                result = (WebPanel) EXCEPTION_HANDLING_WEB_PANEL_DELEGATE_FIELD.get(panel);
            } catch (final IllegalAccessException e) {
                throw new RuntimeException("Could not access " + EXCEPTION_HANDLING_WEB_PANEL_DELEGATE_FIELD + " of " + panel + ".", e);
            }
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    protected String getResourceFileNameOf(@Nonnull ResourceTemplateWebPanel panel) {
        try {
            return (String) RESOURCE_FILENAME_FIELD.get(panel);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Could not access " + RESOURCE_FILENAME_FIELD + " of " + panel + ".", e);
        }
    }

    @Nonnull
    protected WebInterfaceManager getOriginal() {
        final ApplicationContext applicationContext = applicationContext();
        //noinspection unchecked
        final Map<String, WebInterfaceManager> beans = applicationContext.getBeansOfType(WebInterfaceManager.class, false, false);
        WebInterfaceManager result = null;
        for (final Entry<String, WebInterfaceManager> beanNameAndType : beans.entrySet()) {
            final WebInterfaceManager candidate = beanNameAndType.getValue();
            if (!getClass().isInstance(candidate)) {
                result = candidate;
                break;
            }
        }
        if (result == null) {
            throw new IllegalStateException("Could not find any suitable candidate for: " + WebInterfaceManager.class.getName());
        }
        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        _applicationContext = applicationContext;
    }

    @Nonnull
    protected ApplicationContext applicationContext() {
        final ApplicationContext result = _applicationContext;
        if (result == null) {
            throw new IllegalStateException("There was no applicationContext set. Not initialized via Spring?");
        }
        return result;
    }

    @Override
    public void destroy() throws Exception {
        c_instance = null;
    }
}
