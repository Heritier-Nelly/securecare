package com.securecare.controller;

import com.securecare.domain.enums.UserRole;
import com.securecare.service.AuthService;
import com.securecare.service.LoginAttemptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private final AuthService auth; private final LoginAttemptService limiter;
    public AuthController(AuthService auth,LoginAttemptService limiter){this.auth=auth;this.limiter=limiter;}
    public record LoginRequest(@NotBlank String username,@NotBlank String password){}
    public record RegisterRequest(@NotBlank String username,@NotBlank String password,@Email String email,@NotBlank String role){}
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest body,HttpServletRequest request){
        String ip=request.getRemoteAddr(); if(!limiter.allow(ip)) throw new LoginRateLimitException();
        try { Map<String,Object> result=auth.login(body.username(),body.password(),request); limiter.reset(ip); return result; }
        catch(IllegalArgumentException ex){limiter.recordFailure(ip);log.warn("Failed login username={} ip={}",body.username(),ip);throw ex;}
    }
    @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest body){
        try {
            UserRole role = UserRole.valueOf(body.role().toUpperCase());
            return auth.register(body.username(), body.password(), body.email(), role);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role or registration data: " + ex.getMessage());
        }
    }
    @PostMapping("/forgot-password") public Map<String,Object> forgotPassword(@RequestBody Map<String,String> body){
        auth.requestPasswordReset(body.get("email"));
        return Map.of("message", "If an account exists for that email, a reset link has been generated.");
    }
    @PostMapping("/reset-password") public Map<String,Object> resetPassword(@RequestBody Map<String,String> body){
        auth.resetPassword(body.get("token"), body.get("newPassword"));
        return Map.of("message", "Password reset successfully. You can now sign in.");
    }
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS) static class LoginRateLimitException extends RuntimeException { LoginRateLimitException(){super("Too many login attempts. Try again in one minute.");} }
}
