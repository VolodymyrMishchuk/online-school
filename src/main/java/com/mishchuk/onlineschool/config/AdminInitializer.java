package com.mishchuk.onlineschool.config;

import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final PersonRepository personRepository;

    public AdminInitializer(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "pidsercem@gmail.com";
        personRepository.findByEmail(adminEmail).ifPresent(user -> {
            boolean changed = false;
            if (user.getRole() == null
                    || user.getRole() != PersonRole.ADMIN) {
                user.setRole(PersonRole.ADMIN);
                changed = true;
            }
            if (user.getStatus() == null) {
                user.setStatus(PersonStatus.ACTIVE);
                changed = true;
            }
            if (changed) {
                personRepository.save(user);
                System.out.println("Restored ADMIN role and ACTIVE status for user: " + adminEmail);
            }
        });
    }
}
