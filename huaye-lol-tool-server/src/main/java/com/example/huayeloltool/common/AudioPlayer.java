package com.example.huayeloltool.common;

import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class AudioPlayer {

    @Resource(name = "commonAsyncExecutor")
    private ExecutorService audioExecutor;

    // 静态实例，用于静态方法调用
    private static AudioPlayer instance;

    // 构造函数中设置静态实例
    public AudioPlayer() {
        instance = this;
    }

    public static void findGame() {
        if (instance != null) {
            instance.player("已找到对局.mp3");
        }
    }

    public static void championSelectStart() {
        if (instance != null) {
            instance.player("已经进入英雄选择界面.mp3");
        }
    }

    public static void inputLobby() {
        if (instance != null) {
            instance.player("进入大厅.mp3");
        }
    }

    private void player(String name) {
        if (audioExecutor != null) {
            audioExecutor.submit(() -> {
                try {
                    ClassPathResource resource = new ClassPathResource("audio/" + name);
                    InputStream inputStream = resource.getInputStream();
                    Player player = new Player(inputStream);
                    player.play();
                } catch (Exception e) {
                    log.error("播放失败", e);
                }
            });
        } else {
            // 降级方案：如果线程池未初始化，使用直接创建线程的方式
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
}