package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Usuario;
import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombre(String nombre);
    
    List<Usuario> findByNombreCompletoAndPlacaVehiculo(String nombreCompleto, String placaVehiculo);
    
    // Método para buscar solo por nombre y rol
    Usuario findByNombreAndRol(String nombre, String rol);
    
    // Método antiguo que debe ser eliminado o reemplazado:
    // Usuario findByNombreAndContraseñaAndRol(String nombre, String contraseña, String rol);
}