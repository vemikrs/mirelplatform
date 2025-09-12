/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.main;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jp.vemi.extension.function_resolver.exception.CreateTemplateException;

public class TemplateParser {

    private TemplateParser() {
        // Constructor is not used.
    }

    /**
     * パース.<br/>
     * 
     * @param target
     *            対象文字列
     * @param binds
     *            バインド
     * @return パースされた文字列
     */
    public static String parse(String target, Map<String, Object> binds) {

        for (Map.Entry<String, Object> entry : binds.entrySet()) {
            if (null == entry.getValue()) {
                entry.setValue("");
            }
        }

        Template template = newTemplate(target);
        String output;
        try(Writer out = new StringWriter()) {
            template.process(binds, out);
            output = out.toString();
        } catch (TemplateException | IOException e) {
            throw new CreateTemplateException(e);
        }

        return output;

    }

    /**
     * SimpleTemplateで初期化したテンプレートを返します。.<br/>
     * 
     * @param target
     *            対象文字列
     * @return テンプレート
     */
    protected static Template newTemplate(String target) {
            Configuration configration = new Configuration(Configuration.VERSION_2_3_29);
            StringTemplateLoader loader = new StringTemplateLoader();
            loader.putTemplate("once", target);
            try {
                configration.setTemplateLoader(loader);
                return configration.getTemplate("once");
            } catch (IOException e) {
                throw new CreateTemplateException(e);
            }
    }
}
