/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.yml;

import java.util.List;
import java.util.Map;

/**
 * Data.
 */
@lombok.Data
@lombok.NoArgsConstructor
public class ArData {

    private DataTemplate dataTemplate;

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class DataTemplate {
        private String id;
        private List<ValiableItems> variable;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class ValiableItems {
        private String id;
        private String name;
        private String type;
        private String defaultValue;
        private List<Map<String, Object>> defaultValueVariation;
    }
}
