package com.application.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskReminderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskReminderApplication.class, args);
    }
}
