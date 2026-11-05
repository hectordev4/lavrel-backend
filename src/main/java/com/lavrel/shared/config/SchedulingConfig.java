package com.lavrel.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the automatic library sync). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
