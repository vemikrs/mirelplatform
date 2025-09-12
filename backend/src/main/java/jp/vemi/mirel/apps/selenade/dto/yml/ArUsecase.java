/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.yml;

import java.util.List;
import java.util.Map;

/**
 * Usecase.
 */
@lombok.Data
@lombok.NoArgsConstructor
public class ArUsecase {

    private Usecase usecase;
    private List<UsecaseGroup> usecaseGroup;

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Usecase {
        private String id;
        private String name;
        private String note;
        private List<Step> step = new java.util.ArrayList<>();
    }

    /**
     * Operation.<br/>
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Step {
        private String id;
        private String name;
        private String note;
        private Plugin plugin;

        public boolean isSelenade() {
            if (null == this.plugin) {
                return false;
            }

            if (null == this.plugin.selenade) {
                return false;
            }

            return true;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class UsecaseGroup {
        private String id;
        private String name;
        private List<Usecase> usecase;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Plugin {
        private Selenade selenade;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Selenade {
        private String pageId;
        private List<Action> action;
    }

    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = true)
    @lombok.NoArgsConstructor
    public static class Action extends ArSelenadePage.Action {
        private String api;
        private String actionTemplate;
        private Boolean isSaveScreen;
        private Map<String, Object> parameter;
    }
}
