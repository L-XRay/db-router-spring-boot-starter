package cn.ray.middleware.db.router.utils;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Ray
 * @date 2022/10/14 15:36
 * @description
 */
public class PropertyUtil {

    private static int springBootVersion = 1;

    static {
        try {
            Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
        } catch (ClassNotFoundException e) {
            springBootVersion = 2;
        }
    }

    /**
     * Spring Boot 1.x is compatible with Spring Boot 2.x by Using Java Reflect.
     * @param environment : the environment context
     * @param prefix : the prefix part of property key
     * @param targetClass : the target class type of result
     * @param <T> : refer to @param targetClass
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T handle(final Environment environment, final String prefix, final Class<T> targetClass) {
        switch (springBootVersion) {
            case 1:
                return (T) v1(environment, prefix);
            default:
                return (T) v2(environment, prefix, targetClass);
        }
    }

    private static Object v1(final Environment environment, final String prefix) {
        try {
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            Object resolverObject = resolverConstructor.newInstance(environment);
            String prefixParam = prefix.endsWith(".") ? prefix : prefix + ".";
            return getSubPropertiesMethod.invoke(resolverObject, prefixParam);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                       | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    // spring 2.x
    // https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/context/properties/bind/Binder.html
    private static Object v2(final Environment environment, final String prefix, final Class<?> targetClass) {
        try {
            Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            // ?????? binderClass.get ??????
            Method getMethod = binderClass.getDeclaredMethod("get", Environment.class);
            // ?????? binderClass.bind ??????
            Method bindMethod = binderClass.getDeclaredMethod("bind", String.class, Class.class);
            // ???????????????????????????binder??????
            Object binderObject = getMethod.invoke(null, environment);
            // ??????????????????
            // prefix : ray-db-router.jdbc.datasource.db01???ray-db-router.jdbc.datasource.db02
            String prefixParam = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
            // ?????????????????????????????????
            // targetClass : Map.Class
            Object bindResultObject = bindMethod.invoke(binderObject, prefixParam, targetClass);
            // ?????????????????????get??????
            Method resultGetMethod = bindResultObject.getClass().getDeclaredMethod("get");
            // ??????????????????
            return resultGetMethod.invoke(bindResultObject);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                       | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}
