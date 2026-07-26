package com.dthxhieu.ticket_booking_system_be.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // OTP must be exactly 6 digits - validated at the DTO boundary (same constraint as VerifyOtpRequest).
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmNewPassword;

    // Cross-field validation: confirmNewPassword must equal newPassword.
    // @AssertTrue is the standard Jakarta approach for same-class field comparison.
    // Returning null-safe: if either field is null, the @NotBlank above already fails.
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        if (newPassword == null || confirmNewPassword == null) {
            return true; // let @NotBlank handle null case
        }
        return newPassword.equals(confirmNewPassword);
    }
}
