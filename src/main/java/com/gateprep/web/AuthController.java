package com.gateprep.web;

import com.gateprep.model.User;
import com.gateprep.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record RegisterRequest(String name, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String dummyHash;
    private final Set<String> adminEmails = new HashSet<>();

    public AuthController(UserRepository users, PasswordEncoder encoder, @Value("${app.admin-emails:}") String adminList) {
        this.users = users;
        this.encoder = encoder;
        for (String e : adminList.split(",")) if (!e.isBlank()) adminEmails.add(e.trim().toLowerCase(Locale.ROOT));
        this.dummyHash = encoder.encode("not-a-real-password"); // keeps login timing similar for unknown emails
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> view(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("name", u.name);
        m.put("email", u.email);
        m.put("role", u.role);
        return m;
    }

    /** Starts a fresh session for the user. Changing the session id prevents session fixation. */
    private static void startSession(HttpServletRequest request, User u) {
        if (request.getSession(false) != null) request.changeSessionId();
        request.getSession(true).setAttribute(AuthInterceptor.USER_ID, u.id);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest body, HttpServletRequest request) {
        String name = body.name() == null ? "" : body.name().trim();
        String email = normalize(body.email());
        String password = body.password() == null ? "" : body.password();
        if (name.isEmpty() || name.length() > 60)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter your name (up to 60 characters)");
        if (email.length() > 120 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        if (password.length() < 8 || password.length() > 64)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be 8 to 64 characters");
        if (users.existsByEmail(email))
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");

        User u = new User();
        u.name = name;
        u.email = email;
        u.passwordHash = encoder.encode(password);
        // The first account becomes the administrator, and so does any email listed in app.admin-emails.
        u.role = users.count() == 0 || adminEmails.contains(email) ? "ADMIN" : "USER";
        u.createdAt = Instant.now();
        try {
            users.save(u);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        startSession(request, u);
        return ResponseEntity.status(HttpStatus.CREATED).body(view(u));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        String password = body.password() == null ? "" : body.password();
        User u = users.findByEmail(normalize(body.email())).orElse(null);
        boolean ok = encoder.matches(password, u == null ? dummyHash : u.passwordHash) && u != null;
        if (!ok) throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        if (adminEmails.contains(u.email) && !"ADMIN".equals(u.role)) {
            u.role = "ADMIN";
            users.save(u);
        }
        startSession(request, u);
        return view(u);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(AuthInterceptor.USER_ID);
        User u = id == null ? null : users.findById((Long) id).orElse(null);
        if (u == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Not logged in");
        return view(u);
    }
}
