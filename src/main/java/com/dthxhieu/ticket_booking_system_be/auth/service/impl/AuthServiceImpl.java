package com.dthxhieu.ticket_booking_system_be.auth.service.impl;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RegisterResponse;
import com.dthxhieu.ticket_booking_system_be.auth.mapper.AuthMapper;
import com.dthxhieu.ticket_booking_system_be.auth.service.AuthService;
import com.dthxhieu.ticket_booking_system_be.common.constant.RoleConstant;
import com.dthxhieu.ticket_booking_system_be.common.enums.UserStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.EmailAlreadyExistsException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.auth.Role;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRole;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRoleId;
import com.dthxhieu.ticket_booking_system_be.repository.auth.RoleRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        // 1. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        // 2. Lấy role mặc định CUSTOMER
        Role role = roleRepository.findByName(RoleConstant.CUSTOMER)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Default role CUSTOMER not found"));

        // 3. Tạo User
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())

                // TODO: Thay bằng passwordEncoder.encode(...)
                .password(passwordEncoder.encode(request.getPassword()))

                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        // 4. Lưu User
        User savedUser = userRepository.save(user);

        // 5. Tạo UserRole
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();

        // 6. Lưu UserRole
        userRoleRepository.save(userRole);

        // 7. Trả về DTO
        return authMapper.toRegisterResponse(savedUser);
    }
}