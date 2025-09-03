package com.isteer.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder(toBuilder = true)
public class LoggedInUserDto {
	
	@NotBlank(message = "User Name cannot be blank")
	private String userName;
	
	@NotBlank(message = "User Email cannot be blank")
	@Email(message = "Invalid email format")
	private String userEmail;
	
	

}
