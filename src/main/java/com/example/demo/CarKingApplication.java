package com.example.demo;

import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.entity.Cochera;
import com.example.demo.entity.Plaza;
import com.example.demo.entity.Usuario; 

import com.example.demo.repository.CocheraRepository;
import com.example.demo.repository.PlazaRepository; 
import com.example.demo.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class CarKingApplication {

    private final CocheraRepository cocheraRepository;
    private final PlazaRepository plazaRepository;
    private final UsuarioRepository usuarioRepository; 
     private final PasswordEncoder passwordEncoder; // Inyectar PasswordEncoder
    
        public static void main(String[] args) {
        SpringApplication.run(CarKingApplication.class, args);
    }

    @Bean
    @SuppressWarnings("null") // Suprime las advertencias de Null Safety en este método
    public CommandLineRunner initData() {
        return args -> {
            if (cocheraRepository.count() == 0) {
                
                // 1. Crear Cocheras y guardarlas inmediatamente (Líneas 41, 45, 49 en su captura)
                Cochera c1 = cocheraRepository.save(Cochera.builder()
                    .nombre("Parque Echenique").capacidad(12).plazas(new ArrayList<>()).build()); 

                Cochera c2 = cocheraRepository.save(Cochera.builder()
                    .nombre("Plaza de Armas - Chosica").capacidad(18).plazas(new ArrayList<>()).build()); 

                Cochera c3 = cocheraRepository.save(Cochera.builder()
                    .nombre("Trebol Azul").capacidad(10).plazas(new ArrayList<>()).build()); 
                
                // Lógica original de creación de Plazas (Líneas 72, 85, 98 en su captura)
                
                // Plazas para C1 (12 plazas)
                for (int i = 1; i <= c1.getCapacidad(); i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C1-" + i)
                            .ocupada(false)
                            .borde(i % 4 == 0)
                            .fila((i-1)/6 + 1)
                            .columna((i-1)%6 + 1)
                            .cochera(c1)
                            .build();
                    plazaRepository.save(p);
                }
                
                // Plazas para C2 (18 plazas)
                for (int i = 1; i <= c2.getCapacidad(); i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C2-" + i)
                            .ocupada(false)
                            .borde(i % 5 == 0)
                            .fila((i-1)/6 + 1)
                            .columna((i-1)%6 + 1)
                            .cochera(c2)
                            .build();
                    plazaRepository.save(p);
                }

                // Plazas para C3 (10 plazas)
                for (int i = 1; i <= c3.getCapacidad(); i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C3-" + i)
                            .ocupada(false)
                            .borde(i % 3 == 0)
                            .fila((i-1)/5 + 1)
                            .columna((i-1)%5 + 1)
                            .cochera(c3)
                            .build();
                    plazaRepository.save(p);
                }


                // 2. Crear Recepcionistas y asignar Cochera (Líneas 102, 109 en su captura)
                // 3. Crear Recepcionistas y asignar Cochera con contraseñas HASHED
                usuarioRepository.save(Usuario.builder()
                        .nombre("recepcion1")
                        .contraseña(passwordEncoder.encode("1234")) // Contraseña hasheada
                        .rol("RECEPCIONISTA")
                        .cocheraAsignada(c1)
                        .build());

                // 3. Crear Recepcionistas y asignar Cochera con contraseñas HASHED
                usuarioRepository.save(Usuario.builder()
                        .nombre("recepcion2")
                        .contraseña(passwordEncoder.encode("4567")) // Contraseña hasheada
                        .rol("RECEPCIONISTA")
                        .cocheraAsignada(c2)
                        .build());

                // 3. Crear Recepcionistas y asignar Cochera con contraseñas HASHED
                usuarioRepository.save(Usuario.builder()
                        .nombre("recepcion3")
                        .contraseña(passwordEncoder.encode("7894")) // Contraseña hasheada
                        .rol("RECEPCIONISTA")
                        .cocheraAsignada(c3)
                        .build());

                // 3. Crear usuario Conductor de prueba (Línea 117 en su captura)
                usuarioRepository.save(Usuario.builder()
                        .nombre("conductor")
                        .contraseña("1234")
                        .rol("CONDUCTOR")
                        .nombreCompleto("Juan Perez")
                        .placaVehiculo("XYZ-987")
                        .build());
            }
        };
    }
}