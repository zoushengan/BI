package com.example.oj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 主类（项目启动入口）
 */
@SpringBootApplication
@MapperScan("com.example.oj.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class OjApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjApplication.class, args);
    }

}
