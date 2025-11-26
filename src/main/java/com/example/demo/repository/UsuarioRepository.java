package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Usuario;
import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombre(String nombre);
    
    // Cambio: usar List en lugar de Optional para evitar "non-unique result" cuando hay múltiples conductores
    List<Usuario> findByNombreCompletoAndPlacaVehiculo(String nombreCompleto, String placaVehiculo);
    
    Usuario findByNombreAndContraseñaAndRol(String nombre, String contraseña, String rol);
}