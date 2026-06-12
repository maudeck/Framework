import java.io.*;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    
    private Map<String, Mapping> mappings = new HashMap<>();
    
    public void init() throws ServletException {
        mappings.put("test", new Mapping("mg.itu.TestController", "show"));
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        String action = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : "test";
        
        Mapping mapping = mappings.get(action);
        if (mapping == null) {
            action = "test";
            mapping = mappings.get(action);
        }
        
        try {
            Class<?> controllerClass = Class.forName(mapping.getClassName());
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            
            if (controller instanceof BaseController) {
                ((BaseController) controller).execute(mapping.getMethodName(), request, response);
            }
        } catch (Exception e) {
            out.println("<h1>Erreur</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}