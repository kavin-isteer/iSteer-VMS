package com.isteer.vms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.isteer.vms.core.engine.Engine;

@SpringBootApplication
public class ISteerVmsApplication implements CommandLineRunner{

	public static void main(String[] args) {
		SpringApplication.run(ISteerVmsApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Engine.initializeLuceneIndex();
	}
}
