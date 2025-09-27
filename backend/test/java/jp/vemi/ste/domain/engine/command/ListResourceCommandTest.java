package jp.vemi.ste.domain.engine.command;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;

import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import jp.vemi.ste.domain.EngineBinds;

public class ListResourceCommandTest {

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    @Test
    void testParse() {

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        try {
            // TODO: Use proper test resource path instead of hardcoded path
            cfg.setClassForTemplateLoading(this.getClass(), "/test-templates/");
        } catch (Exception e1) {
            e1.printStackTrace();
            fail();
            return;
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(true);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);

        Template template;
        try {
            template = cfg.getTemplate("common/_constClassName_.java.ftl");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
            return;
        }
        try {
            template.process(getBinds(), new OutputStreamWriter(System.out));
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            fail();
            return;
        }

        logger.info("");

    }

    protected EngineBinds getBinds() {
        EngineBinds binds = new EngineBinds();
        binds.put("copyright", "TATATA");
        List<EngineBinds> smscdgroupSet = Lists.newArrayList();
        {
            EngineBinds item = new EngineBinds();
            item.put("cdgroupCategory", "CTGR1");
            item.put("cdgroupName", "CDGRNAME1");
            item.put("cdgroupConst", "CONST1");
            item.put("cdgroupCd", "CDGRCD1");
            smscdgroupSet.add(item);
        }
        {
            EngineBinds item = new EngineBinds();
            item.put("cdgroupCategory", "CTGR2");
            item.put("cdgroupName", "CDGRNAME2");
            item.put("cdgroupConst", "CONST2");
            item.put("cdgroupCd", "CDGRCD2");
            smscdgroupSet.add(item);
        }
        binds.put("smscdgroupSet", smscdgroupSet);

        return binds;
    }

}