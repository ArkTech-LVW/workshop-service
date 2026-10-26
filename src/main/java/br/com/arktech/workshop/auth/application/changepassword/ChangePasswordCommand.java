package br.com.arktech.workshop.auth.application.changepassword;

public record ChangePasswordCommand(String currentPassword, String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordCommand[currentPassword=****, newPassword=****]";
    }
}
