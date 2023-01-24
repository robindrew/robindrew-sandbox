package com.robindrew.sandbox;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

import com.robindrew.common.web.Bootstrap;
import com.robindrew.spring.AbstractSpringService;
import com.robindrew.spring.component.indexlink.IndexLinkMap;

@SpringBootApplication
@ComponentScan(basePackages = "com.robindrew.sandbox.component")
@ServletComponentScan(basePackages = "com.robindrew.sandbox.servlet")
public class SandboxService extends AbstractSpringService {

	public static void main(String[] args) {
		run(SandboxService.class, args);
	}

	@Autowired
	private IndexLinkMap linkMap;

	@PostConstruct
	public void registerLinks() {
		linkMap.add("Google Login", "/GoogleLogin", Bootstrap.COLOR_DEFAULT);
	}

}
