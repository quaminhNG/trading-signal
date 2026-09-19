package com.trading.signal.controller;

import com.trading.signal.dto.user.UpdateUserRequest;
import com.trading.signal.dto.user.UserResponse;
import com.trading.signal.entity.User;
import com.trading.signal.exception.ResourceNotFoundException;
import com.trading.signal.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return toResponse(findByEmail(principal.getUsername()));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal UserDetails principal,
                                 @Valid @RequestBody UpdateUserRequest request) {
        User user = findByEmail(principal.getUsername());
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        userRepository.save(user);
        return toResponse(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.getCreatedAt());
    }
}
