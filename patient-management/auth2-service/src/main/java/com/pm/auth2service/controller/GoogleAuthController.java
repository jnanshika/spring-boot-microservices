package com.pm.auth2service.controller;

import ch.qos.logback.classic.Logger;
import com.pm.auth2service.model.User;
import com.pm.auth2service.repository.UserRepository;
import com.pm.auth2service.service.UserService;
import com.pm.auth2service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {

    //value -> get values from application properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;


    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/callback")
    public ResponseEntity<?>handleGoogleCallback(@RequestParam String code){
        try {
            //1.Exchange auth code for tokens
            String tokenEndPoint = "https://oauth2.googleapis.com/token";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", "https://developers.google.com/oauthplayground");
            params.add("grant_type", "authorization_code");

            //create header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            //create request-> header + body
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            //call post end point
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndPoint, request, Map.class);
            String idToken = (String) tokenResponse.getBody().get("id_token");

            //hit google info api with fetched token, retrive user information
            String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token="+idToken;
            ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);

            if (userInfoResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> userInfo = userInfoResponse.getBody();
                String email = (String) userInfo.get("email");

                User user = new User();
                try {
                    user = userService.findByEmail(email);
                    if(user==null){
                        user = new User();
                        user.setEmail(email);
                        user.setUsername(email);
                        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        user.setRole("USER");
                        userRepository.save(user);
                    }
                }
                catch (Exception e) {
                    log.error("Exception occured while fetching user record. " + e.getMessage());
                }
                String jwtToken = jwtUtil.generateToken(email, user.getRole());
                return ResponseEntity.ok(Collections.singletonMap("token", jwtToken));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Exception occurred while handleGoogleCallback ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

/*

https://accounts.google.com/o/oauth2/auth?
client_id=YOUR_CLIENT_ID
    &redirect_uri=YOUR_REDIRECT_URI
    &response_type=code
    &scope=email profile
    &access_type=offline
    &prompt=consent

*/
