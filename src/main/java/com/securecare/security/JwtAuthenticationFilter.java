package com.securecare.security;

import com.securecare.repository.UserRepository;
import com.securecare.domain.model.UserDocument;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService=jwtService; this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null) {
            String token=header.substring(7);
            try {
                var claims=jwtService.parse(token); String username=claims.getSubject();
                if (jwtService.isValid(token, username)) {
                    UserDocument user=users.findByUsername(username).orElse(null);
                    if (user!=null && user.active()) {
                        var auth=new UsernamePasswordAuthenticationToken(username, null,
                                List.of(new SimpleGrantedAuthority("ROLE_"+user.role().name())));
                        auth.setDetails(user.id()); SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request,response);
    }
}
