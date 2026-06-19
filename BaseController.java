package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public abstract class BaseController {
    public abstract void execute(String methodName, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}