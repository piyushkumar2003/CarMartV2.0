package com.airline.user.dto;

/**
 * Authentication response DTO containing JWT token and user info.
 */
public class AuthResponse {

    private String token;
    private String username;
    private String role;
    private String message;
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, String role, String message, long expiresIn) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.message = message;
        this.expiresIn = expiresIn;
    }

    // Static factory methods for convenience
    public static AuthResponse success(String token, String username, String role, long expiresIn) {
        return new AuthResponse(token, username, role, "Authentication successful", expiresIn);
    }

    public static AuthResponse error(String message) {
        AuthResponse response = new AuthResponse();
        response.setMessage(message);
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
