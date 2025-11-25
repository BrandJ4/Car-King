package com.example.demo.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.NonNull; // <-- ¡AÑADIDO!
import com.example.demo.repository.PlazaRepository;
import com.example.demo.entity.Plaza;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaService {
    private final PlazaRepository plazaRepository;

    // Aplicar @NonNull
    public List<Plaza> obtenerPlazasPorCochera(@NonNull Long cocheraId) {
        return plazaRepository.findByCocheraId(cocheraId);
    }

    // Aplicar @NonNull
    public void actualizarOcupacion(@NonNull Long plazaId, boolean ocupada) {
        Plaza plaza = plazaRepository.findById(plazaId).orElseThrow();
        plaza.setOcupada(ocupada);
        plazaRepository.save(plaza);
    }
}