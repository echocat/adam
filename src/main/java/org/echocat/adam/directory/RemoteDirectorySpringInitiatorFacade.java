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

import com.atlassian.crowd.directory.RemoteDirectory;
import com.atlassian.spring.container.ContainerContext;
import com.atlassian.spring.container.SpringContainerContext;
import org.echocat.jomon.runtime.reflection.ClassUtils;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static com.atlassian.modzdetector.IOUtils.toByteArray;
import static com.atlassian.spring.container.ContainerManager.getInstance;

public class RemoteDirectorySpringInitiatorFacade {

    @Nonnull
    private final ClassLoader _classLoader;
    @Nonnull
    private final Class<?> _targetClass;
    @Nonnull
    private final Method _targetMethod;
    @Nonnull
    private final Object _target;

    public RemoteDirectorySpringInitiatorFacade(@Nonnull DirectoryHelper directoryHelper) throws Exception {
        final Object systemApplicationContext = getSystemApplicationContext();
        final Class<?> systemApplicationContextType = systemApplicationContext.getClass().getClassLoader().loadClass(ApplicationContext.class.getName());
        _classLoader = new ClassLoaderImpl(systemApplicationContext.getClass().getClassLoader());
        _targetClass = _classLoader.loadClass(RemoteDirectorySpringInitiator.class.getName());
        _targetMethod = ClassUtils.getPublicMethodOf(_targetClass, RemoteDirectory.class, "createInstance", Class.class, Class.class);
        _target = _targetClass.getConstructor(Object.class, systemApplicationContextType).newInstance(directoryHelper, systemApplicationContext);
    }

    @Nonnull
    protected Object getSystemApplicationContext() throws Exception {
        final ContainerContext context = getInstance().getContainerContext();
        if (!(context instanceof SpringContainerContext)) {
            throw new IllegalStateException("Could only handle container context of type " + SpringContainerContext.class.getName() + " but got: " + context);
        }
        final Object applicationContext;
        try {
            final Method method = SpringContainerContext.class.getDeclaredMethod("getApplicationContext");
            method.setAccessible(true);
            applicationContext = method.invoke(context);
        } catch (final Exception e) {
            throw new RuntimeException("Could not access " + SpringContainerContext.class.getName() + ".getApplicationContext() method.", e);
        }
        return applicationContext;
    }

    @Nonnull
    public <T extends RemoteDirectory> T createInstance(@Nonnull Class<? extends RemoteDirectory> originalClass, @Nonnull Class<T> newClass) throws Exception {
        //noinspection unchecked
        return (T) _targetMethod.invoke(_target, originalClass, newClass);
    }

    protected static class ClassLoaderImpl extends ClassLoader {

        public ClassLoaderImpl(@Nonnull ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(@Nonnull String name) throws ClassNotFoundException {
            final Class<?> result;
            if (RemoteDirectorySpringInitiator.class.getName().equals(name)) {
                result = cloneClass(RemoteDirectorySpringInitiator.class);
            } else {
                result = findAtParents(name);
            }
            return result;
        }

        @Nonnull
        protected Class<?> cloneClass(@Nonnull Class<?> original) throws ClassNotFoundException {
            return cloneClass(original.getName(), original.getClassLoader());
        }

        @Nonnull
        protected Class<?> cloneClass(@Nonnull String name, @Nonnull ClassLoader classLoader) throws ClassNotFoundException {
            final String resourceName = name.replace('.', '/') + ".class";
            final byte[] bytes;
            try (final InputStream is = classLoader.getResourceAsStream(resourceName)) {
                if (is == null) {
                    throw new ClassNotFoundException(name);
                }
                bytes = toByteArray(is);
            } catch (final IOException e) {
                throw new LinkageError("Cannot find clone the class " + name + ".", e);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }

        @Nonnull
        protected Class<?> findAtParents(@Nonnull String name) throws ClassNotFoundException {
            ClassLoader currentClassLoader = getParent();
            Class<?> result = null;
            while (result == null && currentClassLoader != null && !currentClassLoader.equals(currentClassLoader.getParent())) {
                try {
                    result = currentClassLoader.loadClass(name);
                } catch (final ClassNotFoundException ignored) {}
                currentClassLoader = currentClassLoader.getParent();
            }
            if (result == null) {
                result = RemoteDirectorySpringInitiatorFacade.class.getClassLoader().loadClass(name);
            }
            return result;
        }

    }

}
