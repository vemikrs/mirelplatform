/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterParameter;
import jp.vemi.mirel.apps.mste.domain.service.ReloadStencilMasterService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * ProMarker アプリケーション起動時の初期化処理.<br/>
 * 
 * <p>
 * Spring Boot アプリケーション起動完了時 ({@link ApplicationReadyEvent}) に、
 * ステンシルマスタの自動リロードを実行します。
 * </p>
 * 
 * <h3>設定制御</h3>
 * <ul>
 *   <li>{@code mirel.apps.mste.auto-reload-stencil-on-startup}: 自動リロードの有効/無効を制御</li>
 *   <li>デフォルト: {@code true} (本番環境では {@code false} に設定可能)</li>
 * </ul>
 * 
 * <h3>実行タイミング</h3>
 * <ul>
 *   <li>全てのBean初期化完了後</li>
 *   <li>アプリケーションがリクエスト受付可能になった直後</li>
 *   <li>開発環境では初回 {@code /suggest} API呼び出し前に完了</li>
 * </ul>
 * 
 * @see ApplicationReadyEvent
 * @see ReloadStencilMasterService
 */
@Component
public class ProMarkerStartupListener {

    private static final Logger logger = LoggerFactory.getLogger(ProMarkerStartupListener.class);

    @Autowired
    private ReloadStencilMasterService reloadStencilMasterService;

    /**
     * ステンシルマスタ自動リロード設定フラグ.<br/>
     * 
     * <p>
     * application.yml または環境変数で制御可能:
     * <pre>
     * mirel:
     *   apps:
     *     mste:
     *       auto-reload-stencil-on-startup: true
     * </pre>
     * </p>
     * 
     * <p>
     * 本番環境では {@code false} に設定して手動リロードに切り替え可能。
     * 開発環境では {@code true} (デフォルト) で起動時に自動リロード実行。
     * </p>
     */
    @Value("${mirel.apps.mste.auto-reload-stencil-on-startup:true}")
    private boolean autoReloadOnStartup;

    /**
     * アプリケーション起動完了時のイベントハンドラ.<br/>
     * 
     * <p>
     * {@link ApplicationReadyEvent} は以下の条件で発火:
     * <ul>
     *   <li>全てのSpring Beanの初期化完了</li>
     *   <li>Embedded Tomcat起動完了 (HTTPリクエスト受付可能)</li>
     *   <li>DevToolsの再起動完了後 (開発環境)</li>
     * </ul>
     * </p>
     * 
     * <p>
     * ステンシルマスタリロードを実行して、クラスパス・ファイルシステムから
     * ステンシル定義を収集し、データベースに登録します。
     * </p>
     * 
     * @param event ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!autoReloadOnStartup) {
            logger.info("[ProMarker Startup] Stencil master auto-reload is DISABLED (mirel.apps.mste.auto-reload-stencil-on-startup=false)");
            logger.info("[ProMarker Startup] To reload stencil master, call POST /apps/mste/api/reloadStencilMaster manually");
            return;
        }

        logger.info("=".repeat(80));
        logger.info("[ProMarker Startup] Application ready - Starting stencil master auto-reload...");
        logger.info("=".repeat(80));

        try {
            long startTime = System.currentTimeMillis();

            // ReloadStencilMasterServiceを呼び出してステンシルマスタをリロード
            ApiRequest<ReloadStencilMasterParameter> request = ApiRequest
                .<ReloadStencilMasterParameter>builder()
                .model(ReloadStencilMasterParameter.builder().build())
                .build();

            ApiResponse<?> response = reloadStencilMasterService.invoke(request);

            long elapsedTime = System.currentTimeMillis() - startTime;

            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                logger.error("[ProMarker Startup] Stencil master reload FAILED with errors:");
                response.getErrors().forEach(error -> logger.error("  - {}", error));
                logger.error("[ProMarker Startup] ProMarker may not function correctly. Please check stencil configurations.");
            } else {
                logger.info("=".repeat(80));
                logger.info("[ProMarker Startup] Stencil master reload completed successfully in {}ms", elapsedTime);
                logger.info("[ProMarker Startup] ProMarker is ready to serve /suggest and /generate requests");
                logger.info("=".repeat(80));
            }

        } catch (Exception e) {
            logger.error("=".repeat(80));
            logger.error("[ProMarker Startup] CRITICAL ERROR during stencil master reload:");
            logger.error("[ProMarker Startup] Exception: {}", e.getMessage(), e);
            logger.error("[ProMarker Startup] ProMarker functionality may be severely impacted");
            logger.error("[ProMarker Startup] Recommendation: Fix configuration and restart application");
            logger.error("=".repeat(80));
            
            // エラーがあってもアプリケーション起動は継続
            // 手動でのリロードやトラブルシューティングを可能にする
        }
    }
}
