package com.isteer.vms.dto;

import com.isteer.vms.enums.AuthenticationStatusEnum;
import com.isteer.vms.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticationStatus {
	private AuthenticationStatusEnum status;
	private String token;
	private User user;
	private Object additionalInfo;
}
