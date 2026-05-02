package com.smartfreelance.microservice.complaintservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Configuration email SMTP pour l'envoi des notifications par email.
 *
 * Variables à définir dans application.yaml :
 *   mail.host, mail.port, mail.username, mail.password
 */
@Configuration
@EnableAsync
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            org.springframework.core.env.Environment env) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(env.getProperty("spring.mail.host", "smtp.gmail.com"));
        mailSender.setPort(Integer.parseInt(env.getProperty("spring.mail.port", "587")));
        mailSender.setUsername(env.getProperty("spring.mail.username", ""));
        mailSender.setPassword(env.getProperty("spring.mail.password", ""));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        return mailSender;
    }

    /** Exécuteur dédié aux envois d'emails — évite de saturer le pool HTTP */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }
}