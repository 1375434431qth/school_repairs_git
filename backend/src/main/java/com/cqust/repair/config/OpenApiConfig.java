package com.cqust.repair.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * @author school-repair
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI repairOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园宿舍报修与维修工单派单系统 API")
                        .description("学生 / 维修人员 / 宿舍管理员三端接口")
                        .version("1.0.0")
                        .contact(new Contact().name("school-repair")));
    }
}
