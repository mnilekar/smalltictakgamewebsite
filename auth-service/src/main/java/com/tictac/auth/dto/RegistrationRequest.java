package com.tictac.auth.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class RegistrationRequest {

    @NotBlank private String firstName;
    @NotBlank private String lastName;

    @NotNull private LocalDate birthDate;

    @NotBlank private String nationality;

    @Email @NotBlank private String email;

    @NotBlank private String mobile;

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    // ≥8 chars, ≥1 upper, ≥1 lower, ≥1 special
    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8,}$",
            message = "Password must be ≥8 chars and include upper, lower, and special character"
    )
    private String password;

    // getters/setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}