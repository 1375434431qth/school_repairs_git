package com.cqust.repair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
}
