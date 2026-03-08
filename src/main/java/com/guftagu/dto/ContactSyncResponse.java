package com.guftagu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactSyncResponse {
    private List<UserDTO> guftaguUsers;
    private List<ContactDTO> inviteContacts;
}
