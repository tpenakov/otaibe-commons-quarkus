package org.otaibe.commons.quarkus.reflection.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReflectUtils {

    public static final Collection<Class<?>> cache = new ConcurrentLinkedQueue<>();

    public static Stream<Class<?>> getAllClassesFromPackage(String packageName, Class... baseClasses) {
        if (StringUtils.isBlank(packageName) || ArrayUtils.isEmpty(baseClasses)) {
            return new ArrayList<Class<?>>().stream();
        }

        Reflections reflections = new Reflections(packageName, new SubTypesScanner(false));

        return Arrays.stream(baseClasses)
                .flatMap(aClass -> reflections.getSubTypesOf(aClass).stream())
                .filter(aClass -> !((Class) aClass).isInterface())
                .filter(aClass -> ((Class) aClass).getPackage().getName().equals(packageName));
    }

    public static Stream<Class<?>> getAllAnnotatedClassesFromPackage(String packageName, Class<? extends Annotation>... annotations) {
        if (StringUtils.isBlank(packageName) || ArrayUtils.isEmpty(annotations)) {
            return new ArrayList<Class<?>>().stream();
        }

        Reflections reflections = new Reflections(packageName,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner()
        );

        return Arrays.stream(annotations)
                .flatMap(aClass -> reflections.getTypesAnnotatedWith(aClass).stream())
                .filter(aClass -> !((Class) aClass).isInterface())
                ;
    }

    public static void registerAdditionalClassesForReflection(String packageName) {
        registerAdditionalClassesForReflection(packageName, aClass -> ReflectUtils.registerForReflectionClass(aClass));
    }

    public static void registerAdditionalClassesForReflection(String packageName, Consumer<Class<?>> registerConsumer) {
        getAllAnnotatedClassesFromPackage(packageName, AdditionalClassesForReflection.class)
                .forEach(aClass -> {

                    Arrays.stream(aClass.getAnnotation(AdditionalClassesForReflection.class).targets())
                            .forEach(registerConsumer);

                    registerClassMethodReturnTypes(aClass, registerConsumer);
                });
    }

    public static void registerClassMethodReturnTypes(Class<?> aClass, Consumer<Class<?>> registerConsumer) {
        ReflectionUtils.getAllMethods(aClass, ReflectionUtils.withModifier(Modifier.PUBLIC))
                .stream()
                .filter(method -> {
                    Class<?> returnType = method.getReturnType();
                    return !returnType.isPrimitive();
                })
                .flatMap(method -> {
                    //log.info("method name: {}", method.getName());
                    Class<?> returnType = method.getReturnType();
                    Type type = method.getGenericReturnType();

                    Optional<ParameterizedType> parameterizedType1 = Optional.ofNullable(type)
                            .filter(ParameterizedType.class::isInstance)
                            .map(ParameterizedType.class::cast);

                    if (parameterizedType1.isPresent()) {
                        ParameterizedType parameterizedType = parameterizedType1.get();
                        //log.info("parameterizedType: {}", parameterizedType.getTypeName());
                        return Arrays.stream(parameterizedType.getActualTypeArguments())
                                .filter(type1 -> !WildcardType.class.isInstance(type1))
                                .filter(type1 -> !ParameterizedType.class.isInstance(type1))
                                .map(type1 -> (Class<?>) type1);
                    }

                    return Arrays.asList(returnType).stream();
                })
                .forEach(registerConsumer);
    }

    public static void registerPackage(String packageName, Class... baseClasses) {
        getAllClassesFromPackage(packageName, baseClasses)
                .forEach(aClass -> ReflectUtils.registerForReflectionClass(aClass));

    }

    public static void registerPackage(String packageName, Consumer<Class<?>> registerConsumer, Class<?>... baseClasses) {
        getAllClassesFromPackage(packageName, baseClasses).forEach(registerConsumer);
    }

    public static void registerForReflectionClass(Class<?> clazz) {
        registerForReflectionClass(false, clazz);
    }

    public static void registerForReflectionClass(boolean finalIsWritable, Class<?> clazz) {

        try {
            if (cache.contains(clazz)) {
                return;
            }

            Arrays.stream(clazz.getMethods())
                    .filter(method -> method.isAccessible())
                    .forEach(method -> RuntimeReflection.register(method));

            Arrays.stream(clazz.getFields())
                    .filter(field -> field.isAccessible())
                    .forEach(field -> RuntimeReflection.register(finalIsWritable, field));

            for (Class<?> clazz1 = clazz; !Object.class.equals(clazz1); clazz1 = clazz1.getSuperclass()) {
                if (clazz.isPrimitive() || clazz1.isInterface() || void.class.equals(clazz1)) {
                    break;
                }
                RuntimeReflection.register(clazz1.getDeclaredMethods());
                RuntimeReflection.register(finalIsWritable, clazz1.getDeclaredFields());
                RuntimeReflection.register(clazz1);
                RuntimeReflection.registerForReflectiveInstantiation(clazz1);
            }
        } catch (Exception e) {
        } finally {
            cache.add(clazz);
        }
    }

    public static List<Class<?>> extractParents(Class<?> clazz) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz1 = clazz; !Object.class.equals(clazz1); clazz1 = clazz1.getSuperclass()) {
            if (clazz.isPrimitive() || clazz1.isInterface() || void.class.equals(clazz1)) {
                break;
            }
            result.add(clazz1);
        }
        return result;
    }

    /**
     * Annotation that can be used to determine additional classes for reflection
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface AdditionalClassesForReflection {
        /**
         * Alternative classes that should actually be registered for reflection.
         *
         * This allows for classes in 3rd party libraries to be registered without modification.
         */
        Class<?>[] targets() default {};
    }

}
