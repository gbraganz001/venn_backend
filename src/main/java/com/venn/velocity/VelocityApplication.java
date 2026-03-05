package com.venn.velocity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class VelocityApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(VelocityApplication.class, args);

        if ("true".equalsIgnoreCase(ctx.getEnvironment().getProperty("app.exit-after-run"))) {
            int code = SpringApplication.exit(ctx);
            System.exit(code);
        }

    }

}
