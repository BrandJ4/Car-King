package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plaza {
    private boolean borde;
    private Integer fila;
    private Integer columna;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo; // Ejemplo: "A1", "A2", "B1"
    private boolean ocupada;

    @ManyToOne
    @JoinColumn(name = "cochera_id")
    private Cochera cochera;
}
