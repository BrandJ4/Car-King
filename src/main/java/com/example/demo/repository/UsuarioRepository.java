package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombre(String nombre);
    // Nuevo método de búsqueda
    Usuario findByNombreCompletoAndPlacaVehiculo(String nombreCompleto, String placaVehiculo);
    // Nuevo método para autenticación de Recepcionista
    Usuario findByNombreAndContraseñaAndRol(String nombre, String contraseña, String rol);
}
