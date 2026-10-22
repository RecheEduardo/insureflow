package br.com.eduadoreche.insureflow;

import org.springframework.boot.SpringApplication;

public class TestInsureflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(InsureflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
