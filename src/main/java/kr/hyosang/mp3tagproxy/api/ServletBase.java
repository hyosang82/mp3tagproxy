package kr.hyosang.mp3tagproxy.api;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class ServletBase extends HttpServlet {
    public void responseAsText(HttpServletResponse resp, String data) throws IOException {
        resp.setHeader("Content-Type", "text/plain; charset=utf-8");
        resp.getWriter().print(data);
    }
}
