package com.agrovision.dto;

public class AuthResponseDTO {
    private boolean success;
    private String message;
    private Long id;
    private String name;
    private String email;
    private String role;
    private String companyOrFarm;
    private String phone;

    public AuthResponseDTO() {}

    public static AuthResponseDTO success(Long id, String name, String email, String role, String companyOrFarm, String phone, String message) {
        AuthResponseDTO dto = new AuthResponseDTO();
        dto.setSuccess(true);
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(email);
        dto.setRole(role);
        dto.setCompanyOrFarm(companyOrFarm);
        dto.setPhone(phone);
        dto.setMessage(message);
        return dto;
    }

    public static AuthResponseDTO error(String message) {
        AuthResponseDTO dto = new AuthResponseDTO();
        dto.setSuccess(false);
        dto.setMessage(message);
        return dto;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCompanyOrFarm() { return companyOrFarm; }
    public void setCompanyOrFarm(String companyOrFarm) { this.companyOrFarm = companyOrFarm; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
