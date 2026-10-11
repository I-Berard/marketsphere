package rw.ac.rca.cloudsphere.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.cloudsphere.common.exception.ApiException;
import rw.ac.rca.cloudsphere.security.AuthPrincipal;
import rw.ac.rca.cloudsphere.security.TokenBlacklist;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "UC-CS-03 Register · UC-CS-04 Login")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;
    private final TokenBlacklist blacklist;

    public AuthController(AuthService authService, UserRepository users, TokenBlacklist blacklist) {
        this.authService = authService;
        this.users = users;
        this.blacklist = blacklist;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Create a customer account and sign in")
    public TokenResponse register(@Valid @RequestBody RegisterRequest body,
                                  @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return authService.register(body, cartToken);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Sign in. Errors are generic (no user enumeration).")
    public TokenResponse login(@Valid @RequestBody LoginRequest body,
                               @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return authService.login(body, cartToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current access token")
    public Map<String, String> logout(HttpServletRequest request) {
        Object jti = request.getAttribute("jwtJti");
        Object exp = request.getAttribute("jwtExp");
        if (jti instanceof String id && exp instanceof Instant expiresAt) {
            blacklist.revoke(id, expiresAt);
        }
        return Map.of("status", "signed-out");
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return users.findById(principal.userId())
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
