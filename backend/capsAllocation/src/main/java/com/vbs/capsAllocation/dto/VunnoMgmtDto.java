package com.vbs.capsAllocation.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VunnoMgmtDto {
    private String ldap;
    private String name;
    private String role;
    private String email;
    private String programAlignment;
    private String team;
    private String lead;
    private String manager;
    private String shift;
}
