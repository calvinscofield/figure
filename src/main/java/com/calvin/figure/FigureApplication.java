package com.calvin.figure;

import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;

import com.calvin.figure.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class FigureApplication {

	public static final long SERIAL_VERSION_UID = 520L;

	public static void main(String[] args) {
		SpringApplication.run(FigureApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskExecutor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// 设置线程的名称前缀
		executor.setThreadNamePrefix("异步线程");
		// 线程池维护线程的最少数量
		executor.setCorePoolSize(5);
		// 线程池维护线程的最大数量
		executor.setMaxPoolSize(10);
		// 非核心线程数的存活时间
		executor.setKeepAliveSeconds(4);
		// 阻塞队列LinkedBlockingQueue
		executor.setQueueCapacity(25);
		// 对拒绝任务的处理策略
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		// 初始化
		executor.initialize();
		return executor;
	}

	@Bean
	public CalInterceptor calInterceptor() {
		return new CalInterceptor();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedMethods("*")
						.allowedOriginPatterns("*")
						.allowCredentials(true);
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(calInterceptor())
						.addPathPatterns("/**")
						.excludePathPatterns("/users/login", "/users/logout", "/error", "/users/register",
								"/users/emailLoginCode", "/users/emailRegisterCode", "/users/exist");
			}
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuditorAware<User> auditorProvider() {
		return new CalAuditorAware();
	}

	@Bean
	public CalUtility calUtility() {
		return new CalUtility();
	}

	@Bean
	public JPAQueryFactory jPAQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}
}
