package com.example.demo;

import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.entity.Cochera;
import com.example.demo.entity.Plaza;
import com.example.demo.repository.CocheraRepository;

@SpringBootApplication
public class CarKingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarKingApplication.class, args);
    }

    /**
     * Poblado inicial: 3 cocheras con X plazas cada una
     */
    @Bean
    CommandLineRunner init(CocheraRepository cocheraRepository) {
        return args -> {
            if (cocheraRepository.count() == 0) {
                // Cochera 1
                Cochera c1 = Cochera.builder().nombre("Cochera 1").capacidad(12).plazas(new ArrayList<>()).build();
                for (int i = 1; i <= 12; i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C1-" + i)
                            .ocupada(false)
                            .borde(i % 4 == 0) // algunos bordes
                            .fila((i-1)/6 + 1)
                            .columna((i-1)%6 + 1)
                            .cochera(c1)
                            .build();
                    c1.getPlazas().add(p);
                }
                cocheraRepository.save(c1);

                // Cochera 2
                Cochera c2 = Cochera.builder().nombre("Cochera 2").capacidad(18).plazas(new ArrayList<>()).build();
                for (int i = 1; i <= 18; i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C2-" + i)
                            .ocupada(false)
                            .borde(i % 5 == 0)
                            .fila((i-1)/6 + 1)
                            .columna((i-1)%6 + 1)
                            .cochera(c2)
                            .build();
                    c2.getPlazas().add(p);
                }
                cocheraRepository.save(c2);

                // Cochera 3
                Cochera c3 = Cochera.builder().nombre("Cochera 3").capacidad(10).plazas(new ArrayList<>()).build();
                for (int i = 1; i <= 10; i++) {
                    Plaza p = Plaza.builder()
                            .codigo("C3-" + i)
                            .ocupada(false)
                            .borde(i % 3 == 0)
                            .fila((i-1)/5 + 1)
                            .columna((i-1)%5 + 1)
                            .cochera(c3)
                            .build();
                    c3.getPlazas().add(p);
                }
                cocheraRepository.save(c3);
            }
        };
    }
}
