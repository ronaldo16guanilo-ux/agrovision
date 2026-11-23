package com.agrovision.dto;

public class LoginRequestDTO {
    private String email;
    private String password;
    private String roleType; // "WORKER" (Admin / Trabajador) or "CLIENT" (Cliente)

    public LoginRequestDTO() {}

    public LoginRequestDTO(String email, String password, String roleType) {
        this.email = email;
        this.password = password;
        this.roleType = roleType;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
}
