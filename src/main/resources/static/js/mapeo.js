// Variable para almacenar la plaza seleccionada
let selectedPlaza = null;
const PRECIO_POR_HORA = 5.0; // Consistente con el backend

document.addEventListener("DOMContentLoaded", () => {
    const cocheraId = sessionStorage.getItem("cocheraId");
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    const unsure = sessionStorage.getItem("unsure") === 'true'; // Convertir a booleano

    // Construir la URL con las horas y el estado 'unsure'
    const params = new URLSearchParams({
        horaIngreso: getCurrentDateTimeString(horaIngreso),
        horaSalida: horaSalida ? getCurrentDateTimeString(horaSalida) : '',
        unsure: unsure
    });

    fetch(`/api/cocheras/${cocheraId}/plazas?${params.toString()}`)
        .then(r => r.json())
        .then(data => {
            const cont = document.getElementById("mapeo");
            // Almacenar el número de columnas para el grid (asumo que es el máximo de 'columna' en la data)
            const maxColumna = data.reduce((max, p) => Math.max(max, p.columna || 0), 0);
            cont.style.gridTemplateColumns = `repeat(${maxColumna}, 50px)`;
            
            data.forEach(plaza => {
                const div = document.createElement("div");
                div.classList.add("plaza");
                div.classList.add(plaza.estado.toLowerCase());
                div.textContent = plaza.codigo;
                div.dataset.plazaId = plaza.plazaId;
                div.dataset.estado = plaza.estado;
                div.dataset.codigo = plaza.codigo;
                
                // Posicionamiento en el grid (si los campos fila/columna están definidos)
                if (plaza.fila && plaza.columna) {
                    div.style.gridRowStart = plaza.fila;
                    div.style.gridColumnStart = plaza.columna;
                }

                // Solo permitir selección de plazas VERDE o AZUL
                if (plaza.estado === 'VERDE' || plaza.estado === 'AZUL') {
                    div.onclick = () => seleccionar(div, plaza);
                    div.style.cursor = 'pointer';
                } else {
                    div.style.cursor = 'not-allowed';
                }

                cont.appendChild(div);
            });
        })
        .catch(err => {
             document.getElementById("mapeo").innerHTML = '<p style="color: red;">Error al cargar las plazas.</p>';
             console.error(err);
        });
});

/**
 * Convierte la hora ingresada (HH:MM) a un formato completo de fecha y hora ISO (YYYY-MM-DDTHH:MM:SS)
 * Se usa la fecha actual para el año, mes y día.
 */
function getCurrentDateTimeString(timeStr) {
    if (!timeStr) return '';
    const now = new Date();
    const [hours, minutes] = timeStr.split(':').map(Number);
    now.setHours(hours, minutes, 0, 0);
    // Formato ISO 8601: 2025-11-24T18:00:00
    return now.toISOString().slice(0, 19);
}

function seleccionar(div, plazaData) {
    // Deseleccionar todos
    document.querySelectorAll(".plaza").forEach(p => p.classList.remove("seleccionado"));
    
    // Seleccionar la actual
    div.classList.add("seleccionado");
    selectedPlaza = plazaData;

    // --- Actualizar Sidebar ---
    document.getElementById('plazaSeleccionadaDisplay').textContent = plazaData.codigo;
    document.getElementById('plazaEstadoDisplay').textContent = plazaData.estado;
    
    // Mostrar el formulario de confirmación
    const confirmacionPagoDiv = document.getElementById('confirmacionPago');
    confirmacionPagoDiv.style.display = 'block';
    document.getElementById('plazaSeleccionada').textContent = plazaData.codigo;

    const unsure = sessionStorage.getItem("unsure") === 'true';
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    
    document.getElementById('rangoHoras').textContent = `${horaIngreso} a ${horaSalida || 'Indefinido'}`;

     const seccionPago = document.getElementById('seccionPago');
    const pagoPosterior = document.getElementById('pagoPosterior');
    const btnConfirmar = document.getElementById('btnConfirmar');

    if (unsure || !horaSalida) {
        // Pago posterior
        seccionPago.style.display = 'none';
        pagoPosterior.style.display = 'block';
        btnConfirmar.textContent = 'Confirmar Ingreso (Pago a la Salida)';
    } else {
        // Pago al momento
        seccionPago.style.display = 'block';
        pagoPosterior.style.display = 'none';
        btnConfirmar.textContent = 'Confirmar Reserva y Pagar';

        // Calcular y mostrar el monto
        const inicio = getCurrentDateTimeString(horaIngreso);
        const fin = getCurrentDateTimeString(horaSalida);
        
        // Simular cálculo de horas: (Nota: El backend hará el cálculo definitivo)
        const diffMs = new Date(fin).getTime() - new Date(inicio).getTime();
        const diffHours = Math.ceil(diffMs / (1000 * 60 * 60));
        const totalPagar = diffHours > 0 ? diffHours * PRECIO_POR_HORA : PRECIO_POR_HORA;
        
        document.getElementById('totalPagar').textContent = `S/. ${totalPagar.toFixed(2)}`;
    }
}

