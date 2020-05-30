package com.company.projects.application;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.company.projects.utils.Utils.isEmpty;

public class BeanDescriptor {

    private Class<?> bean;
    private Constructor<?> annotatedConstructor;
    private List<Class<?>> constructorDependencies;
    private Map<String, Class<?>> methodDependencies;
    private Map<String, Class<?>> fieldDependencies;


    public BeanDescriptor(Class<?> depender) {
        this.bean = depender;
        this.constructorDependencies = new LinkedList<>();
        this.methodDependencies = new HashMap<>();
        this.fieldDependencies = new HashMap<>();
    }

    public Class<?> getBean() {
        return bean;
    }

    public void setBean(Class<?> bean) {
        this.bean = bean;
    }

    public Constructor<?> getAnnotatedConstructor() {
        return annotatedConstructor;
    }

    public void setAnnotatedConstructor(Constructor<?> annotatedConstructor) {
        this.annotatedConstructor = annotatedConstructor;
    }

    public List<Class<?>> getConstructorDependencies() {
        return this.constructorDependencies;
    }

    public void setConstructorDependencies(List<Class<?>> constructorDependencies) {
        this.constructorDependencies = constructorDependencies;
    }

    public Map<String, Class<?>> getMethodDependencies() {
        return methodDependencies;
    }

    public void setMethodDependencies(Map<String, Class<?>> methodDependencies) {
        this.methodDependencies = methodDependencies;
    }

    public Map<String, Class<?>> getFieldDependencies() {
        return fieldDependencies;
    }

    public void setFieldDependencies(Map<String, Class<?>> fieldDependencies) {
        this.fieldDependencies = fieldDependencies;
    }

    public List<Class<?>> collectAllDependencies() {
        return ImmutableList.<Class<?>>builder()
                .addAll(constructorDependencies)
                .addAll(methodDependencies.values())
                .addAll(fieldDependencies.values())
                .build();
    }

    public String listAllDependencies() {
        List<Class<?>> allDependencies = collectAllDependencies();
        if (isEmpty(allDependencies)) {
            return bean.getSimpleName() + " has no dependencies";
        }
        StringBuilder lister = new StringBuilder();
        for (int i = 0; i < allDependencies.size(); i++) {
            Class cl = allDependencies.get(i);
            if(lister.toString().isEmpty()) {
                lister.append(bean.getSimpleName() + " depends on ");
                lister.append(allDependencies.size() == 1
                        ? cl.getSimpleName()
                        : cl.getSimpleName() + ", ");
            } else if (i == allDependencies.size() - 1) {
                lister.append(cl.getSimpleName());
            } else {
                lister.append(cl.getSimpleName() + ", ");
            }
        }
        return lister.toString();
    }
}
