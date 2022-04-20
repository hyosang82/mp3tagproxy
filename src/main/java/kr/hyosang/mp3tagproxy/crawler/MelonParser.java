package kr.hyosang.mp3tagproxy.crawler;

import kr.hyosang.mp3tagproxy.HelloResource;
import kr.hyosang.mp3tagproxy.model.Album;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MelonParser extends ParserBase {
    @Override
    public List<Album> searchAlbumList(String strSearch) {
        List<Album> albums = new ArrayList<>();
        String enc = URLEncoder.encode(strSearch, StandardCharsets.UTF_8);

        int collected;
        int pageSize = 21;
        int page = 1;

        do {
            String url = "https://www.melon.com/search/album/index.htm?pageSize=" + pageSize + "&q=" + enc + "&sortorder=&section=artist&sectionId=&genreDir=";

            if(page > 1) {
                url += "&startIndex=" + (((page - 1) * pageSize) + 1);
            }

            collected = 0;

            String body = getHtmlContent(url);

            int idx1 = 0, idx2;
            int albumEndIdx;
            int tmp;
            FindString n = new FindString();
            while(true) {
                Album alb = new Album();
                idx1 = body.indexOf("<li class=\"album11_li\">", idx1);
                if(idx1 >= 0) {
                    idx2 = body.indexOf("<li class=\"album11_li\">", idx1 + 10);
                    if(idx2 == -1) {
                        idx2 = body.indexOf("</li>", idx1);
                    }
                    albumEndIdx = idx2;

                    String album = body.substring(idx1, idx2);

                    // 앨범별 데이터 취득
                    Pattern p = Pattern.compile("goAlbumDetail\\('([0-9]+)'\\)");
                    Matcher m = p.matcher(album);

                    if(m.find()) {
                        alb.idx = m.group(1);
                    }

                    n.blockFromIndex = album.indexOf("thumb_frame");
                    n.fromString = "src=\"";
                    n.endString = "\"";
                    findStr(album, n);
                    alb.coverArt = n.foundString;

                    tmp = album.indexOf("앨범명");
                    tmp = album.indexOf("<a", tmp);

                    n.blockFromIndex = tmp;
                    n.fromString = ">";
                    n.endString = "</a>";
                    findStr(album, n);
                    alb.title = n.foundString;

                    albums.add(alb);
                    collected++;

                    idx1 = albumEndIdx;
                }else {
                    break;
                }
            }

            page++;
        }while((collected > 0) && (page < 5));

        return albums;
    }

    @Override
    public Album getAlbumDetail(String idx) {
        String url = "https://www.melon.com/album/detail.htm?albumId=" + idx;
        String body = getHtmlContent(url);

        //System.out.println(body);

        Album album = new Album();

        // 주요 부분 추출
        int idx1 = body.indexOf("<div class=\"section_info\">");
        if(idx1 > 0) {
            int idx2 = body.indexOf("<div class=\"section_albuminfo\">", idx1);
            if(idx2 > 0) {
                body = body.substring(idx1, idx2);
            }
        }

        FindString n = new FindString();
        n.blockFromIndex = body.indexOf("<div class=\"song_name\">");
        n.fromString = "</strong>";
        n.endString = "</";
        findStr(body, n);

        album.title = n.foundString;

        n.blockFromIndex = body.indexOf("<div class=\"artist\">");
        n.fromString = "<span>";
        n.endString = "</span>";
        findStr(body, n);

        album.artist = n.foundString;

        n.blockFromIndex = body.indexOf("WEBPOCIMG.defaultAlbumImg");
        n.fromString = "src=\"";
        n.endString = "\"";
        findStr(body, n);

        String tmp = n.foundString;
        if(tmp.indexOf("/melon/resize") > 0) {
            album.coverArt = n.foundString.substring(0, n.foundString.indexOf("/melon/resize"));
        }else {
            album.coverArt = n.foundString;
        }

        System.out.println("COVERART => " + album.coverArt);

        n.fromString = "<div class=\"meta\">";
        n.endString = "</div>";
        findStr(body, n);

        //System.out.println(n.foundString);

        Pattern p = Pattern.compile("<d[dt]>(.*)</d[dt]>");
        Matcher m = p.matcher(n.foundString);
        String lastStr = null;
        while(m.find()) {
            if(lastStr == null) {
                lastStr = m.group(1);
            }else {
                if("발매일".equals(lastStr)) {
                    album.releaseDate = m.group(1).replaceAll("[^0-9]", "");
                }else if("장르".equals(lastStr)) {
                    album.genre = m.group(1);
                }else if("발매사".equals(lastStr)) {
                    album.publisher = m.group(1);
                }else if("기획사".equals(lastStr)) {
                    album.agency = m.group(1);
                }
                lastStr = null;
            }
        }

        // 곡목록 추출
        int tIdx;
        idx1 = body.indexOf("<tbody>");
        if(idx1 > 0) {
            int idx2 = body.indexOf("</tbody>", idx1);
            if((idx2 > 0) && (idx1 < idx2)) {
                String tracks = body.substring(idx1, idx2);
                idx1 = tracks.indexOf("<tr");
                while(idx1 >= 0) {
                    idx2 = tracks.indexOf("</tr>", idx1);

                    String trk = tracks.substring(idx1, idx2);

                    tIdx = trk.indexOf("<div class=\"wrap_song_info\">");
                    if(tIdx > 0) {
                        int t1 = trk.indexOf("</div>", tIdx);
                        tmp = trk.substring(tIdx, t1);
                        tIdx = tmp.indexOf("<span class=\"none\">Title</span>");
                        if(tIdx > 0) {
                            tmp = tmp.substring(tIdx + 31);
                        }

                        String title = tmp.replaceAll("<[^>]+>", "").trim();

                        tmp = trk.substring(t1, trk.indexOf("</div>", t1+6));
                        t1 = tmp.indexOf("<span");
                        tmp = tmp.substring(t1, tmp.indexOf("</span>", t1));

                        String artist = tmp.replaceAll("<[^>]+>", "").trim();

                        album.addTrack(title, artist);

                        //System.out.println("TITLE = " + title + ", ARTIST = " + artist);
                    }

                    idx1 = tracks.indexOf("<tr", idx2);
                }
            }
        }

        return album;
    }
}
