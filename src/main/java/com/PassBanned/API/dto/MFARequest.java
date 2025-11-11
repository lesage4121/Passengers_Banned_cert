package com.PassBanned.API.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MFARequest {
    private String email;
    private int code;

}
