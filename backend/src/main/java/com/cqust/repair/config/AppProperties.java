package com.cqust.repair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务配置绑定类，与 application.yml 的 {@code repair:} 段一一对应。
 *
 * @author school-repair
 */
@Data
@Component
@ConfigurationProperties(prefix = "repair")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Order order = new Order();
    private Notice notice = new Notice();
    private Mq mq = new Mq();
    private Async async = new Async();
    private Ai ai = new Ai();

    @Data
    public static class Jwt {
        private String secret;
        private long expireSeconds = 7200;
        /** refresh token 有效期（秒），默认 7 天 */
        private long refreshExpireSeconds = 604800;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Upload {
        private int maxCountPerType = 6;
        private int maxCountPerOrder = 12;
        private String[] allowExt = {"jpg", "jpeg", "png", "webp"};
        private long maxSize = 5 * 1024 * 1024;
    }

    @Data
    public static class Order {
        private String noPrefix = "WR";
        private int autoConfirmDays = 7;
        private int autoCloseDays = 7;
        private Map<Integer, Integer> priorityHours = new LinkedHashMap<>();
    }

    @Data
    public static class Notice {
        private Ws websocket = new Ws();

        @Data
        public static class Ws {
            private boolean enabled = true;
            private int heartbeatSeconds = 30;
            private int maxSessionsPerUser = 3;
        }
    }

    @Data
    public static class Mq {
        private boolean enabled = false;
    }

    @Data
    public static class Async {
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 200;
        private int keepAliveSeconds = 60;
    }

    @Data
    public static class Ai {
        private boolean enabled = true;
        private boolean failFastOnMissingKey = true;
        private String model = "deepseek-chat";
        private int maxInputChars = 500;
        private int maxHistoryTurns = 10;
        private int historyTtlMinutes = 30;
        private int timeoutSeconds = 90;
        /** 同时进行的 AI 对话上限，防止上游并发与线程资源耗尽 */
        private int maxConcurrent = 20;
        private Knowledge knowledge = new Knowledge();
        private RateLimit rateLimit = new RateLimit();
        /** 角色 → 允许跳转的静态路由 */
        private Map<String, List<String>> routeWhitelist = new LinkedHashMap<>();

        @Data
        public static class Knowledge {
            private int topK = 4;
            private int maxChars = 4000;
            private double minScore = 1.0;
        }

        @Data
        public static class RateLimit {
            private int perMinute = 10;
            private int perDay = 200;
        }
    }
}
