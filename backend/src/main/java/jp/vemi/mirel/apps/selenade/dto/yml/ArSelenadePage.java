/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.yml;

import java.util.List;

@lombok.Data
public class ArSelenadePage {

    private Page page;

    @lombok.Data
    public static class Page {
        private String id;
        private String name;
        private String appId;
        private String contextPath;
        private String note;
        private List<Action> action;
    }

    @lombok.Data
    public static class Action {
        private String id;
        private String name;
        private String type;
        private String value;
        private String locator;
        private Boolean ignoreIfNotFound;
        private Boolean saveScreen = true;
        private List<String> includeTag;
    }
}
