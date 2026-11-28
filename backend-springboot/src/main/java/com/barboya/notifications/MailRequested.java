package com.barboya.notifications;

public record MailRequested(String to, String subject, String body) {

    @Override
    public String toString() {
        return "MailRequested[subject=" + subject + "]";
    }
}
