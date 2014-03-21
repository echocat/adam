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

package org.echocat.adam.directory;

import com.atlassian.confluence.setup.ConfluenceListableBeanFactory;
import com.atlassian.confluence.setup.ConfluenceXmlWebApplicationContext;
import com.atlassian.confluence.user.crowd.ConfluenceSpringContextInstanceFactory;
import com.atlassian.crowd.directory.RemoteDirectory;
import com.atlassian.crowd.directory.loader.LDAPDirectoryInstanceLoader;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.util.InstanceFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.spring.container.ContainerManager.getComponent;

public class ExtendingLDAPDirectoryInstanceLoader implements LDAPDirectoryInstanceLoader, DisposableBean {

    @Nonnull
    private final RemoteDirectoryExtender _extender = new RemoteDirectoryExtender();
    @Nonnull
    private final Map<String, Class<? extends RemoteDirectory>> _inputToExtendedClass = new HashMap<>();
    @Nonnull
    private final InstanceFactory _instanceFactory;
    @Nonnull
    private final ConfluenceListableBeanFactory _beanFactory;

    public ExtendingLDAPDirectoryInstanceLoader(@Nonnull DirectoryHelper directoryHelper) {
        this((InstanceFactory) getComponent("instanceFactory"), directoryHelper);
    }

    public ExtendingLDAPDirectoryInstanceLoader(@Nonnull InstanceFactory instanceFactory, @Nonnull DirectoryHelper directoryHelper) {
        _instanceFactory = instanceFactory;
        _beanFactory = getBeanFactoryOf(instanceFactory);
        _beanFactory.registerSingleton("directoryHelper", directoryHelper);
    }

    @Nonnull
    protected ConfluenceListableBeanFactory getBeanFactoryOf(@Nonnull InstanceFactory instanceFactory) {
        try {
            final Field field = ConfluenceSpringContextInstanceFactory.class.getDeclaredField("applicationContext");
            field.setAccessible(true);
            final ConfluenceXmlWebApplicationContext applicationContext = (ConfluenceXmlWebApplicationContext) field.get(instanceFactory);
            return ((ConfluenceListableBeanFactory)applicationContext.getAutowireCapableBeanFactory());
        } catch (final Exception e) {
            throw new RuntimeException("Could not hook into applicationContext of " + instanceFactory + ".", e);
        }
    }

    @Override
    @Nonnull
    public RemoteDirectory getRawDirectory(@Nullable Long directoryId, @Nonnull String className, @Nullable Map<String, String> attributes) {
        return newRemoteDirectory(directoryId, className, attributes);
    }

    @Override
    @Nonnull
    public RemoteDirectory getDirectory(@Nonnull Directory directory) {
        return getRawDirectory(directory.getId(), directory.getImplementationClass(), directory.getAttributes());
    }

    @Nonnull
    protected RemoteDirectory newRemoteDirectory(@Nullable Long directoryId, @Nonnull String className, @Nullable Map<String, String> attributes) {
        try {
            final Class<? extends RemoteDirectory> clazz = getRemoteDirectoryFor(className, getClass().getClassLoader());
            final RemoteDirectory remoteDirectory = (RemoteDirectory) _instanceFactory.getInstance(clazz.getName(), clazz.getClassLoader());
            if (directoryId != null) {
                remoteDirectory.setDirectoryId(directoryId);
            }
            remoteDirectory.setAttributes(attributes);
            return remoteDirectory;
        } catch (final Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canLoad(String className) {
        try {
            return findRemoteDirectoryFor(className, getClass().getClassLoader()) != null;
        } catch (final Exception e) {
            throw new RuntimeException("Could not check if " + className + " is loadable.", e);
        }
    }

    @Nullable
    protected Class<? extends RemoteDirectory> findRemoteDirectoryFor(@Nonnull String className, @Nonnull ClassLoader classLoader) throws Exception {
        return getRemoteDirectoryFor(className, classLoader, true);
    }

    @Nonnull
    protected Class<? extends RemoteDirectory> getRemoteDirectoryFor(@Nonnull String className, @Nonnull ClassLoader classLoader) throws Exception {
        // noinspection ConstantConditions
        return getRemoteDirectoryFor(className, classLoader, false);
    }

    @SuppressWarnings("DuplicateThrows")
    @Nullable
    protected Class<? extends RemoteDirectory> getRemoteDirectoryFor(@Nonnull String className, @Nonnull ClassLoader classLoader, boolean canBeNull) throws Exception, IllegalArgumentException {
        synchronized (_inputToExtendedClass) {
            Class<? extends RemoteDirectory> result;
            if (_inputToExtendedClass.containsKey(className)) {
                result = _inputToExtendedClass.get(className);
            } else {
                try {
                    result = _extender.extend(className, RemoteDirectory.class, classLoader);
                    _inputToExtendedClass.put(className, result);
                } catch (final IllegalArgumentException e) {
                    _inputToExtendedClass.put(className, null);
                    if (canBeNull) {
                        result = null;
                    } else {
                        throw e;
                    }
                }
            }
            if (result == null && !canBeNull) {
                throw new IllegalArgumentException("Could not extend " + className + ".");
            }
            return result;
        }
    }

    @Override
    public void destroy() throws Exception {
        _beanFactory.destroySingleton("directoryHelper");
    }
}