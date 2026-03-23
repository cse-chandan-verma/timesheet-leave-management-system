package com.application.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.authservice.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
	Optional<User> findByEmail(String email);
	
	Optional<User> findByEmployeeCode(String employeeCode);
	
	Optional<User> findByEmailAndIsActiveTrue(String email);
	
	boolean existsByEmail(String email);
	
	boolean existsByEmployeeCode(String employeeCode);
	
	@Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
	Optional<User> findActiveUserByEmail(@Param("email") String email);
	
}
