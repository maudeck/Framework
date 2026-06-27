package framework.utils;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.*;
import java.util.*;
import java.util.jar.*;

public class PackageScanner {

    public static List<Class<?>> getAnnotatedClassesInPackage(
        String packageName,
        Class<? extends Annotation> annotation
    ) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = PackageScanner.findClassesInPackage(packageName);
        List<Class<?>> annotated = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotation(annotation) != null) {
                annotated.add(clazz);
            }
        }
        return annotated;
    }

    public static List<String> getAnnotatedClassesNamesInPackage(
        String packageName,
        Class<? extends Annotation> annotation
    ) throws IOException, ClassNotFoundException {
        List<String> classesNames = new ArrayList<>();
        List<Class<?>> classes = PackageScanner.findClassesInPackage(packageName);
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotation(annotation) != null) {
                classesNames.add(clazz.getName());
            }
        }

        return classesNames;
    }

    public static List<Class<?>> findClassesInPackage(String packageName)
    throws IOException, ClassNotFoundException {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread()
                .getContextClassLoader()
                .getResources(path);

        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("jar".equals(resource.getProtocol())) {
                classes.addAll(findClassesInJar(resource, packageName));
            } else {
                classes.addAll(findClassesInDirectory(new File(resource.getFile()), packageName));
            }
        }
        return classes;
    }

    private static List<Class<?>> findClassesInDirectory(File dir, String packageName)
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!dir.exists())
            return classes;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String name = packageName + "." + file.getName().replace(".class", "");
                classes.add(Class.forName(name));
            }
        }
        return classes;
    }

    private static List<Class<?>> findClassesInJar(URL resource, String packageName)
            throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
            String prefix = packageName.replace('.', '/');
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }

}