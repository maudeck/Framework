package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import framework.annotations.Controller;
import framework.annotations.URLMapping;
import framework.routing.Mapping;
import framework.utils.PackageScanner;

public class FrontControllerServlet extends HttpServlet {

    private String controllersPackage;
    private HashMap<String, Mapping> mappingUrls;

    @Override
    public void init() throws ServletException {
        try {
            loadParams();
            buildRoutes();
        } catch (Exception e) {
            throw new ServletException("Échec de l'initialisation du FrontController: ", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private void loadParams() {
        final String PACKAGE_NAME_PARAM = "controller_package_name";
        this.controllersPackage = this.getServletContext().getInitParameter(PACKAGE_NAME_PARAM);
        if (this.controllersPackage == null || this.controllersPackage.trim().isEmpty()) {
            throw new IllegalArgumentException("Le paramètre d'initialisation 'controller_package_name' est manquant.");
        }
    }

    private void buildRoutes() throws Exception {
        this.mappingUrls = new HashMap<>();

        List<Class<?>> controllers = PackageScanner.getAnnotatedClassesInPackage(
            controllersPackage,
            Controller.class
        );

        for (Class<?> clazz : controllers) {
            String fullClassName = clazz.getName();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(URLMapping.class)) {
                    URLMapping mappingAnnotation = method.getAnnotation(URLMapping.class);
                    String url = mappingAnnotation.value();

                    if (mappingUrls.containsKey(url)) {
                        throw new IllegalArgumentException("URL dupliquée: " + url);
                    }

                    mappingUrls.put(url, new Mapping(fullClassName, method.getName()));
                }
            }
        }
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();
        String url = requestURI.substring(contextPath.length());

        res.setContentType("text/html; charset=UTF-8");

        try {
            if (mappingUrls.containsKey(url)) {
                Mapping mapping = mappingUrls.get(url);
                Class<?> controllerClass = Class.forName(mapping.getClassName());
                Object controller = controllerClass.getDeclaredConstructor().newInstance();
                Method method = controllerClass.getDeclaredMethod(mapping.getMethod());
                method.setAccessible(true);
                Object result = method.invoke(controller);
                try (PrintWriter out = res.getWriter()) {
                    out.println("<!DOCTYPE html><html><body>");
                    if (result instanceof String html) {
                        out.println(html);
                    } else {
                        out.println("<pre>" + url + " -> " + mapping.getClassName() + " -> " + mapping.getMethod() + "()</pre>");
                    }
                    out.println("</body></html>");
                }
            } else {
                try (PrintWriter out = res.getWriter()) {
                    out.println("<!DOCTYPE html><html><body>");
                    out.println("<h1>URL non trouvee : " + url + "</h1>");
                    out.println("<h2>Routes disponibles :</h2><ul>");
                    for (Map.Entry<String, Mapping> entry : mappingUrls.entrySet()) {
                        Mapping m = entry.getValue();
                        out.println("<li>" + entry.getKey() + " -> " + m.getClassName() + " -> " + m.getMethod() + "()</li>");
                    }
                    out.println("</ul>");
                    out.println("</body></html>");
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur interne: " + e.getMessage(), e);
        }
    }
}
