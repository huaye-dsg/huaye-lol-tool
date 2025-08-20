package com.example.huayeloltool.common;

import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
public class AudioPlayer {


    public static void findGame() {
        player("已找到对局.mp3");
    }

    public static void championSelectStart() {
        player("已经进入英雄选择界面.mp3");
    }

    public static void inputLobby() {
        player("进入大厅.mp3");
    }

    private static void player(String name) {
        new Thread(() -> {
            try {
                ClassPathResource resource = new ClassPathResource("audio/" + name);
                InputStream inputStream = resource.getInputStream();
                Player player = new Player(inputStream);
                player.play();
            } catch (Exception e) {
                log.error("播放失败", e);
            }
        }).start();

    }


}
