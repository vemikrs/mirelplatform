/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
/**
 * Mira (mirel Assist) - AI アシスタントアプリケーション.
 * 
 * <p>mirelplatform に統合される AI アシスタントで、以下の機能を提供します:</p>
 * <ul>
 *   <li>汎用チャット型生成 AI</li>
 *   <li>mirelplatform 各アプリケーションのコンテキストヘルプ</li>
 *   <li>mirel Studio / mirel Workflow の Assistant Agent</li>
 * </ul>
 * 
 * <h2>パッケージ構造</h2>
 * <ul>
 *   <li>{@code application/controller} - REST API コントローラ</li>
 *   <li>{@code domain/api} - API インタフェース定義</li>
 *   <li>{@code domain/dao} - エンティティ・リポジトリ</li>
 *   <li>{@code domain/dto} - リクエスト/レスポンス DTO</li>
 *   <li>{@code domain/service} - ビジネスロジック</li>
 *   <li>{@code infrastructure/ai} - AI プロバイダクライアント</li>
 *   <li>{@code infrastructure/config} - 設定クラス</li>
 * </ul>
 * 
 * @see <a href="https://docs.spring.io/spring-ai/reference/">Spring AI Reference</a>
 */
package jp.vemi.mirel.apps.mira;
