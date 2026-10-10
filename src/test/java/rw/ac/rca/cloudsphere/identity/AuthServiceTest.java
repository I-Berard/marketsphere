package rw.ac.rca.cloudsphere.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.ac.rca.cloudsphere.cart.CartService;
import rw.ac.rca.cloudsphere.common.exception.ApiException;
import rw.ac.rca.cloudsphere.security.JwtService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("test@cloudsphere.rw", "Pass123!", "John", "Doe", "0780000000");

        when(users.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(encoder.encode(request.password())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("token123");
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        TokenResponse response = authService.register(request, null);
        assertNotNull(response);
        assertEquals("token123", response.accessToken());
    }

    @Test
    void testRegister_EmailExists() {
        RegisterRequest request = new RegisterRequest("test@cloudsphere.rw", "Pass123!", "John", "Doe", "0780000000");
        when(users.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(request, null));
    }
}
