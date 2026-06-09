// src/main/java/com/cloudcost/cloud_cost_optimizer/controller/TestController.java
package com.cloudcost.cloud_cost_optimizer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    
    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Server is running!");
        response.put("message", "Cloud Cost Optimizer API is ready");
        return response;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", new java.util.Date().toString());
        return response;
    }
}