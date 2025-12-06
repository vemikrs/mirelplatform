/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mira エラーコード.
 * 
 * <p>エラー分類に基づくエラーコードを定義します。</p>
 * 
 * <h3>コード体系</h3>
 * <ul>
 *   <li>MIRA-1xxx: API接続エラー</li>
 *   <li>MIRA-2xxx: 認証エラー</li>
 *   <li>MIRA-3xxx: レート制限</li>
 *   <li>MIRA-4xxx: コンテキストエラー</li>
 *   <li>MIRA-5xxx: モデルエラー</li>
 *   <li>MIRA-9xxx: 内部エラー</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum MiraErrorCode {

    // ========================================
    // API接続エラー (1xxx)
    // ========================================
    
    /** AI プロバイダーへの接続失敗 */
    API_CONNECTION_FAILED("MIRA-1001", "AI プロバイダーへの接続に失敗しました", true),
    
    /** タイムアウト */
    API_TIMEOUT("MIRA-1002", "AI プロバイダーからの応答がタイムアウトしました", true),
    
    /** サービス利用不可 */
    API_UNAVAILABLE("MIRA-1003", "AI サービスが一時的に利用できません", true),

    // ========================================
    // 認証エラー (2xxx)
    // ========================================
    
    /** 無効なトークン */
    AUTH_TOKEN_INVALID("MIRA-2001", "API トークンが無効です", false),
    
    /** トークン期限切れ */
    AUTH_TOKEN_EXPIRED("MIRA-2002", "API トークンの有効期限が切れています", false),
    
    /** 権限なし */
    AUTH_PERMISSION_DENIED("MIRA-2003", "API へのアクセス権限がありません", false),

    // ========================================
    // レート制限 (3xxx)
    // ========================================
    
    /** リクエスト制限超過 */
    RATE_LIMIT_EXCEEDED("MIRA-3001", "リクエスト制限を超過しました", true),
    
    /** クォータ超過 */
    QUOTA_EXCEEDED("MIRA-3002", "利用可能なクォータを超過しました", false),

    // ========================================
    // コンテキストエラー (4xxx)
    // ========================================
    
    /** コンテキスト構築失敗 */
    CONTEXT_BUILD_FAILED("MIRA-4001", "コンテキストの構築に失敗しました", false),
    
    /** コンテキストサイズ超過 */
    CONTEXT_TOO_LARGE("MIRA-4002", "コンテキストがトークン制限を超過しています", false),
    
    /** テンプレート未発見 */
    TEMPLATE_NOT_FOUND("MIRA-4003", "プロンプトテンプレートが見つかりません", false),

    // ========================================
    // モデルエラー (5xxx)
    // ========================================
    
    /** 不正な応答 */
    MODEL_RESPONSE_INVALID("MIRA-5001", "AI モデルからの応答が不正です", true),
    
    /** コンテンツフィルタ */
    MODEL_CONTENT_FILTERED("MIRA-5002", "コンテンツフィルタにより応答がブロックされました", false),
    
    /** モデル過負荷 */
    MODEL_OVERLOADED("MIRA-5003", "AI モデルが過負荷状態です", true),

    // ========================================
    // セキュリティエラー (6xxx)
    // ========================================
    
    /** プロンプトインジェクション検出 */
    SECURITY_INJECTION_DETECTED("MIRA-6001", "セキュリティ上の理由により、このリクエストは処理できません", false),
    
    /** 認可拒否 */
    SECURITY_AUTHORIZATION_DENIED("MIRA-6002", "この操作を行う権限がありません", false),
    
    /** クロステナントアクセス */
    SECURITY_CROSS_TENANT("MIRA-6003", "不正なテナントアクセスが検出されました", false),

    // ========================================
    // 内部エラー (9xxx)
    // ========================================
    
    /** 内部エラー */
    INTERNAL_ERROR("MIRA-9001", "内部エラーが発生しました", false),
    
    /** 会話未発見 */
    CONVERSATION_NOT_FOUND("MIRA-9002", "会話が見つかりません", false),
    
    /** メッセージ保存失敗 */
    MESSAGE_SAVE_FAILED("MIRA-9003", "メッセージの保存に失敗しました", false);

    /** エラーコード */
    private final String code;
    
    /** デフォルトメッセージ */
    private final String defaultMessage;
    
    /** リトライ可能フラグ */
    private final boolean retryable;

    /**
     * コードからエラーコードを検索.
     * 
     * @param code エラーコード文字列
     * @return MiraErrorCode（見つからない場合は INTERNAL_ERROR）
     */
    public static MiraErrorCode fromCode(String code) {
        for (MiraErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
