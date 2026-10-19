package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.AdminMemberResponse;
import com.example.lostandfound.entity.Role;
import com.example.lostandfound.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // GET /api/admin/members?keyword=&role=&page=&size=
    @GetMapping("/members")
    public ResponseEntity<Page<AdminMemberResponse>> searchMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)Role role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {

        return ResponseEntity.ok(managerService.searchMembers(keyword, role, pageable));
    }
}
