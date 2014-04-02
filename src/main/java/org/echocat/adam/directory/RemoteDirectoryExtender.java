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
import javassist.*;
import org.echocat.jomon.runtime.util.SerialGenerator;
import org.echocat.jomon.runtime.util.SimpleLongSerialGenerator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteDirectoryExtender {

    protected static final CtClass[] NO_CLASSES = new CtClass[0];
    protected static final String REFERENCE_FIELD_NAME = "_reference";

    @Nonnull
    private final Map<String, Class<? extends RemoteDirectory>> _inputToExtendedClass = new HashMap<>();
    @Nonnull
    private final SerialGenerator<Long> _serialGenerator = new SimpleLongSerialGenerator().withInitialValue(1);
    @Nonnull
    private final ClassPool _parentClassPool = ClassPool.getDefault();
    @Nonnull
    private final RemoteDirectorySpringInitiatorFacade _facade;

    public RemoteDirectoryExtender(@Nonnull DirectoryHelper directoryHelper) throws Exception {
        _facade = new RemoteDirectorySpringInitiatorFacade(directoryHelper);
    }

    @Nonnull
    public <T extends RemoteDirectory> T createInstance(@Nonnull String originalClassName, @Nonnull Class<T> baseClass, @Nonnull ClassLoader classLoader) throws Exception {
        final Class<? extends T> newDirectoryType = extend(originalClassName, baseClass, classLoader);
        // noinspection unchecked
        return _facade.createInstance((Class<? extends RemoteDirectory>) classLoader.loadClass(originalClassName), newDirectoryType);
    }

    @Nonnull
    public <T extends RemoteDirectory> Class<? extends T> extend(@Nonnull String originalClassName, @Nonnull Class<T> baseClass, @Nonnull ClassLoader classLoader) throws Exception {
        synchronized (_inputToExtendedClass) {
            final Class<? extends T> result;
            if (_inputToExtendedClass.containsKey(originalClassName)) {
                //noinspection unchecked
                result = (Class<? extends T>) _inputToExtendedClass.get(originalClassName);
            } else {
                try {
                    result = extendInternal(originalClassName, baseClass, classLoader);
                    _inputToExtendedClass.put(originalClassName, result);
                } catch (final IllegalArgumentException e) {
                    _inputToExtendedClass.put(originalClassName, null);
                    throw e;
                }
            }
            if (result == null) {
                throw new IllegalArgumentException("Could not extend " + originalClassName + ".");
            }
            return result;
        }
    }

    @Nonnull
    protected  <T extends RemoteDirectory> Class<? extends T> extendInternal(@Nonnull String originalClassName, @Nonnull Class<T> baseClass, @Nonnull ClassLoader classLoader) throws Exception {
        final ClassPool pool = new ClassPool(_parentClassPool);
        pool.insertClassPath(new LoaderClassPath(classLoader));
        pool.insertClassPath(new LoaderClassPath(baseClass.getClassLoader()));
        final CtClass superClass = pool.get(originalClassName);
        final CtClass newClass = pool.makeClass(originalClassName + "$$Extension$$" + _serialGenerator.next(), superClass);
        createAndAddFields(newClass, pool);
        createAndAddConstructors(superClass, newClass, pool);
        createAndAddMethods(newClass, pool);
        final ClassLoader newClassLoader = new ClassLoaderImpl(newClass, classLoader);
        //noinspection unchecked
        final Class<? extends T> result = (Class<? extends T>) newClassLoader.loadClass(newClass.getName());
        if (!baseClass.isAssignableFrom(result)) {
            throw new IllegalArgumentException("The given original class " + originalClassName + " is not of base type " + baseClass + ".");
        }
        return result;
    }

    protected void createAndAddFields(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        createAndAddReferenceField(newClass, pool);
    }

    protected void createAndAddReferenceField(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        final CtField field = createReferenceField(newClass, pool);
        newClass.addField(field);
    }

    @Nonnull
    protected CtField createReferenceField(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        return new CtField(pool.get(DirectoryHelper.class.getName()), REFERENCE_FIELD_NAME, newClass);
    }

    protected void createAndAddConstructors(@Nonnull CtClass superClass, @Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        removeAllConstructors(newClass);
        for (final CtConstructor superConstructor : superClass.getDeclaredConstructors()) {
            createAndAddConstructor(superConstructor, newClass, pool);
        }
    }

    protected void removeAllConstructors(@Nonnull CtClass newClass) throws Exception {
        for (final CtConstructor constructor : newClass.getConstructors()) {
            newClass.removeConstructor(constructor);
        }
    }

    protected void createAndAddConstructor(@Nonnull CtConstructor superConstructor, @Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        newClass.addConstructor(createConstructor(superConstructor, newClass, pool));
    }

    @Nonnull
    protected CtConstructor createConstructor(@Nonnull CtConstructor superConstructor, @Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        final CtConstructor constructor = CtNewConstructor.make(superConstructor.getParameterTypes(), superConstructor.getExceptionTypes(), newClass);
        constructor.addParameter(pool.get(DirectoryHelper.class.getName()));
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("super(");
        final int numberOfParameters = superConstructor.getParameterTypes().length;
        for (int i = 0; i < numberOfParameters; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('$').append(i + 1);
        }
        sb.append(");");
        sb.append(REFERENCE_FIELD_NAME).append(" = $").append(numberOfParameters + 1).append(';');
        sb.append('}');
        constructor.setBody(sb.toString());
        return constructor;
    }

    protected void createAndAddMethods(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        createAndAddGetCustomUserAttributeMappersMethod(newClass, pool);
    }

    protected void createAndAddGetCustomUserAttributeMappersMethod(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        final CtMethod ctMethod = createGetCustomUserAttributeMappersMethod(newClass, pool);
        newClass.addMethod(ctMethod);
    }

    @Nonnull
    protected CtMethod createGetCustomUserAttributeMappersMethod(@Nonnull CtClass newClass, @Nonnull ClassPool pool) throws Exception {
        final CtClass listClass = pool.get(List.class.getName());
        final String body = createGetCustomUserAttributeMappersMethodBody();
        try {
            return CtNewMethod.make(listClass, "getCustomUserAttributeMappers", NO_CLASSES, NO_CLASSES, body, newClass);
        } catch (final CannotCompileException e) {
            throw new CannotCompileException("Cannot compile body:\n" + body, e);
        }
    }

    protected String createGetCustomUserAttributeMappersMethodBody() {
        return "{"
                + "return " + REFERENCE_FIELD_NAME + ".extendAttributeMappers(super.getCustomUserAttributeMappers($$));"
                + "}";
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
