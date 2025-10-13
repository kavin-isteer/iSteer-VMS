package com.isteer.vms.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AuthenticationStatusEnum {
	AUTHENTICATION_SUCCESS(1),
	BAD_CREDENTIALS(-1)
	;
	
	private int statusCode;
}
