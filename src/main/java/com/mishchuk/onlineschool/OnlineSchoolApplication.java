package com.mishchuk.onlineschool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@EnableAsync
public class OnlineSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineSchoolApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    org.springframework.boot.CommandLineRunner run(
            com.mishchuk.onlineschool.repository.PersonRepository personRepository) {
        return args -> {
            String adminEmail = "pidsercem@gmail.com";
            personRepository.findByEmail(adminEmail).ifPresent(user -> {
                boolean changed = false;
                if (user.getRole() == null
                        || user.getRole() != com.mishchuk.onlineschool.repository.entity.PersonRole.ADMIN) {
                    user.setRole(com.mishchuk.onlineschool.repository.entity.PersonRole.ADMIN);
                    changed = true;
                }
                if (user.getStatus() == null) {
                    user.setStatus(com.mishchuk.onlineschool.repository.entity.PersonStatus.ACTIVE);
                    changed = true;
                }
                if (changed) {
                    personRepository.save(user);
                    System.out.println("Restored ADMIN role and ACTIVE status for user: " + adminEmail);
                }
            });
        };
    }

}
