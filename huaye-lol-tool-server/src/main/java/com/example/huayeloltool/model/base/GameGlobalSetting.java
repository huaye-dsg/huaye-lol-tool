package com.example.huayeloltool.model.base;

import com.example.huayeloltool.enums.Heros;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 游戏全局配置
 * 
 * <p>由 Spring 容器管理的全局配置类，默认为单例模式。
 * 配置在应用运行期间保持不变，跨游戏会话保持用户设置。
 * 
 * <p>使用 Spring 管理的优势：
 * - 统一的依赖注入模式，避免混合手写单例和 Spring 管理
 * - 更好的测试支持，可以轻松 mock 和替换
 * - 配置集中管理，通过 @Configuration 类统一配置
 * - 生命周期由 Spring 容器管理，代码更简洁
 */
@Data
@Component
public class GameGlobalSetting {

    /**
     * 是否自动接受对局
     */
    private Boolean autoAcceptGame = true;

    /**
     * 自动选择的英雄ID
     */
    private Integer autoPickChampID = 0;

    /**
     * 是否自动选择英雄
     */
    private Boolean autoPickChamp = false;

    /**
     * 自动禁用的英雄ID
     */
    private Integer autoBanChampID = Heros.GRAVES.getHeroId();

    /**
     * 是否自动禁英雄
     */
    private Boolean autoBanChamp = true;
}
