package com.auto.test.platform.config;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * allure报告配置类
 */
@Slf4j
@Configuration
public class AllureConfig {

    @Value("${allure.results.directory:./target/allure-results}")
    private String resultDirectory;

    @Bean
    public AllureLifecycle allureLifecycle() {
        try {
            Path path = Paths.get(resultDirectory);
            if (!Files.exists(path)){
                Files.createDirectories(path);
            }

            FileSystemResultsWriter writer = new FileSystemResultsWriter(path);
            AllureLifecycle lifecycle = new AllureLifecycle(writer);

            log.info("Allure生命周期初始化完整，结果目录：{}", resultDirectory);
            return lifecycle;
        } catch (Exception e) {
            log.error("初始化Allure生命周期失败",e);
            throw new RuntimeException("初始化Allure失败",e);
        }
    }
}
