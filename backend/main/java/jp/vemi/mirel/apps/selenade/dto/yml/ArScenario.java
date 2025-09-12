/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.yml;

import java.util.List;
import java.util.Map;

/**
 * Scenario.<br/>
 */
@lombok.Data
@lombok.NoArgsConstructor
public class ArScenario {

    private Scenario scenario;
    private Data data;

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Scenario {
        private String id;
        private String name;
        private String note;
        private List<Usecase> usecase;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Data {
        private String id;
        private List<DataVariable> variable;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Usecase {
        private String id;
        private String name;
        private Integer sort;
        private String usecaseId;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class DataVariable {
        private String id;
        private String name;
        private String type;
        private String defaultValue;
        private List<Map<String, Object>> valuePattern;
    }
}
