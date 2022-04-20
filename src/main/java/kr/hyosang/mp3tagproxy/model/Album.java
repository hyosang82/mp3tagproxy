package kr.hyosang.mp3tagproxy.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Album {
    public String idx;
    public String title;
    public String artist;
    public String coverArt;
    public String releaseDate;
    public String genre;
    public String publisher;
    public String agency;

    private List<Track> tracks = new ArrayList<>();

    public void addTrack(String title, String artist) {
        tracks.add(new Track(title, artist));
    }
}
