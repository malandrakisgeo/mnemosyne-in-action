package com.gmalandrakis.mnemosyne.demo;

import com.gmalandrakis.mnemosyne.spring.MnemosyneSpringConf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(MnemosyneSpringConf.class)
@EnableAspectJAutoProxy
@EnableAutoConfiguration
@ComponentScan("com.gmalandrakis")
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
