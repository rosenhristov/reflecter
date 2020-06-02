package com.company.projects.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.company.projects.annotations.IAmService;
import com.company.projects.annotations.INeedThis;

import static com.company.projects.utils.Utils.isEmpty;
import static com.company.projects.utils.Utils.isNotEmpty;

public class Reflector {

    public List<File> getAppPackages(String packageName) {
       ProjectScanner projectScanner = new ProjectScanner();
        List<File> packages = new LinkedList<>();
       try {
           packages = projectScanner.getPackages(packageName);
       } catch (IOException e) {
           System.out.println("Exception occurred during collection of project packages:"
                   + e.getMessage());
       }
       return packages;
    }

    public List<Class> getApplicationClasses(List<File> packages) {
        ProjectScanner projectScanner = new ProjectScanner();
        List<Class> classes = new LinkedList<>();
        try {
            classes = projectScanner.getClasses(packages);
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Exception occurred during collection of project classes:"
                    + e.getMessage());
        }
        return classes;
    }

    public List<Class> collectServices(List<Class> classes) {
        return classes.stream().filter(klass -> isService(klass)).collect(Collectors.toList());
    }

    public BeanDescriptor mapDependencies(Class<?> klass) {
        BeanDescriptor beanDescriptor = new BeanDescriptor(klass);
        Optional<Constructor> annotatedConstructorOpt = getAnnotatedConstructor(klass);

        if (annotatedConstructorOpt.isPresent()) {
            beanDescriptor.setAnnotatedConstructor(annotatedConstructorOpt.get());
            beanDescriptor.setConstructorDependencies(
                    Arrays.asList(annotatedConstructorOpt.get().getParameterTypes()));
        }

        List<Method> annotatedMethods = getAnnotatedMethods(klass);
        Map<String, Class<?>> annotatedMethodDepsMap =
            (isEmpty(annotatedMethods))
                ? new HashMap<>()
                : annotatedMethods
                    .stream()
                    .collect(Collectors.toMap(
                                Method::getName,
                                method -> method.getParameterTypes()[0]));
        beanDescriptor.setMethodDependencies(annotatedMethodDepsMap);

        List<Field> annotatedFields = getAnnotatedFields(klass);
        Map<String, Class<?>> annotatedFieldDepsMap =
                isEmpty(annotatedFields)
                    ? new HashMap<>()
                    : annotatedFields
                        .stream()
                        .collect(Collectors.toMap(
                                    field -> field.getName(),
                                    field -> field.getType()));
        beanDescriptor.setFieldDependencies(annotatedFieldDepsMap);

        return beanDescriptor;
    }


    public Optional<Constructor> getAnnotatedConstructor(Class klass) {
        Optional<Constructor> optionalConstructor =
                Arrays.stream(klass.getDeclaredConstructors())
                      .filter(constructor -> constructor.isAnnotationPresent(INeedThis.class))
                      .findFirst();
        return optionalConstructor.isPresent() ? optionalConstructor : Optional.empty();
    }

    public List<Method> getAnnotatedMethods(Class klass) {
        return Arrays
                .stream(klass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(INeedThis.class))
                .collect(Collectors.toList());
    }

    public List<Field> getAnnotatedFields(Class klass) {
        return Arrays
                .stream((klass.getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(INeedThis.class))
                .collect(Collectors.toList());
    }

    public void wireBeans(List<BeanDescriptor> descriptors, Map<String, Object> beanInstances) {
        for (BeanDescriptor descriptor : descriptors) {
            Class bean = descriptor.getBean();
            Object beanInstance = null;
            if (!beanInstances.containsKey(bean.getSimpleName())) {
                beanInstance = wireBean(descriptor, beanInstances);
            }
            if (beanInstance != null
                    && !beanInstances.containsKey(beanInstance.getClass().getSimpleName())) {
                beanInstances.put(beanInstance.getClass().getSimpleName(), beanInstance);
            }
        }
    }

    private Object wireBean(BeanDescriptor descriptor, Map<String, Object> beanInstances) {
        Class<?> bean = descriptor.getBean();
        String beanName = bean.getSimpleName();

        if (beanInstances.containsKey(bean.getSimpleName())) {
            return beanInstances.get(bean.getSimpleName());
        }

        List<Class<?>> constructorDependencies = descriptor.getConstructorDependencies();
        Map<String, Class<?>> methodDependencies = descriptor.getMethodDependencies();
        Map<String, Class<?>> fieldDependencies = descriptor.getFieldDependencies();

        Object beanInstance = null;
        try {
            if (isEmpty(constructorDependencies)) {
                beanInstance = bean.newInstance();
            } else {
                Object[] dependencyInstances = constructorDependencies.stream()
                                .map(d -> wireBean(mapDependencies(d), beanInstances))
                                .collect(Collectors.toList()).toArray();
                beanInstance = descriptor.getAnnotatedConstructor().newInstance(dependencyInstances);
            }

            if (isNotEmpty(methodDependencies)) {
                for (Map.Entry<String, Class<?>> entry : methodDependencies.entrySet()) {
                    injectDependencyByMethod(bean, beanInstance, entry, beanInstances);
                }
            }

            if (isNotEmpty(fieldDependencies)) {
                for (Map.Entry<String, Class<?>> entry : fieldDependencies.entrySet()) {
                    injectDependencyByField(bean, beanInstance, entry, beanInstances);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (beanInstance != null) {
            beanInstances.put(beanName, beanInstance);
        }
        return beanInstance;
    }

    private void injectDependencyByMethod(Class<?> bean, Object beanInstance,
                                          Map.Entry<String, Class<?>> methodDependencyEntry,
                                          Map<String, Object> beanInstances) throws Exception {
        Object dependencyInstance = beanInstances.containsKey(methodDependencyEntry.getKey())
                ? beanInstances.get(methodDependencyEntry.getKey())
                : wireBean(mapDependencies(methodDependencyEntry.getValue()), beanInstances);
        if (dependencyInstance == null) {
            System.out.printf("Method dependency %s of bean %s could not instantiated",
                    methodDependencyEntry.getKey(), bean.getSimpleName());
        }
        Method method = bean.getDeclaredMethod(
                methodDependencyEntry.getKey(), new Class[] {methodDependencyEntry.getValue()});
        method.setAccessible(true);
        method.invoke(beanInstance, dependencyInstance);
    }

    private void injectDependencyByField(Class<?> bean, Object beanInstance,
                                         Map.Entry<String, Class<?>> fieldDependencyEntry,
                                         Map<String, Object> beanInstances) throws Exception {
        Object dependencyInstance = beanInstances.containsKey(fieldDependencyEntry.getKey())
                ? beanInstances.get(fieldDependencyEntry.getKey())
                : wireBean(mapDependencies(fieldDependencyEntry.getValue()), beanInstances);
        if (dependencyInstance == null) {
            System.out.printf("Field dependency %s of bean %s could not instantiated",
                    fieldDependencyEntry.getKey(), bean.getSimpleName());
        }
        Field field = bean.getDeclaredField(fieldDependencyEntry.getKey());
        field.setAccessible(true);
        field.set(beanInstance, dependencyInstance);
    }

    public static boolean isService(Class<?> klass) {
        return klass.isAnnotationPresent(IAmService.class);
    }
}
