package kr.hyosang.mp3tagproxy.api.melon;

import kr.hyosang.mp3tagproxy.api.ServletBase;
import kr.hyosang.mp3tagproxy.crawler.MelonParser;
import kr.hyosang.mp3tagproxy.crawler.ParserBase;
import kr.hyosang.mp3tagproxy.model.Album;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/api/search")
public class AlbumSearch extends ServletBase {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter("q");
        String src = req.getParameter("s");

        ParserBase parser;
        if("melon".equals(src)) {
            parser = new MelonParser();
        }else {
            parser = new MelonParser();
        }

        List<Album> albums = parser.searchAlbumList(query);
        StringBuffer sb = new StringBuffer();
        for(Album a : albums) {
            sb.append(a.idx).append("|").append(a.title).append("|").append(a.coverArt).append("\n");
        }

        responseAsText(resp, sb.toString());
    }
}