function cancelarSeleccion() {
    selectedPlaza = null;
    document.querySelectorAll(".plaza").forEach(p => p.classList.remove("seleccionado"));
    document.getElementById('confirmacionPago').style.display = 'none';

    // Resetear el sidebar
    document.getElementById('plazaSeleccionadaDisplay').textContent = 'Ninguna';
    document.getElementById('plazaEstadoDisplay').textContent = '---';
}

function confirmarReserva() {
    if (!selectedPlaza) {
        alert("Por favor, seleccione una plaza.");
        return;
    }
    
    // Obtener datos del sessionStorage
    const nombre = sessionStorage.getItem("nombreConductor");
    const placa = sessionStorage.getItem("placaVehiculo");
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    const unsure = sessionStorage.getItem("unsure") === 'true';
    const metodoPago = document.getElementById('metodoPago').value;
    
    // Preparar datos ISO para el backend
    const horaIngresoISO = getCurrentDateTimeString(horaIngreso);
    const horaSalidaISO = unsure ? null : getCurrentDateTimeString(horaSalida); 

    const reservaData = {
        plazaId: selectedPlaza.id,
        horaIngreso: horaIngresoISO,
        horaSalida: horaSalidaISO,
        unsure: unsure,
        nombreConductor: nombre,
        placaVehiculo: placa,
        metodoPago: metodoPago 
    };

    fetch('/api/reservar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reservaData)
    })
    .then(async response => {
        if (!response.ok) {
            // Lógica de error mejorada: intenta leer el mensaje de error del cuerpo
            const errorText = await response.text(); 
            try {
                // Si el cuerpo es JSON con un mensaje, úsalo
                const errorJson = JSON.parse(errorText);
                throw new Error(errorJson.message || errorText);
            } catch (e) {
                // Si no es JSON o no tiene mensaje, usa el texto crudo
                throw new Error(errorText || 'Error desconocido al confirmar la reserva.');
            }
        }
        return response.json();
    })
    .then(reserva => {
        // ... (Lógica de éxito: Mostrar boleta, limpiar sessionStorage, etc.)
        sessionStorage.clear();
        alert(`Reserva de plaza ${reserva.plaza.codigo} confirmada. ¡Bienvenido!`);
        // Aquí se mostraría la boleta, por ahora solo redirigimos o mostramos mensaje
        document.getElementById('reservaExitosa').innerHTML = `
            <h3>¡Reserva Exitosa!</h3>
            <p>Plaza: <strong>${reserva.plaza.codigo}</strong></p>
            <p>Ingreso: ${new Date(reserva.horaIngreso).toLocaleString()}</p>
            ${reserva.horaSalida ? `<p>Salida estimada: ${new Date(reserva.horaSalida).toLocaleString()}</p>` : `<p>Pago al salir (Cálculo por cronómetro).</p>`}
            ${reserva.pagado ? `<p>Total pagado: <strong>S/. ${reserva.boleta.monto.toFixed(2)}</strong></p>` : `<p>Estado: Pago Pendiente</p>`}
        `;
        document.getElementById('confirmacionPago').style.display = 'none';
        document.getElementById('reservaExitosa').style.display = 'block';
        document.querySelector('.map-area').style.pointerEvents = 'none';
    })
    .catch(error => {
        alert("ERROR EN RESERVA: " + error.message);
        console.error("Error al confirmar reserva:", error);
    });
}
    // Envío al backend
    fetch('/api/reservar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reservaData)
    })
    .then(r => {
        if (!r.ok) {
            throw new Error('Error al registrar la reserva en el servidor');
        }
        return r.json();
    })
    .then(boleta => {
        // Ocultar mapeo y mostrar éxito
        document.getElementById('mapeo').style.display = 'none';
        document.getElementById('titulo').style.display = 'none';
        document.getElementById('confirmacionPago').style.display = 'none';
        document.getElementById('reservaExitosa').style.display = 'block';

        const detalleBoleta = document.getElementById('detalleBoleta');
        const btnDescargar = document.getElementById('btnDescargarBoleta');
        const mensajePostpago = document.getElementById('mensajePostpago');

        if (boleta) {
            // Pago al momento, mostrar detalles de la boleta
            detalleBoleta.innerHTML = `
                <p><strong>Cochera:</strong> Cochera ${sessionStorage.getItem("cocheraId")}</p>
                <p><strong>Casilla:</strong> ${selectedPlaza.codigo}</p>
                <p><strong>Conductor:</strong> ${reservaData.nombreConductor}</p>
                <p><strong>Vehículo:</strong> ${reservaData.placaVehiculo}</p>
                <p><strong>Ingreso:</strong> ${reservaData.horaIngreso.split('T')[1].substring(0, 5)}</p>
                <p><strong>Salida Estimada:</strong> ${reservaData.horaSalida.split('T')[1].substring(0, 5)}</p>
                <p style="font-size: 1.2em; color: green;"><strong>MONTO PAGADO: S/. ${boleta.monto.toFixed(2)} (${boleta.metodoPago})</strong></p>
            `;
            btnDescargar.style.display = 'block';
            mensajePostpago.style.display = 'none';
            // Guardar datos de la boleta en session para descarga
            sessionStorage.setItem('boletaData', JSON.stringify(boleta));
            sessionStorage.setItem('reservaInfo', JSON.stringify(reservaData));
        } else {
            // Pago a la salida (unsure o salida null)
            detalleBoleta.innerHTML = `
                <p><strong>Cochera:</strong> Cochera ${sessionStorage.getItem("cocheraId")}</p>
                <p><strong>Casilla:</strong> ${selectedPlaza.codigo}</p>
                <p><strong>Conductor:</strong> ${reservaData.nombreConductor}</p>
                <p><strong>Vehículo:</strong> ${reservaData.placaVehiculo}</p>
                <p><strong>Ingreso:</strong> ${reservaData.horaIngreso.split('T')[1].substring(0, 5)}</p>
                <p style="font-size: 1.1em; color: blue;">Su reserva ha sido registrada.</p>
            `;
            mensajePostpago.textContent = "El pago se realizará con el recepcionista al momento de retirar su vehículo.";
            mensajePostpago.style.display = 'block';
            btnDescargar.style.display = 'none'; // No hay boleta real para descargar aún
        }
        
    })
    .catch(error => {
        console.error('Error:', error);
        alert("Ocurrió un error al confirmar la reserva: " + error.message);
    });


function descargarBoleta() {
    const boleta = JSON.parse(sessionStorage.getItem('boletaData'));
    const info = JSON.parse(sessionStorage.getItem('reservaInfo'));
    
    if (!boleta || !info) {
        alert("No se encontró la información de la boleta para descargar.");
        return;
    }

    const contenido = `
========================================
           BOLETA ELECTRÓNICA
             CAR-KING CHOSICA
========================================
Fecha de Emisión: ${new Date(boleta.fechaEmision).toLocaleString()}
----------------------------------------
Cochera: Cochera ${sessionStorage.getItem("cocheraId")}
Casilla: ${selectedPlaza.codigo}
----------------------------------------
Conductor: ${info.nombreConductor}
Vehículo: ${info.placaVehiculo}
----------------------------------------
Hora Ingreso: ${info.horaIngreso.split('T')[1].substring(0, 5)}
Hora Salida Estimada: ${info.horaSalida.split('T')[1].substring(0, 5)}
----------------------------------------
Monto Total: S/. ${boleta.monto.toFixed(2)}
Método de Pago: ${boleta.metodoPago}
========================================
GRACIAS POR SU PREFERENCIA.
`;

    const blob = new Blob([contenido], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Boleta_CarKing_${boleta.id || Date.now()}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}