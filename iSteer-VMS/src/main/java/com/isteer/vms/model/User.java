package com.isteer.vms.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class User {
	private int id;
	private String username;
	private String uuid;
	private String email;
	private String password;
	private int roleId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String createdByUuid;
}
