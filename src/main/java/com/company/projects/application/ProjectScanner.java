package com.company.projects.application;

import wiremock.org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectScanner {

    private final String STANDARD_CLASS_FILES_LOCATION = "target/classes";

    public List<File> getPackages(String packageName) throws IOException {
        String path = packageName.replace(".", "/");
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        List<File> resources = getResources(parentPath);
        List<File> packages = resources.stream()
                .filter(pack -> isPackageWithClasses(pack))
                .collect(Collectors.toList());
        return packages;
    }

    private boolean isPackageWithClasses(File pack) {
        return pack.isDirectory() && Arrays.stream(pack.listFiles())
                .filter(file -> file.getPath().endsWith(".class"))
                .collect(Collectors.toList())
                .size() > 0;
    }


    public List<Class> getClasses(List<File> packages) throws ClassNotFoundException {
        List<Class> classes = new LinkedList<>();
        for (File pack : packages) {
            String path = pack.getPath();
            String relPath = path.substring(path.indexOf(STANDARD_CLASS_FILES_LOCATION)
                    + STANDARD_CLASS_FILES_LOCATION.length() + 1);
            classes.addAll(Lists.newArrayList(findClasses(pack, relPath)));
        }
        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param pack   The base directory
     * @param packName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List findClasses(File pack, String packName) throws ClassNotFoundException {
        List classes = new ArrayList();
        if (!pack.exists()) {
            return classes;
        }
        File[] files = pack.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String packageName = packName.replace("/", ".");
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    public List<File> getResources(String path) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        Enumeration enumer = classLoader.getResources(path);
        List<File> resources = new ArrayList<>();
        if (enumer == null || !enumer.hasMoreElements()) {
            return resources;
        }
        while (enumer.hasMoreElements()) {
            Object next = enumer.nextElement();
            if (next == null) {
                continue;
            }
            URL resource = (URL) next;
            resources.add(new File(resource.getFile()));
        }
        collectChildrenIfExist(resources);
        return resources;
    }

    private void collectChildrenIfExist(List<File> resources) {
        if (CollectionUtils.isEmpty(resources)) {
            return;
        }
        List<File> children = new LinkedList<>();
        for (File resource : resources) {
            if (!resource.isDirectory() || resource.listFiles().length == 0) {
                return;
            }
            children.addAll(Arrays.asList(resource.listFiles()));
        }
        resources.addAll(children);
    }
}
