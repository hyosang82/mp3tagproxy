package kr.hyosang.mp3tagproxy.crawler;

import kr.hyosang.mp3tagproxy.HelloResource;
import kr.hyosang.mp3tagproxy.model.Album;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class ParserBase {
    public abstract List<Album> searchAlbumList(String strSearch);
    public abstract Album getAlbumDetail(String idx);

    protected String getHtmlContent(String url) {
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();

            //conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            //conn.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
            //conn.addRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
            //conn.addRequestProperty("Referer", "https://www.melon.com/search/total/index.htm?q=%EC%84%9C%ED%83%9C%EC%A7%80&section=&mwkLogType=T");
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
            conn.setDoInput(true);

            System.out.println("STATUS =>" + conn.getResponseMessage());

            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);

            char [] buf = new char[1024];
            StringBuffer sb = new StringBuffer();
            int read = 0;

            while((read = reader.read(buf)) > 0) {
                sb.append(buf, 0, read);
            }

            return sb.toString();
        }catch(IOException e) {
            e.getStackTrace();

        }

        return "";
    }

    public void findStr(String data, FindString needle) {
        int idx1 = data.indexOf(needle.fromString, needle.blockFromIndex);
        if(idx1 >= 0) {
            idx1 += needle.fromString.length();
            int idx2 = data.indexOf(needle.endString, idx1);

            if((idx2 >= 0) && (idx1 <= idx2)) {
                needle.foundString = data.substring(idx1, idx2).trim();
                needle.endIndex = idx2;
            }else {
                needle.endIndex = idx1;
            }
        }else {
            needle.endIndex = needle.blockFromIndex;
        }
    }

    protected class FindString {
        public int blockFromIndex;
        public String fromString;
        public String endString;

        // out
        public String foundString = "";
        public int endIndex;
    }
}
