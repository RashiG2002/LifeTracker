package com.lifetracker.service;

import com.lifetracker.entity.User;
import com.lifetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).email("test@example.com").password("password").build();
    }

    @Test
    void registerUser_shouldRegisterSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.registerUser(user);

        // Assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void registerUser_shouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.registerUser(user));
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void loginUser_shouldReturnUserWhenCredentialsMatch() {
        // Arrange
        when(userRepository.findByEmailAndPassword("test@example.com", "password")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.loginUser("test@example.com", "password");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void loginUser_shouldReturnEmptyWhenCredentialsDoNotMatch() {
        // Arrange
        when(userRepository.findByEmailAndPassword("test@example.com", "wrong")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.loginUser("test@example.com", "wrong");

        // Assert
        assertFalse(result.isPresent());
    }
}