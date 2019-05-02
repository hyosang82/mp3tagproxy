package kr.hyosang.grabber;

import java.util.ArrayList;

public class ConsoleMain {
    public static final void main(String[] args) {
        Melon m = new Melon();
        //ArrayList<AlbumSearchItem> result = m.searchAlbum("서태지");
        //System.out.println("Search: " + result);
        //Album a = m.getAlbumDetail("3736");
//        System.out.println("Album: " + a.toJsonStringify());

//        ArrayList<SongSearchItem> list = m.searchSong("서태지+수시아");
//        for(SongSearchItem i : list) {
//            System.out.println("SONG: " + i.toJsonStringify());
//        }

        Album song = m.getSongDetail("1770442");
        System.out.println("SONG: " + song.toJsonStringify());

    }
}
