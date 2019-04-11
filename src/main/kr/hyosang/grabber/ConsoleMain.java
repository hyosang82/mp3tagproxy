package kr.hyosang.grabber;

public class ConsoleMain {
    public static final void main(String[] args) {
        Melon m = new Melon();
        //ArrayList<AlbumSearchItem> result = m.searchAlbum("서태지");
        //System.out.println("Search: " + result);
        Album a = m.getAlbumDetail("3736");
        System.out.println("Album: " + a.toJsonStringify());

    }
}
