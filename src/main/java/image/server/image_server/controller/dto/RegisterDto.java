package image.server.image_server.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterDto {
    @NotBlank
    public String username;

    @NotBlank
    public String password;

    @Email
    public String email;

    // constructors
    public RegisterDto() {}
    public RegisterDto(String username, String password, String email){
        this.username = username; this.password = password; this.email = email;
    }

    // getters / setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
