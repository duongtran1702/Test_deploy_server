package com.production.payment.controller;

import com.production.payment.model.ChaosConfig;
import com.production.payment.service.ChaosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
@Tag(name = "Chaos Engineering Controller", description = "Simulate latency, failures, gateway outages, and force DLT routing")
public class ChaosController {

    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get current Chaos Engineering simulation settings")
    public ResponseEntity<ChaosConfig> getChaosConfig() {
        return ResponseEntity.ok(chaosService.getConfig());
    }

    @PostMapping("/config")
    @Operation(summary = "Update Chaos Engineering simulation settings")
    public ResponseEntity<Map<String, Object>> updateChaosConfig(@RequestBody ChaosConfig newConfig) {
        chaosService.updateConfig(newConfig);
        return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "config", chaosService.getConfig(),
                "message", "Chaos configuration applied successfully"
        ));
    }
}
