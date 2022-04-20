package kr.hyosang.mp3tagproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter @Setter
public class Track {
    private String title;
    private String artist;
}
