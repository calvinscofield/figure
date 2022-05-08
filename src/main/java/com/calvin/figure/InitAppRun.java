package com.calvin.figure;

import com.calvin.figure.service.InitService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitAppRun implements ApplicationRunner {

	@Autowired
	private InitService initService;

	@Value("${version}")
	private String version;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		initService.initUserRole();
	}
}