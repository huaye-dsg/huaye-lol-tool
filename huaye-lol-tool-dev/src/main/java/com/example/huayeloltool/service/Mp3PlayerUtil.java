package com.example.huayeloltool.service;

import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.i18n.qual.LocalizableKeyBottom;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class Mp3PlayerUtil {


    public static void findGame() {
        player("C:\\Users\\77646\\Desktop\\音频资源\\已找到对局.mp3");
    }

    public static void championSelectStart() {
        player("C:\\Users\\77646\\Desktop\\音频资源\\已经进入英雄选择界面.mp3");
    }


    public static void inputLobby() {
        player("/audio/findGame.mp3");
    }

    private static void player(String path) {
        new Thread(() -> {
            try (InputStream inputStream = new FileInputStream(path)) {
                Player player = new Player(inputStream);
                player.play();
            } catch (Exception e) {
                log.error("音频播放失败！", e);
            }
        }).start();

    }




}
