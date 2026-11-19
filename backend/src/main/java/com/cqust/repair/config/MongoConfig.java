package com.cqust.repair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * MongoDB 配置：GridFS 图片存储模板。
 *
 * @author school-repair
 */
@Configuration
public class MongoConfig {

    /**
     * GridFS 模板（图片二进制存 MongoDB，MySQL 只存 fileId）。
     */
    @Bean
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory mongoDatabaseFactory, MongoConverter mongoConverter) {
        return new GridFsTemplate(mongoDatabaseFactory, mongoConverter);
    }
}
