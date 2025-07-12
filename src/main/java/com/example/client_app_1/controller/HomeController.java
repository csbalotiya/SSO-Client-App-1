package com.example.client_app_1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class HomeController {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/home")
    public String home() {
        return "home"; // Create home.html template
    }


    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String welcome(@AuthenticationPrincipal OAuth2User principal) {

        try {
            // Read HTML template from resources
            String htmlTemplate = loadHtmlTemplate();

            // Extract user information
            String username = principal != null ? principal.getName() : "Unknown User";
            String authorities = principal != null ? principal.getAuthorities().toString() : "No authorities";
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));

            String clientName = "OAuth2 Client";
            String registrationId = "Not found";

            if (principal != null) {
                // Try to find the client registration
                OAuth2AuthorizedClient authorizedClient = findAuthorizedClient(principal);
                if (authorizedClient != null) {
                    clientName = authorizedClient.getClientRegistration().getClientName();
                    registrationId = authorizedClient.getClientRegistration().getRegistrationId();
                }
            }

            // Replace placeholders in template
            htmlTemplate = htmlTemplate.replace("{{username}}", username);
            htmlTemplate = htmlTemplate.replace("{{authorities}}", authorities);
            htmlTemplate = htmlTemplate.replace("{{currentTime}}", currentTime);
            htmlTemplate = htmlTemplate.replace("{{clientName}}", clientName);

            return htmlTemplate;

        } catch (IOException e) {
            return "<h1>Error loading template: " + e.getMessage() + "</h1>";
        }
    }

    private String loadHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/welcome.html");
        byte[] binaryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(binaryData, StandardCharsets.UTF_8);
    }

    private OAuth2AuthorizedClient findAuthorizedClient(OAuth2User principal) {
        // Get all available client registrations
        List<String> registrationIds = getAvailableClientRegistrations();

        for (String registrationId : registrationIds) {
            try {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        registrationId, principal.getName());
                if (client != null) {
                    return client;
                }
            } catch (Exception e) {
                // Continue to next registration
            }
        }
        return null;
    }

    private List<String> getAvailableClientRegistrations() {
        List<String> registrationIds = new ArrayList<>();

        // Common registration IDs to try
        String[] commonIds = {"google", "github", "facebook", "my-auth-server", "auth-server", "sso-server", "oauth2-server"};

        for (String id : commonIds) {
            try {
                ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(id);
                if (registration != null) {
                    registrationIds.add(id);
                }
            } catch (Exception e) {
                // Registration not found, continue
            }
        }

        return registrationIds;
    }

    @GetMapping(value = "/profile", produces = MediaType.TEXT_HTML_VALUE)
    public String profile(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "<h1>Not authenticated</h1>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Profile</title></head><body>");
        html.append("<h1>User Profile</h1>");
        html.append("<h2>Attributes:</h2>");
        html.append("<ul>");

        principal.getAttributes().forEach((key, value) -> {
            html.append("<li><strong>").append(key).append(":</strong> ").append(value).append("</li>");
        });

        html.append("</ul>");
        html.append("<a href='/'>Back to Home</a>");
        html.append("</body></html>");

        return html.toString();
    }
}