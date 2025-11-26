package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombre(String nombre);
    
    // Cambiado a Optional para un manejo de nulos más limpio en el servicio (CORREGIDO)
    Optional<Usuario> findByNombreCompletoAndPlacaVehiculo(String nombreCompleto, String placaVehiculo);
    
    Usuario findByNombreAndContraseñaAndRol(String nombre, String contraseña, String rol);
}