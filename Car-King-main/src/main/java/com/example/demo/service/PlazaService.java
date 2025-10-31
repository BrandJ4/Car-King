package com.example.demo.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.demo.repository.PlazaRepository;
import com.example.demo.entity.Plaza;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaService {
    private final PlazaRepository plazaRepository;

    public List<Plaza> obtenerPlazasPorCochera(Long cocheraId) {
        return plazaRepository.findByCocheraId(cocheraId);
    }

    public void actualizarOcupacion(Long plazaId, boolean ocupada) {
        Plaza plaza = plazaRepository.findById(plazaId).orElseThrow();
        plaza.setOcupada(ocupada);
        plazaRepository.save(plaza);
    }
}
