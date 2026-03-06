package com.guftagu.controller;

import com.guftagu.dto.ContactSyncRequest;
import com.guftagu.dto.UserDTO;
import com.guftagu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<List<UserDTO>> syncContacts(@RequestBody ContactSyncRequest request) {
        return ResponseEntity.ok(userService.syncContacts(request.getPhoneNumbers()));
    }
}
