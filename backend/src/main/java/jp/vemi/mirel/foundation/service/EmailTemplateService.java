/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

/**
 * メールテンプレートサービス.
 * FreeMarkerテンプレートエンジンを使用したHTMLメール生成
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {
    
    private final Configuration freemarkerConfig;
    
    /**
     * テンプレートからHTMLを生成
     * 
     * @param templateName テンプレート名 (拡張子なし、例: "otp-login")
     * @param variables テンプレート変数
     * @return 生成されたHTML
     */
    public String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Template template = freemarkerConfig.getTemplate("email/" + templateName + ".ftl");
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, variables);
        } catch (Exception e) {
            log.error("テンプレート処理失敗: template={}, error={}", templateName, e.getMessage(), e);
            throw new RuntimeException("メールテンプレート処理エラー: " + templateName, e);
        }
    }
}
