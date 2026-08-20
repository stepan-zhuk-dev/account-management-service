package ua.stepan.zhuk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan({"ua.stepan.zhuk.account", "ua.stepan.zhuk.outbox"})
@SpringBootApplication
public class AccountManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountManagementServiceApplication.class, args);
    }

}
