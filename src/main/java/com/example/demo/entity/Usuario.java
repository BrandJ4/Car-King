package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull // Requerido por el builder de recepcionista
    private String nombre; 
    @NonNull // Requerido por el builder de recepcionista
    private String rol; 
    private String contraseña;
    
    // Campos del conductor
    private String nombreCompleto; 
    private String placaVehiculo;

    @ManyToOne
    @JoinColumn(name = "cochera_id")
    @JsonIgnore
    private Cochera cocheraAsignada;
}