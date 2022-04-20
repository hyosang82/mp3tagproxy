package kr.hyosang.mp3tagproxy.api.melon;

import kr.hyosang.mp3tagproxy.api.ServletBase;
import kr.hyosang.mp3tagproxy.crawler.MelonParser;
import kr.hyosang.mp3tagproxy.crawler.ParserBase;
import kr.hyosang.mp3tagproxy.model.Album;
import kr.hyosang.mp3tagproxy.model.Track;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/api/album")
public class MelonAlbumDetail extends ServletBase {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idx = req.getParameter("i");
        String src = req.getParameter("s");

        ParserBase parser;
        if("melon".equals(src)) {
            parser = new MelonParser();
        }else {
            parser = new MelonParser();
        }

        Album album = parser.getAlbumDetail(idx);

        StringBuffer sb =new StringBuffer();

        sb.append(album.title).append("|").append(album.artist).append("|").append(album.releaseDate).append("\n");

        for(Track t : album.getTracks()) {
            sb.append(t.getTitle()).append("|").append(t.getArtist());
        }

        responseAsText(resp, sb.toString());


    }
}
