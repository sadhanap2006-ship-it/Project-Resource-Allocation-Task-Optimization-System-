package com.sist.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Project Resource Allocation System.
 *
 * Running this class starts an embedded Tomcat server on port 8080,
 * connects to MySQL and publishes every REST API in the controller package.
 */
@SpringBootApplication
public class ResourceAllocationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceAllocationApplication.class, args);
    }
}
