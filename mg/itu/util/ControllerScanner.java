package mg.itu.util;

import jakarta.servlet.ServletContext;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import mg.itu.annotation.Controller;

public class ControllerScanner {
    public static List<String> scanControllers(ServletContext servletContext, String basePackage) throws Exception {
        List<String> controllers = new ArrayList<>();

        if (servletContext == null || basePackage == null || basePackage.isBlank()) {
            return controllers;
        }

        String packagePath = basePackage.replace('.', '/');
        scanClassesDirectory(servletContext, controllers, "/WEB-INF/classes/" + packagePath, "/WEB-INF/classes/");
        scanLibJars(servletContext, controllers, packagePath);

        return controllers;
    }

    public static boolean isController(Class<?> clazz) {
        return clazz != null && clazz.isAnnotationPresent(Controller.class);
    }

    private static void scanClassesDirectory(ServletContext servletContext, List<String> controllers, String path, String rootPath) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(path);
        if (resourcePaths == null) {
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith("/")) {
                scanClassesDirectory(servletContext, controllers, resourcePath, rootPath);
            } else if (resourcePath.endsWith(".class")) {
                String className = resourcePath.substring(rootPath.length(), resourcePath.length() - 6).replace('/', '.');
                addControllerIfAnnotated(controllers, className);
            }
        }
    }

    private static void scanLibJars(ServletContext servletContext, List<String> controllers, String packagePath) throws Exception {
        Set<String> jars = servletContext.getResourcePaths("/WEB-INF/lib");
        if (jars == null) {
            return;
        }

        for (String jarPath : jars) {
            if (!jarPath.endsWith(".jar")) {
                continue;
            }

            try (JarFile jarFile = openJarFile(servletContext, jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    if (!entry.getName().startsWith(packagePath + "/")) {
                        continue;
                    }

                    String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    addControllerIfAnnotated(controllers, className);
                }
            }
        }
    }

    private static JarFile openJarFile(ServletContext servletContext, String jarPath) throws Exception {
        String realPath = servletContext.getRealPath(jarPath);
        if (realPath != null) {
            return new JarFile(realPath);
        }

        URL url = servletContext.getResource(jarPath);
        if (url == null) {
            return null;
        }

        JarURLConnection connection = (JarURLConnection) url.openConnection();
        return connection.getJarFile();
    }

    private static void addControllerIfAnnotated(List<String> controllers, String className) throws Exception {
        try {
            Class<?> clazz = Class.forName(className);
            if (isController(clazz) && !controllers.contains(className)) {
                controllers.add(className);
            }
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
