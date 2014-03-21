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

import javassist.*;
import org.echocat.jomon.runtime.util.SerialGenerator;
import org.echocat.jomon.runtime.util.SimpleLongSerialGenerator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class RemoteDirectoryExtender {

    protected static final String REFERENCE_FIELD_NAME = "_reference";
    public static final CtClass[] NO_CLASSES = new CtClass[0];

    @Nonnull
    private final SerialGenerator<Long> _serialGenerator = new SimpleLongSerialGenerator().withInitialValue(1);
    @Nonnull
    private final ClassPool _parentClassPool = ClassPool.getDefault();

    @Nonnull
    public <T> Class<? extends T> extend(@Nonnull Class<T> superClass) throws Exception {
        return extend(superClass.getName(), superClass, superClass.getClassLoader());
    }

    @Nonnull
    public Class<?> extend(@Nonnull String originalClassName, @Nonnull ClassLoader classLoader) throws Exception {
        return extend(originalClassName, Object.class, classLoader);
    }

    @Nonnull
    public <T> Class<? extends T> extend(@Nonnull String originalClassName, @Nonnull Class<T> baseClass, @Nonnull ClassLoader classLoader) throws Exception {
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
