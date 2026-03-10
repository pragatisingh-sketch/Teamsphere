package com.vbs.capsAllocation.dto;

import java.util.List;

public class SendNotificationRequest {
    private List<String> recipientEmails;

    public List<String> getRecipientEmails() {
        return recipientEmails;
    }

    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }
}
