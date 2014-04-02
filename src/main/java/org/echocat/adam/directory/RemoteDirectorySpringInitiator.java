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
import javassist.CannotCompileException;
import javassist.CtClass;
import org.echocat.jomon.runtime.util.SerialGenerator;
import org.echocat.jomon.runtime.util.SimpleLongSerialGenerator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.isStatic;

public class RemoteDirectorySpringInitiator {

    protected static final Object[] NO_ARGUMENTS = new Object[0];
    protected static final Method CREATE_BEAN_INSTANCE_METHOD = getCreateBeanInstanceMethodOf(AbstractAutowireCapableBeanFactory.class);

    @Nonnull
    private static Method getCreateBeanInstanceMethodOf(@Nonnull Class<?> clazz) {
        final Method result;
        try {
            result = clazz.getDeclaredMethod("createBeanInstance", String.class, RootBeanDefinition.class, Object[].class);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Could not find " +  clazz.getName() + ".createBeanInstance() method.", e);
        }
        if (isStatic(result.getModifiers())) {
            throw new IllegalStateException("Method is static: " + result);
        }
        result.setAccessible(true);
        return result;
    }

    @Nonnull
    private final SerialGenerator<Long> _serialGenerator = new SimpleLongSerialGenerator().withInitialValue(1);
    @Nonnull
    private final ConfigurableListableBeanFactory _beanFactory;
    @Nonnull
    private final Object _directoryHelper;

    public RemoteDirectorySpringInitiator(@Nonnull Object directoryHelper, @Nonnull ApplicationContext applicationContext) throws Exception {
        _directoryHelper = directoryHelper;
        _beanFactory = getBeanFactoryFor(applicationContext);
    }

    @Nonnull
    protected ConfigurableListableBeanFactory getBeanFactoryFor(@Nonnull ApplicationContext applicationContext) throws Exception {
        if (!(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new IllegalStateException("Could only handle application context of type " + ConfigurableApplicationContext.class.getName() + " but got: " + applicationContext);
        }
        return ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    @Nonnull
    public <T extends RemoteDirectory> T createInstance(@Nonnull Class<? extends RemoteDirectory> originalClass, @Nonnull Class<T> newClass) throws Exception {
        final BeanDefinition originalBeanDefinition = getBeanDefinitionFor(originalClass);
        final BeanDefinition newBeanDefinition = extend(originalBeanDefinition, newClass);
        final BeanWrapper newDirectory = createBeanInstance("adam.generatedRemoteDirectory." + _serialGenerator.next(), newBeanDefinition);
        return newClass.cast(newDirectory.getWrappedInstance());
    }


    @Nonnull
    protected BeanDefinition getBeanDefinitionFor(@Nonnull Class<? extends RemoteDirectory> directoryType) throws Exception {
        final BeanDefinition result = findBeanDefinitionFor(directoryType);
        if (result == null) {
            throw new IllegalArgumentException("Could not find a bean definition for directory of type " + directoryType.getName() + ".");
        }
        return result;
    }

    @Nullable
    protected BeanDefinition findBeanDefinitionFor(@Nonnull Class<? extends RemoteDirectory> directoryType) throws Exception {
        final BeanDefinition result;
        final String[] candidateNames = _beanFactory.getBeanNamesForType(directoryType, true, true);
        if (candidateNames != null && candidateNames.length > 0) {
            result = _beanFactory.getBeanDefinition(candidateNames[0]);
        } else {
            result = null;
        }
        return result;
    }

    @Nonnull
    protected BeanDefinition extend(@Nonnull BeanDefinition original, @Nonnull Class<? extends RemoteDirectory> newDirectoryType) throws Exception {
        final RootBeanDefinition result = new RootBeanDefinition();
        result.overrideFrom(original);
        result.setBeanClass(newDirectoryType);
        final ConstructorArgumentValues constructor = result.getConstructorArgumentValues();
        if (!constructor.getGenericArgumentValues().isEmpty()) {
            constructor.addGenericArgumentValue(_directoryHelper);
        } else {
            final int newIndex = constructor.getIndexedArgumentValues().size();
            constructor.addIndexedArgumentValue(newIndex, _directoryHelper);
        }
        return result;
    }

    @Nonnull
    protected BeanWrapper createBeanInstance(@Nonnull String beanName, @Nonnull BeanDefinition beanDefinition) {
        if (!(beanDefinition instanceof RootBeanDefinition)) {
            throw new IllegalArgumentException("Requires a beanDefinition of type " + RootBeanDefinition.class.getName() + ".");
        }
        try {
            return (BeanWrapper) CREATE_BEAN_INSTANCE_METHOD.invoke(_beanFactory, beanName, beanDefinition, null);
        } catch (final Exception e) {
            throw new RuntimeException("Could not create instance.", e);
        }
    }

    protected class ClassLoaderImpl extends ClassLoader {

        public ClassLoaderImpl(@Nonnull CtClass clazz, @Nonnull ClassLoader parent) throws IOException, CannotCompileException {
            super(parent);
            register(clazz);
        }

        protected void register(@Nonnull CtClass clazz) throws IOException, CannotCompileException {
            final byte[] bytes = clazz.toBytecode();
            defineClass(clazz.getName(), bytes, 0, bytes.length);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            ClassLoader currentClassLoader = getParent();
            Class<?> result = null;
            while (result == null && currentClassLoader != null && !currentClassLoader.equals(currentClassLoader.getParent())) {
                try {
                    result = currentClassLoader.loadClass(name);
                } catch (final ClassNotFoundException ignored) {}
                currentClassLoader = currentClassLoader.getParent();
            }
            if (result == null) {
                throw new ClassNotFoundException();
            }
            return result;
        }
    }

}
