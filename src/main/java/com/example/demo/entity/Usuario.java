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

    @NonNull
    private String nombre; // Nombre de usuario (e.g., "recepcion1")
    @NonNull
    private String rol; // "CONDUCTOR" o "RECEPCIONISTA"
    private String contraseña;
    
    // Campos del conductor (reutilizados para registrar en el check-in manual)
    private String nombreCompleto; 
    private String placaVehiculo;

    // Relación para el Recepcionista
    @ManyToOne
    @JoinColumn(name = "cochera_id")
    @JsonIgnore
    private Cochera cocheraAsignada;
}