/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.yml;

import java.util.List;

/**
 * Data.
 */
@lombok.Data
@lombok.NoArgsConstructor
public class ArConfig {

    private Config config;

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Config {
        private String id;
        /** environment */
        private List<Environment> environment;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Environment {
        private String id;
        private List<Server> server;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class Server {
        private String id;
        private String url;
    }

}
