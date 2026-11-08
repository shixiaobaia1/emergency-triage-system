package org.example.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类。
 *
 * 启动后：
 *   - 默认端口 8080
 *   - 折半查找接口前缀 /api/binary-search
 *   - 哈希表接口前缀 /api/hashtable
 */
@SpringBootApplication
public class QueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueApplication.class, args);
    }
}
