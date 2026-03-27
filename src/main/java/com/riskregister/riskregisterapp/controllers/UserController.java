package com.riskregister.riskregisterapp.controllers;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.riskregister.riskregisterapp.entities.CustomUserDetails;
import com.riskregister.riskregisterapp.entities.Organization;
import com.riskregister.riskregisterapp.entities.Role;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.OrganizationRepository;
import com.riskregister.riskregisterapp.repositories.UserRepository;
import com.riskregister.riskregisterapp.services.CustomUserDetailsService;
import com.riskregister.riskregisterapp.services.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;

@Controller
public class UserController {

    private static Logger log = LoggerFactory.getLogger(UserController.class);

    private final SecureRandom random = new SecureRandom();
    private static final int TOKEN_BYTE_SIZE = 32;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // private SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("create-user")
    public String createUser(User user,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false) String organizationName) {
        // this.securityContextRepository = securityContextRepository;
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Create organization
        Organization org = new Organization();
        org.setName(organizationName != null && !organizationName.isBlank() ? organizationName : "Default Organization");
        organizationRepository.save(org);

        // Set organization ID for user
        user.setOrganizationId(org.getId());

        // Self-signup always creates ADMIN with approved=true
        user.setRole(Role.ADMIN);
        user.setApproved(true);

        userRepository.save(user);
        model.addAttribute("successTitle",
                "Yay! Your account has been created successfully! Log in to your account now.");
        model.addAttribute("successMessage", "Your account has been created successfully! Log in to your account now.");

        authenticateUser(user.getEmail(), request, response);

        return "redirect:/";
    }

    @GetMapping("/verify-token-and-login")
    public String verifyTokenAndLogin(User user,
            Model model,
            @Param(value = "token") String token,
            @Param(value = "email") String email,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttrs) {

        User existingUser = userRepository.findByEmailAndToken(email, token);
        if (existingUser != null) {

            authenticateUser(existingUser.getEmail(), request, response);

            redirectAttrs.addFlashAttribute("successMessage", "Login successfull.");

            // Invalidate the token
            existingUser.setToken(null);
            existingUser.setTokenUsedDate(null);
            existingUser.setTokenExpirationDate(null);
            userRepository.save(existingUser);

            return "redirect:/";
        } else {
            existingUser = userRepository.findByEmail(email);
            if (existingUser == null) {
                model.addAttribute("errorTitle", "Login error");
                model.addAttribute("errorMessage", "No account for this email.");
            } else {
                model.addAttribute("errorTitle", "Login error");
                model.addAttribute("errorMessage",
                        "Login link expired. Please try login again and check your email for new login link.");
            }

            return "login";
        }

    }

    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(name = "error", required = false) String error) {
        model.addAttribute("error", error);
        if (error != null) {
            model.addAttribute("errorTitle", "Login failed!");
            model.addAttribute("errorMessage", "Your email or password did not match our records. Please try again.");
        }
        model.addAttribute("user", new User());

        return "login";
    }

    @PostMapping("create-user-magic")
    public String createUserMagic(User user,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(required = false) String organizationName) {
        User existingUser = userRepository.findByEmail(user.getEmail());

        String token = generateToken();

        if (existingUser != null) {

            existingUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            existingUser.setTokenExpirationDate(expirationDate);
            existingUser.setTokenUsedDate(null);
            userRepository.save(existingUser);
            emailService.sendEmail(existingUser);

        } else {

            // Create organization for new user
            Organization org = new Organization();
            org.setName(organizationName != null && !organizationName.isBlank() ? organizationName : "Default Organization");
            organizationRepository.save(org);

            // Set organization ID for new user
            user.setOrganizationId(org.getId());

            // Self-signup always creates ADMIN with approved=true
            user.setRole(Role.ADMIN);
            user.setApproved(true);

            user.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            user.setTokenExpirationDate(expirationDate);
            user.setTokenUsedDate(null);
            userRepository.save(user);
            emailService.sendEmail(user);

        }

        model.addAttribute("user", new User());

        model.addAttribute("successTitle", "Magic link sent to email");
        model.addAttribute("successMessage", "Check your email for the magic link to login to your account.");

        return "signup";
    }

    @PostMapping("login-magic")
    public String loginMagic(User user,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        User existingUser = userRepository.findByEmail(user.getEmail());

        String token = generateToken();

        if (existingUser != null) {

            existingUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            existingUser.setTokenExpirationDate(expirationDate);
            existingUser.setTokenUsedDate(null);
            userRepository.save(existingUser);
            model.addAttribute("user", new User());

            // return "login";
            // return "redirect:/login-magic?token=" + token;

            emailService.sendEmail(existingUser);

            model.addAttribute("successTitle", "Magic link sent to email");
            model.addAttribute("successMessage", "Check your email for the magic link to login to your account.");

            return "login";

        } else {

            model.addAttribute("user", new User());

            model.addAttribute("errorTitle", "Login error");
            model.addAttribute("errorMessage", "No account for this email.");

            return "signup";

        }

    }

    @GetMapping("/forgot-password")
    public String showForgotForm(Model model) {
        model.addAttribute("user", new User());
        return "forgot-password";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", user);
        return "profile/index";
    }

    private void autoLoginAfterSignup(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void authenticateUser(String email, HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities());

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private String generateToken() {

        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        random.nextBytes(bytes);
        return String.valueOf(Hex.encode(bytes));
    }
}
