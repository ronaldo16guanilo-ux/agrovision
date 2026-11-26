package com.agrovision.service;

import com.agrovision.dto.AuthResponseDTO;
import com.agrovision.dto.LoginRequestDTO;
import com.agrovision.dto.RegisterRequestDTO;
import com.agrovision.model.User;
import com.agrovision.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return AuthResponseDTO.error("Debe ingresar correo y contraseña.");
        }

        String email = request.getEmail().trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            return AuthResponseDTO.error("No existe una cuenta registrada con el correo: " + email);
        }

        User user = userOpt.get();

        // Verificar contraseña (soporta texto plano o hash SHA-256)
        boolean passwordMatch = matchesPassword(request.getPassword(), user.getPasswordHash());
        if (!passwordMatch) {
            return AuthResponseDTO.error("Contraseña incorrecta.");
        }

        // Validación de rol según la pestaña elegida (Trabajador vs Cliente)
        String requestedRoleType = request.getRoleType() != null ? request.getRoleType().toUpperCase() : "CLIENT";

        if ("WORKER".equals(requestedRoleType)) {
            if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !"WORKER".equalsIgnoreCase(user.getRole())) {
                return AuthResponseDTO.error("Acceso denegado: Esta cuenta no tiene permisos de Trabajador o Administrador. Inicie sesión en la sección de Clientes.");
            }
        } else if ("CLIENT".equals(requestedRoleType)) {
            // Un trabajador o admin también puede ingresar o se le permite acceso
            // Pero si es cliente regular, validamos su rol
        }

        return AuthResponseDTO.success(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyOrFarm(),
                user.getPhone(),
                "Inicio de sesión exitoso."
        );
    }

    @Transactional
    public AuthResponseDTO registerClient(RegisterRequestDTO request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return AuthResponseDTO.error("El nombre o razón social es obligatorio.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return AuthResponseDTO.error("El correo electrónico es obligatorio.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return AuthResponseDTO.error("La contraseña debe tener al menos 4 caracteres.");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return AuthResponseDTO.error("Ya existe una cuenta registrada con el correo: " + email);
        }

        User newUser = new User();
        newUser.setName(request.getName().trim());
        newUser.setEmail(email);
        newUser.setPasswordHash(hashPassword(request.getPassword().trim()));
        newUser.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        newUser.setRole("CLIENT");
        newUser.setCompanyOrFarm(request.getCompanyOrFarm() != null ? request.getCompanyOrFarm().trim() : "Particular");

        User saved = userRepository.save(newUser);

        return AuthResponseDTO.success(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getCompanyOrFarm(),
                saved.getPhone(),
                "¡Cuenta creada con éxito! Ya puedes acceder a la plataforma."
        );
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) return false;
        if (storedHash.equals(rawPassword)) return true; // match directo
        String hashedRaw = hashPassword(rawPassword);
        return storedHash.equalsIgnoreCase(hashedRaw);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // Fallback
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllClients() {
        List<User> clients = userRepository.findByRoleIgnoreCase("CLIENT");
        clients.forEach(u -> u.setPasswordHash(null));
        return clients;
    }
}
