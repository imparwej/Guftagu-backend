package com.guftagu.controller;

import com.guftagu.dto.ContactSyncRequest;
import com.guftagu.dto.ContactSyncResponse;
import com.guftagu.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@CrossOrigin("*")
@lombok.extern.slf4j.Slf4j
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/sync")
    public ResponseEntity<ContactSyncResponse> syncContacts(@RequestBody ContactSyncRequest request) {
        log.info("Received contact sync request with {} contacts", 
            request.getContacts() != null ? request.getContacts().size() : "null");
        return ResponseEntity.ok(contactService.syncContacts(request.getContacts()));
    }
}
