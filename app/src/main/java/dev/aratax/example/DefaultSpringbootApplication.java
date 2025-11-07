package dev.aratax.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class DefaultSpringbootApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DefaultSpringbootApplication.class);
		app.setMainApplicationClass(DefaultSpringbootApplication.class);
		app.run(args);
	}

}
