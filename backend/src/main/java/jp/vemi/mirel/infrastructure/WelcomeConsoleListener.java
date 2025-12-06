/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Welcome Console Logger.
 * 
 * <p>
 * Spring Boot アプリケーション起動完了時 ({@link ApplicationReadyEvent}) に、
 * mirelplatform ロゴ入りの Welcome Console を出力します。
 * </p>
 * 
 * <h3>出力内容</h3>
 * <ul>
 *   <li>mirelplatform ASCII アートロゴ</li>
 *   <li>アプリケーション名とバージョン</li>
 *   <li>実際のバインドポート</li>
 *   <li>アクティブプロファイル</li>
 *   <li>API ベースパス</li>
 *   <li>E2E テスト用 READY トークン</li>
 * </ul>
 * 
 * @see ApplicationReadyEvent
 */
@Component
public class WelcomeConsoleListener {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeConsoleListener.class);

    private final Environment environment;

    @Value("${app.name:mirelplatform}")
    private String appName;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String appVersion;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${server.port:3000}")
    private String serverPort;

    public WelcomeConsoleListener(Environment environment) {
        this.environment = environment;
    }

    /**
     * アプリケーション起動完了時のイベントハンドラ.
     * 
     * <p>
     * {@link ApplicationReadyEvent} 発火時に Welcome Console を出力します。
     * </p>
     * 
     * @param event ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        String[] activeProfiles = environment.getActiveProfiles();
        String profilesStr = activeProfiles.length > 0 
            ? String.join(", ", activeProfiles) 
            : "default";

        String welcomeMessage = buildWelcomeMessage(profilesStr);
        
        // ログ出力
        logger.info("\n" + welcomeMessage);
        
        // E2E テスト用 READY トークン
        logger.info("READY");
    }

    /**
     * Welcome メッセージを構築.
     * 
     * @param profiles アクティブプロファイル
     * @return Welcome メッセージ
     */
    private String buildWelcomeMessage(String profiles) {
        String asciiArt = """
            ############################################################
            #            _          _
            #  _ __ ___ (_)_ __ ___| |
            # | '_ ` _ \\| | '__/ _ \\ |
            # | | | | | | | | |  __/ |
            # |_| |_| |_|_|_|  \\___|_|
            #
            #        _       _    __
            #  _ __ | | __ _| |_ / _| ___  _ __ _ __ ___
            # | '_ \\| |/ _` | __| |_ / _ \\| '__| '_ ` _ \\
            # | |_) | | (_| | |_|  _| (_) | |  | | | | | |
            # | .__/|_|\\__,_|\\__|_|  \\___/|_|  |_| |_| |_|
            # |_|
            ############################################################
            """;

        StringBuilder sb = new StringBuilder();
        sb.append(asciiArt);
        sb.append("\n");
        sb.append("#  Application:    ").append(appName).append("\n");
        sb.append("#  Version:        ").append(appVersion).append("\n");
        sb.append("#  Port:           ").append(serverPort).append("\n");
        sb.append("#  Profile(s):     ").append(profiles).append("\n");
        sb.append("#  API Base Path:  ").append(contextPath).append("\n");
        sb.append("#\n");
        sb.append("#  Status:         🚀 READY\n");
        sb.append("############################################################\n");

        return sb.toString();
    }
}
