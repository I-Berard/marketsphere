package rw.ac.rca.cloudsphere.identity;

public record TokenResponse(String accessToken, String tokenType, UserResponse user) {
    public TokenResponse(String accessToken, UserResponse user) {
        this(accessToken, "Bearer", user);
    }
}
