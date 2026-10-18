package com.example.lostandfound.controller;

import com.example.lostandfound.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final ManagerService managerService;

    // POST /api/admin/managers/{memberId}
    @PostMapping("/managers/{memberId}")
    public ResponseEntity<Void> appointManager(@PathVariable Long memberId) {

        managerService.appoint(memberId);

        return ResponseEntity.noContent().build();
    }

    // DELETE /api/admin/managers/{memberId}
    @DeleteMapping("/managers/{memberId}")
    public ResponseEntity<Void> dismissManager(@PathVariable Long memberId) {
        managerService.dismiss(memberId);

        return ResponseEntity.noContent().build();
    }
}
