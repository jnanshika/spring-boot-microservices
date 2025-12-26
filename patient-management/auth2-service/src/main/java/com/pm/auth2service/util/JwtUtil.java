package com.pm.auth2service.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key secretKey;

    //get secretKey from env variables
    public JwtUtil(@Value("${jwt.secret}")  String secretKey) {
        //converting secretKey from string to key bytes
        byte[] keyBytes = Base64.getDecoder().decode(secretKey.getBytes(StandardCharsets.UTF_8));

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email) //default user identity
                .claim("role", role) //custom property -> need to mention the type like "role" then value
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 *60 *60 * 10)) //10 hours
                .signWith(secretKey) //sign token with secretKEy
                .compact(); //compact everything done above into a string
    }

    public void validateToken(String token) {
        try{
            Jwts.parser().verifyWith((SecretKey) secretKey)
                    .build()
                    .parseSignedClaims(token);
        }
        catch (SignatureException e){
            throw new JwtException("Invalid JWT signature");
        }
        catch (JwtException e){
            throw new JwtException("Invalid JWT");
        }
    }
}
