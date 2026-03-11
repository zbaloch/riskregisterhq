package com.riskregister.riskregisterapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.Role;
import com.riskregister.riskregisterapp.entities.User;
import com.riskregister.riskregisterapp.repositories.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Updates a user's role
     * @param userId the ID of the user
     * @param role the new role to assign
     * @return the updated user, or null if user not found
     */
    public User updateUserRole(String userId, Role role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setRole(role);
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Updates a user's role by email
     * @param email the email of the user
     * @param role the new role to assign
     * @return the updated user, or null if user not found
     */
    public User updateUserRoleByEmail(String email, Role role) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setRole(role);
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Promotes a user to STORE_ADMIN role
     * @param userId the ID of the user
     * @return the updated user, or null if user not found
     */
    public User promoteToStoreAdmin(String userId) {
        return updateUserRole(userId, Role.ADMIN);
    }

    /**
     * Promotes a user to STORE_ADMIN role by email
     * @param email the email of the user
     * @return the updated user, or null if user not found
     */
    public User promoteToStoreAdminByEmail(String email) {
        return updateUserRoleByEmail(email, Role.ADMIN);
    }

    /**
     * Demotes a user back to USER role
     * @param userId the ID of the user
     * @return the updated user, or null if user not found
     */
    public User demoteToUser(String userId) {
        return updateUserRole(userId, Role.MEMBER);
    }

    /**
     * Demotes a user back to USER role by email
     * @param email the email of the user
     * @return the updated user, or null if user not found
     */
    public User demoteToUserByEmail(String email) {
        return updateUserRoleByEmail(email, Role.MEMBER);
    }

    /**
     * Checks if a user has STORE_ADMIN role
     * @param user the user to check
     * @return true if user has STORE_ADMIN role, false otherwise
     */
    public boolean isStoreAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Checks if a user has USER role
     * @param user the user to check
     * @return true if user has USER role, false otherwise
     */
    public boolean isUser(User user) {
        return user != null && user.getRole() == Role.MEMBER;
    }
    
    /**
     * Approves a user, allowing them to login
     * @param userId the ID of the user
     * @return the updated user, or null if user not found
     */
    public User approveUser(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setApproved(true);
            return userRepository.save(user);
        }
        return null;
    }
    
    /**
     * Approves a user by email, allowing them to login
     * @param email the email of the user
     * @return the updated user, or null if user not found
     */
    public User approveUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setApproved(true);
            return userRepository.save(user);
        }
        return null;
    }
    
    /**
     * Rejects or revokes approval for a user, preventing them from logging in
     * @param userId the ID of the user
     * @return the updated user, or null if user not found
     */
    public User revokeUserApproval(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setApproved(false);
            return userRepository.save(user);
        }
        return null;
    }
    
    /**
     * Rejects or revokes approval for a user by email, preventing them from logging in
     * @param email the email of the user
     * @return the updated user, or null if user not found
     */
    public User revokeUserApprovalByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setApproved(false);
            return userRepository.save(user);
        }
        return null;
    }
    
    /**
     * Checks if a user is approved
     * @param user the user to check
     * @return true if user is approved, false otherwise
     */
    public boolean isApproved(User user) {
        return user != null && user.isApproved();
    }

    public String getUserId(String email) {
        User user = userRepository.findByEmail(email);
        return user != null ? user.getId() : null;
    }
}
