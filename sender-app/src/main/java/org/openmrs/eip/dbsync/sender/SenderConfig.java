package org.openmrs.eip.dbsync.sender;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:sender-application.properties")
public class SenderConfig {}
