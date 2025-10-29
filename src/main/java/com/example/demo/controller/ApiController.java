package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.example.demo.dto.PlazaStateDTO;
import com.example.demo.entity.Plaza;
import com.example.demo.entity.Reserva;
import com.example.demo.repository.PlazaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.service.PlazaService;
import com.example.demo.util.ColorUtils;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final PlazaService plazaService;         // si usas servicios
    private final PlazaRepository plazaRepository;   // para queries directas
    private final ReservaRepository reservaRepository;

    // Endpoint simple: devuelve todas las plazas de una cochera (sin estados)
    @GetMapping("/plazas/{cocheraId}")
    public ResponseEntity<List<Plaza>> obtenerPlazas(@PathVariable Long cocheraId) {
        List<Plaza> plazas = plazaRepository.findByCocheraId(cocheraId);
        return ResponseEntity.ok(plazas);
    }

    // Actualizar ocupación de una plaza (usado por recepcionista)
    @PostMapping("/plazas/{id}/estado")
    public ResponseEntity<Void> actualizarEstado(@PathVariable Long id, @RequestParam boolean ocupada) {
        plazaService.actualizarOcupacion(id, ocupada);
        return ResponseEntity.ok().build();
    }

    // Endpoint que devuelve plazas con estado (ROJO/VERDE/AZUL) para el mapeo
    @GetMapping("/cocheras/{id}/plazas")
    public ResponseEntity<List<PlazaStateDTO>> obtenerPlazasPorCochera(
            @PathVariable Long id,
            @RequestParam(required = false) String horaIngreso,
            @RequestParam(required = false) String horaSalida,
            @RequestParam(defaultValue = "false") boolean unsure) {

        LocalDateTime ingreso = (horaIngreso != null && !horaIngreso.isEmpty()) ? LocalDateTime.parse(horaIngreso) : null;
        LocalDateTime salida = (horaSalida != null && !horaSalida.isEmpty()) ? LocalDateTime.parse(horaSalida) : null;

        List<Plaza> plazas = plazaRepository.findByCocheraId(id);
        List<PlazaStateDTO> estados = new ArrayList<>();

        for (Plaza plaza : plazas) {
            List<Reserva> reservas = reservaRepository.findByPlazaId(plaza.getId());
            ColorUtils.Estado estado = ColorUtils.calcularEstadoPlaza(plaza, reservas, ingreso, salida, unsure);

            PlazaStateDTO dto = new PlazaStateDTO(
                    plaza.getId(),
                    plaza.getCodigo(),
                    estado.toString(),
                    plaza.isOcupada(),
                    plaza.isBorde(),
                    plaza.getFila(),
                    plaza.getColumna()
            );
            estados.add(dto);
        }

        return ResponseEntity.ok(estados);
    }

    // Endpoint de dashboard con datos de ejemplo
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("conApp", 28);
        datos.put("sinApp", 12);
        datos.put("dias", List.of("Lun", "Mar", "Mié", "Jue", "Vie"));
        datos.put("ingresos", List.of(120, 200, 150, 300, 250));
        return ResponseEntity.ok(datos);
    }
}
