package jp.vemi.mirel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "mipla2.security.api")
@Getter
@Setter
public class Mipla2SecurityProperties {

    /**
     * API認証の有効/無効
     */
    private boolean enabled = true;
    /**
     * CSRF保護の有効/無効
     * デフォルトはtrue（本番環境推奨）
     * 開発環境でのみapplication-dev.ymlでfalseに設定可能
     */
    private boolean csrfEnabled = true;
}