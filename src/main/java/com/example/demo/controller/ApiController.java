package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

// Eliminamos la importación externa si el DTO está definido anidado abajo.
// import com.example.demo.dto.RecepcionistaLoginDTO; 

import com.example.demo.dto.PlazaStateDTO;
import com.example.demo.dto.ReservaRequestDTO;
import com.example.demo.entity.Plaza;
import com.example.demo.entity.Reserva;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.Boleta;
import com.example.demo.repository.PlazaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.PlazaService;
import com.example.demo.service.ReservaService;
import com.example.demo.util.ColorUtils;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final PlazaService plazaService; 
    private final PlazaRepository plazaRepository; 
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService; 
    private final UsuarioRepository usuarioRepository; 

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

    // Endpoint para procesar la Reserva y Boleta (Conductor)
    @PostMapping("/reservar")
    public ResponseEntity<Boleta> crearReserva(@RequestBody ReservaRequestDTO reservaDto) {
        try {
            Boleta boletaGenerada = reservaService.crearReservaYBoleta(reservaDto);
            return ResponseEntity.ok(boletaGenerada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
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

    // DTO de Login definido anidado (lo que elimina la necesidad de importarlo)
    @Data
    public static class RecepcionistaLoginDTO {
        private String nombre;
        private String contraseña;
    }

    // Endpoint 1: Login del Recepcionista (Punto 3)
    @PostMapping("/recepcion/login")
    public ResponseEntity<Map<String, Object>> loginRecepcionista(@RequestBody RecepcionistaLoginDTO loginDto) {
        Usuario recepcionista = usuarioRepository.findByNombreAndContraseñaAndRol(
            loginDto.getNombre(), loginDto.getContraseña(), "RECEPCIONISTA"
        );

        if (recepcionista != null && recepcionista.getCocheraAsignada() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", recepcionista.getId());
            response.put("nombreUsuario", recepcionista.getNombre());
            response.put("cocheraId", recepcionista.getCocheraAsignada().getId());
            response.put("cocheraNombre", recepcionista.getCocheraAsignada().getNombre());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    // Endpoint 2: Listar coches registrados/reservas activas (Punto 3)
    @GetMapping("/cocheras/{cocheraId}/reservas/activas")
    public ResponseEntity<List<Reserva>> obtenerReservasActivasPorCochera(@PathVariable Long cocheraId) {
        List<Reserva> reservas = reservaRepository.findByPlazaCocheraIdAndHoraSalidaIsNull(cocheraId);
        return ResponseEntity.ok(reservas);
    }
    
    // Endpoint 3: Check-in manual (Punto 3)
    @PostMapping("/recepcion/checkin")
    public ResponseEntity<?> registrarVehiculoManualmente(@RequestBody ReservaRequestDTO reservaDto) {
        try {
            reservaDto.setUnsure(true);
            reservaService.crearReservaYBoleta(reservaDto);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint 4: Checkout (Finalizar Servicio y Pago) (Punto 3)
    @PostMapping("/recepcion/checkout/{reservaId}")
    public ResponseEntity<Boleta> finalizarServicioYpago(@PathVariable Long reservaId, 
                                                         @RequestParam String metodoPago) {
        try {
            Boleta boletaFinal = reservaService.finalizarReservaYGenerarBoleta(reservaId, metodoPago);
            return ResponseEntity.ok(boletaFinal);
        } catch (RuntimeException e) {
            // Usar ResponseEntity.badRequest().body(e.getMessage()) si quieres que el front reciba el error.
            return ResponseEntity.badRequest().build(); 
        }
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