package com.vbs.capsAllocation.dto;

import java.time.LocalDateTime;

public record CheckInStatusResponse(
                String status,
                boolean checkedIn,
                String checkInTime,
                boolean isLate,

                String checkedOutStatus,
                boolean checkedOut,
                String checkOutTime,
                String earlyOrLateCheckOut,
                LocalDateTime checkinDateTime,
                LocalDateTime checkOutDateTime,
                Boolean isCheckInOutsideOffice,
                Boolean isCheckOutOutsideOffice) {
}