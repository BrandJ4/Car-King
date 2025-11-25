package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cochera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull // Requerido por el builder
    private String nombre;
    private int capacidad;

    @OneToMany(mappedBy = "cochera", cascade = CascadeType.ALL)
    private List<Plaza> plazas;
}