package com.smartslot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.smartslot.mapper")
public class SmartSlotApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartSlotApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  SmartSlot 智能场地时段预约服务启动成功!");
        System.out.println("  接口文档访问地址: http://localhost:8080/doc.html");
        System.out.println("=================================================");
    }
}
