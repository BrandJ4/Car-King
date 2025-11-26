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

import com.example.demo.dto.PlazaStateDTO;
import com.example.demo.dto.ReservaRequestDTO;
import com.example.demo.entity.Plaza;
import com.example.demo.entity.Reserva;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.Boleta;
import com.example.demo.repository.PlazaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.BoletaRepository;
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
    private final BoletaRepository boletaRepository; 

    // Endpoint simple: devuelve todas las plazas de una cochera (sin estados)
    @GetMapping("/plazas/{cocheraId}")
    public ResponseEntity<List<Plaza>> obtenerPlazas(@PathVariable Long cocheraId) {
        List<Plaza> plazas = plazaRepository.findByCocheraId(cocheraId);
        return ResponseEntity.ok(plazas);
    }

    // Actualizar ocupación de una plaza (usado por recepcionista para el management list)
    @PostMapping("/plazas/{id}/estado")
    public ResponseEntity<Void> actualizarEstado(@PathVariable Long id, @RequestParam boolean ocupada) {
        plazaService.actualizarOcupacion(id, ocupada);
        return ResponseEntity.ok().build();
    }

    // Endpoint para procesar la Reserva y Boleta (Conductor)
    @PostMapping("/reservar")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaRequestDTO reservaDto) {
        try {
            // Llama al método de reserva del Conductor (crea reserva y boleta si es anticipado)
            Reserva reservaGenerada = reservaService.crearReservaConductor(reservaDto);
            // Devuelve la reserva completa (con o sin boleta ligada)
            return ResponseEntity.ok(reservaGenerada);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al procesar reserva: " + e.getMessage()));
        }
    }

    // Endpoint que devuelve plazas con estado (ROJO/VERDE/AZUL) para el mapeo
    @GetMapping("/cocheras/{id}/plazas")
    public ResponseEntity<List<PlazaStateDTO>> obtenerPlazasPorCochera(
            @PathVariable Long id,
            @RequestParam(required = false) String horaIngreso,
            @RequestParam(required = false) String horaSalida,
            @RequestParam(defaultValue = "false") boolean unsure) {

        try {
            LocalDateTime ingreso = null;
            LocalDateTime salida = null;
            
            if (horaIngreso != null && !horaIngreso.isEmpty()) {
                try {
                    ingreso = LocalDateTime.parse(horaIngreso);
                } catch (Exception e) {
                    // Si no se puede parsear, simplemente ignorar
                }
            }
            
            if (horaSalida != null && !horaSalida.isEmpty()) {
                try {
                    salida = LocalDateTime.parse(horaSalida);
                } catch (Exception e) {
                    // Si no se puede parsear, simplemente ignorar
                }
            }

            List<Plaza> plazas = plazaRepository.findByCocheraId(id);
            List<PlazaStateDTO> estados = new ArrayList<>();

            for (Plaza plaza : plazas) {
                // Solo necesitamos reservas activas para la validación de cruce
                List<Reserva> reservas = reservaRepository.findByPlazaIdAndActivaTrue(plaza.getId());
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    // DTO de Login definido anidado
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
        // Busca todas las reservas activas (coche dentro de la cochera)
        List<Reserva> reservas = reservaRepository.findByPlazaCocheraIdAndActivaTrue(cocheraId);
        return ResponseEntity.ok(reservas);
    }
    
    // Endpoint 3: Check-in manual (Recepcionista)
    @PostMapping("/recepcion/checkin")
    public ResponseEntity<?> registrarVehiculoManualmente(@RequestBody ReservaRequestDTO reservaDto) {
        try {
            // Llama al método de check-in manual
            Reserva reserva = reservaService.crearReservaManual(reservaDto);
            return ResponseEntity.ok(reserva);
        } catch (IllegalStateException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al registrar check-in: " + e.getMessage()));
        }
    }

    // Endpoint 4: Checkout (Finalizar Servicio y Pago) (Recepcionista)
    @PostMapping("/recepcion/checkout/{reservaId}")
    public ResponseEntity<?> finalizarServicioYpago(@PathVariable Long reservaId, 
                                                         @RequestParam String metodoPago) {
        try {
            Boleta boletaFinal = reservaService.finalizarCheckout(reservaId, metodoPago);
            return ResponseEntity.ok(boletaFinal);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al finalizar checkout: " + e.getMessage()));
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

    // Endpoint: Listar todas las reservas activas (para Checkout/Pagos Pendientes)
    @GetMapping("/recepcion/{cocheraId}/reservas-checkout")
    public ResponseEntity<List<Reserva>> obtenerReservasCheckout(@PathVariable Long cocheraId) {
        // Trae todas las reservas activas (sin hora de salida, pendientes de pago)
        List<Reserva> reservas = reservaRepository.findByPlazaCocheraIdAndActivaTrue(cocheraId);
        return ResponseEntity.ok(reservas);
    }

    // Endpoint: Listar todos los coches registrados en una cochera (histórico completo)
    @GetMapping("/recepcion/{cocheraId}/coches-registrados")
    public ResponseEntity<List<Reserva>> obtenerCochesRegistrados(@PathVariable Long cocheraId) {
        // Trae TODAS las reservas (activas e inactivas) para ver el historial
        List<Reserva> reservas = reservaRepository.findByPlazaCocheraId(cocheraId);
        return ResponseEntity.ok(reservas);
    }

    // Endpoint: Listar boletas pagadas del día (reportes)
    @GetMapping("/recepcion/{cocheraId}/pagos-dia")
    public ResponseEntity<List<Boleta>> obtenerPagosDia(@PathVariable Long cocheraId) {
        // Busca todas las boletas emitidas hoy (simplificado: devuelve todas las boletas)
        // En producción, habría que filtrar por fecha del día actual
        return ResponseEntity.ok(new ArrayList<>());
    }
}