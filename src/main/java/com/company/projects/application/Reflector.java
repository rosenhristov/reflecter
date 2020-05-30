package com.company.projects.application;

import java.io.File;
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

    public static String RELATIVE_CLASS_PACKAGES_ROOT;

    public List<File> getAppPackages(String classesRootPath) {
        if (isEmpty(classesRootPath)) {
            return null;
        }
        List<File> packages = new ArrayList<>();
        File root = new File(classesRootPath);
        List<String> dirs = listPackages(root);
        for (String dir : dirs) {
            String relPath = RELATIVE_CLASS_PACKAGES_ROOT.replace(".", "/") + "/" + dir;
            URL url = ClassLoader.getSystemClassLoader().getResource(relPath);
            if (url == null) {
                throw new RuntimeException("No resource for " + relPath);
            }
            File pack;
            try {
                pack = new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(dir + ": (" + url + ") does not appear to be a valid URL/URI.", e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (pack != null) {
                packages.add(pack);
            }
        }
        return packages;
    }

    public String getClassesRootPath(String projectLocation) {
        if (isEmpty(projectLocation)) {
            return null;
        }
        File project = new File(projectLocation);
        List<File> packages = Arrays.asList(project.listFiles());
        File target = getSpecificPackage(packages, "target");
        File classes = getSpecificChildPackage(target, "classes");
        if (containsClasses(classes)) {
            return classes.getPath();
        }
        List<File> children = Arrays.asList(classes.listFiles());
        classes = excludePackagesAndFiles(children, Arrays.asList(new String[]{"META-INF"}));
        while(!containsClasses(classes)) {
            File[] child = classes.listFiles();
            classes = child[0];
        }
        return classes.getParent();
    }

    private boolean containsClasses(File pack) {
        if (isEmpty(pack.listFiles())) {
            return false;
        }
        return Arrays.stream(pack.listFiles())
                .filter(file -> file.isFile() && file.getName().endsWith(".class"))
                .collect(Collectors.toList())
                .size() > 0;

    }

    private File excludePackagesAndFiles(List<File> packLages, List<String> packagesToExclude) {
        return packLages.stream()
                .filter(pack -> pack.isDirectory() && !packagesToExclude.contains(pack.getName()))
                .collect(Collectors.toList()).get(0);
    }

    public File getSpecificChildPackage(File pack, String packageName) {
        return Arrays.stream(pack.listFiles()).filter(
                file -> file.getName().equals(packageName)).collect(Collectors.toList()).get(0);
    }

    private File getSpecificPackage(List<File> packages, String packageName) {
        return packages.stream().filter(
                file -> file.getName().equals(packageName)).collect(Collectors.toList()).get(0);
    }


    public List<String> listPackages(File dir) {
        String[] directories = dir.list((current, name) -> new File(current, name).isDirectory());
    return isEmpty(directories) ? null : Arrays.asList(directories);
    }

    public List<Class<?>> getApplicationClasses(List<File> packages) {
       return  packages.stream()
                .flatMap(pack -> getClassesOfPackage(pack).stream())
                .collect(Collectors.toList());
    }

    private List<Class> collectClasses(List<File> content) throws ClassNotFoundException {
        List<Class> classes = new LinkedList<>();
        for (File element : content) {
            if (element.getName().endsWith(".class")) {
                Class kl = Class.forName(element.getPath());
                classes.add(kl);
            } else {
                collectClasses(Arrays.asList(element.listFiles()));
            }
        }
        return classes;
    }

    public List<Class<?>> getClassesOfPackage(File pack) {
        List<Class<?>> classes = new ArrayList<>();
        if (pack != null && pack.exists()) {
            List<String> files = Arrays.asList(pack.list());
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i).endsWith(".class")) {
                    String className = String.join(".", RELATIVE_CLASS_PACKAGES_ROOT,
                            pack.getName(), files.get(i).substring(0, files.get(i).length() - 6));
                    try {
                        Class<?> cl = Class.forName(className);
                        classes.add(cl);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("ClassNotFoundException occurred while loading " + className, e);
                    }
                }
            }
        }
        return classes;
    }

    public List<Class> collectServices(List<Class<?>> classes) {
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

    public String getPackagesRoot(String projectLocation) {
        if (isEmpty(projectLocation)) {
            return null;
        }
        File project = new File(projectLocation);
        List<File> packages = Arrays.asList(project.listFiles());
        File target = getSpecificPackage(packages, "target");
        File classes = getSpecificChildPackage(target, "classes");
        List<File> children = Arrays.asList(classes.listFiles());
        classes = excludePackagesAndFiles(children, Arrays.asList(new String[]{"META-INF"}));
        String packagesRoot = "";
        while(!containsClasses(classes)) {
            File[] child = classes.listFiles();
            classes = child[0];
            if (packagesRoot == "") {
                packagesRoot = classes.getParent();
                packagesRoot = packagesRoot.substring(packagesRoot.lastIndexOf("/") + 1);
            } else {
                String shortName = classes.getParent().substring(classes.getParent().lastIndexOf("/") + 1);
                packagesRoot += "/" + shortName;
            }
        }
        RELATIVE_CLASS_PACKAGES_ROOT = packagesRoot.replace("/", ".");
        return packagesRoot;
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
