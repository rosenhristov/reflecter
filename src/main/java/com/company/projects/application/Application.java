package com.company.projects.application;

import com.company.projects.services.IService;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Application {

    public static void main(String[] args) {

        Reflector reflector = new Reflector();

        List<File> packages = reflector.getAppPackages(Application.class.getPackage().getName());
        System.out.println("Packages found in the project:\n==========================");
        packages.forEach(p -> System.out.println(p.getName()));

        List<Class> classes = reflector.getApplicationClasses(packages);
        System.out.println("\nClasses found in the project:\n==========================");
        classes.forEach(c -> System.out.println(c.getSimpleName()));

        List<Class> services = reflector.collectServices(classes);

        List<BeanDescriptor> beanDescriptors = new LinkedList<>();
        for (Class service : services) {
            BeanDescriptor beanDescriptor = reflector.mapDependencies(service);
            beanDescriptors.add(beanDescriptor);
        }
        System.out.println("\nProject classes annotated as services:" +
                "\n=====================================");
        listServicesAndTheirDependencies(services, beanDescriptors);

        System.out.println("\nAutowiring services:\n===================");
        Map<String, Object> serviceInstancesMap = new LinkedHashMap<>();
        reflector.wireBeans(beanDescriptors, serviceInstancesMap);
        Collection<Object> serviceInstances = serviceInstancesMap.values();

        System.out.printf("\nIn summary, %s services were autowired:"
                         + "\n=====================================\n", serviceInstancesMap.size());
        serviceInstancesMap
                .values()
                .forEach(s -> System.out.print(s.getClass().getSimpleName() + "; "));

        Map<Integer, IService> serviceDepsMap = new HashMap<>();
        serviceInstances.forEach(service ->
                serviceDepsMap.put(
                    reflector.mapDependencies(service.getClass()).collectAllDependencies().size(),
                    (IService) service
                )
        );
        int maxDeps = serviceDepsMap.keySet().stream()
                                    .max((o1, o2) -> o1 == o2 ? 0 : o1 > o2 ? 1 : -1)
                                    .get();
        System.out.println("\n\nRecursive printing of simple names:"
                + "\n==================================");
        serviceDepsMap.get(maxDeps).printDeep();
    }

    public static void listServicesAndTheirDependencies(List<Class> services,
                                                        List<BeanDescriptor> beanDescriptors) {
        services.forEach(s -> System.out.printf("%s;  ", s.getSimpleName()));
        System.out.println("\n\nServices dependencies:\n=====================");
        beanDescriptors.forEach(d -> System.out.printf("Service %s\n", d.listAllDependencies()));
    }
}