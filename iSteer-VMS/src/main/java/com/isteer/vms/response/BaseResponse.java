package com.isteer.vms.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse {

	private int statusCode;
	private String message;
}
