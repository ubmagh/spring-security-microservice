package me.ubmagh.securityservicelegacy.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private AuthenticationManager authenticationManager;
    private UserDetailsService userDetailsService;


    public AuthController(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthenticationManager authenticationManager, UserDetailsService userDetailsService) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }


    @PostMapping("/login-v1")
    public Map<String, String> jwtToken_loginv1(Authentication authentication) { // here spring is taking care of authentication in a basic-auth mode, and returns authentication object

        Instant now = Instant.now();

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .issuer("security-service")
                .claim("scope", authentication.getAuthorities().stream().map(grantedAuthority -> grantedAuthority.getAuthority()).collect(Collectors.joining(" ")))
                .build();
        String jwtAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet)).getTokenValue();
        Map<String, String> tokens = new HashMap<>();
        tokens.put("AccessToken", jwtAccessToken);
        return tokens;
    }


    @PostMapping("/login-v2")
    public Map<String, String> jwtToken_loginv2(String username, String password) { // here we create the authentication ourselves using the authmanager

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        Instant now = Instant.now();

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .issuer("security-service")
                .claim("scope", authentication.getAuthorities().stream().map(grantedAuthority -> grantedAuthority.getAuthority()).collect(Collectors.joining(" ")))
                .build();
        String jwtAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet)).getTokenValue();
        Map<String, String> tokens = new HashMap<>();
        tokens.put("AccessToken", jwtAccessToken);
        return tokens;
    }


    @PostMapping("/login-v3")
    public ResponseEntity<Map<String, String>> jwtToken_loginv3(
            String grantType,
            String username,
            String password,
            boolean withRefreshToken,
            String refreshToken) {

        // here we create the authentication ourselves using the authmanager

        String subject = null;
        String scope = null;
        if (grantType.equals("password")) {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            subject = authentication.getName();
            scope = authentication.getAuthorities()
                    .stream().map(aut -> aut.getAuthority())
                    .collect(Collectors.joining(" "));

        } else if (grantType.equals("refreshToken")) {
            if (refreshToken == null) {
                return new ResponseEntity<>(Map.of("errorMessage", "Refresh  Token is required"), HttpStatus.UNAUTHORIZED);
            }
            Jwt decodeJWT = null;
            try {
                decodeJWT = jwtDecoder.decode(refreshToken);
            } catch (JwtException e) {
                return new ResponseEntity<>(Map.of("errorMessage", e.getMessage()), HttpStatus.UNAUTHORIZED);
            }
            subject = decodeJWT.getSubject();
            UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
            Collection<? extends GrantedAuthority>  authorities = userDetails.getAuthorities();
            scope = authorities.stream().map(auth -> auth.getAuthority()).collect(Collectors.joining(" "));
        }
        Map<String, String> idToken = new HashMap<>();
        Instant instant = Instant.now();
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(instant)
                .expiresAt(instant.plus(withRefreshToken ? 1 : 5, ChronoUnit.MINUTES))
                .issuer("security-service")
                .claim("scope", scope)
                .build();
        String jwtAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet)).getTokenValue();
        idToken.put("accessToken", jwtAccessToken);
        if (withRefreshToken) {
            JwtClaimsSet jwtClaimsSetRefresh = JwtClaimsSet.builder()
                    .subject(subject)
                    .issuedAt(instant)
                    .expiresAt(instant.plus(5, ChronoUnit.MINUTES))
                    .issuer("security-service")
                    .build();
            String jwtRefreshToken = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSetRefresh)).getTokenValue();
            idToken.put("refreshToken", jwtRefreshToken);
        }
        return new ResponseEntity<>(idToken, HttpStatus.OK);
    }


}
