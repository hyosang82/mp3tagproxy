package kr.hyosang.mp3tagproxy;

import kr.hyosang.mp3tagproxy.crawler.MelonParser;
import kr.hyosang.mp3tagproxy.model.Album;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "hello", urlPatterns = "/")
public class HelloResource extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String str = "서태지";
        MelonParser parser = new MelonParser();
        /*
        List<Album> albums = parser.searchAlbumList(str);
        for(Album a : albums) {
            resp.getWriter().println(a.title + "\n");
        }

         */

        parser.getAlbumDetail("10554246");
    }
}