package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckoutResponseDto {

    private String ldap;
    private String name;
    private String team;

    private LocalDate entryDate;
    private LocalDate exitDate;

    private LocalDateTime entryTimestamp;
    private LocalDateTime exitTimestamp;

    private String lateLoginReason;
    private String lateOrEarlyLogoutReason;
    private String loginComment;

    private Boolean checkinWhileOutsideOffice;
    private Boolean checkoutWhileOutsideOffice;

    private Boolean lateCheckin;
    private String  lateOrEarlyCheckout;

}
