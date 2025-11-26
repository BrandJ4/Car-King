// Variables Globales
const cocheraId = sessionStorage.getItem('cocheraId');
const cocheraNombre = sessionStorage.getItem('cocheraNombre');
let reservaEnCheckout = null;

const PRECIO_POR_HORA = 5.0; 
let cronometroInterval; // Para el contador de tiempo de las reservas activas (tabla)
let mapeoCronometroInterval; // Para el cronómetro visual en el mapa

document.addEventListener("DOMContentLoaded", () => {
    // Verificación de sesión e inicialización de UI
    if (!cocheraId) {
        window.location.href = '/recepcion/login';
        return;
    }
    
    document.getElementById('cocheraNombreDisplay').textContent = cocheraNombre;
    document.getElementById('nombreRecepcionista').textContent = `Usuario: ${sessionStorage.getItem('nombreUsuario') || 'Recepcionista'}`;
    
    // Carga inicial
    mostrarSeccion('mapeo'); 
    cargarDashboard(); // Carga inicial del Dashboard
});


// --- Funciones de Ayuda (Cronómetro y Cálculo) ---

/** Convierte la hora ingresada (HH:MM) a un formato completo de fecha y hora ISO (YYYY-MM-DDTHH:MM:SS) */
function getNowDateTimeString(timeStr) {
    if (!timeStr) return '';
    const now = new Date();
    const [hours, minutes] = timeStr.split(':').map(Number);
    now.setHours(hours, minutes, 0, 0);
    return now.toISOString().slice(0, 19);
}

function formatDuration(horaIngresoISO) {
    const ingreso = new Date(horaIngresoISO);
    const now = new Date();
    const diffMs = now.getTime() - ingreso.getTime();
    
    let seconds = Math.floor(diffMs / 1000);
    let minutes = Math.floor(seconds / 60);
    let hours = Math.floor(minutes / 60);

    seconds = seconds % 60;
    minutes = minutes % 60;

    const pad = (num) => num.toString().padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

function calcularTotalPagar(horaIngresoISO) {
    const ingreso = new Date(horaIngresoISO);
    const now = new Date();
    const diffMs = now.getTime() - ingreso.getTime();
    
    const diffHours = Math.ceil(diffMs / (1000 * 60 * 60)); 
    const hoursBilled = Math.max(1, diffHours);
    const totalPagar = hoursBilled * PRECIO_POR_HORA; 
    
    return {
        hours: hoursBilled,
        total: totalPagar.toFixed(2)
    };
}

function iniciarCronometroActivas() {
    if (cronometroInterval) {
        clearInterval(cronometroInterval);
    }
    cronometroInterval = setInterval(() => {
        document.querySelectorAll('[id^="duration-"]').forEach(td => {
            const horaIngresoISO = td.getAttribute('data-ingreso');
            td.textContent = formatDuration(horaIngresoISO);
        });
    }, 1000); 
}

function iniciarCronometroMapeo(activeReservas) {
    if (mapeoCronometroInterval) {
        clearInterval(mapeoCronometroInterval);
    }

    const reservaMap = {};
    activeReservas.forEach(r => {
        reservaMap[r.plaza.id] = r;
    });

    mapeoCronometroInterval = setInterval(() => {
        document.querySelectorAll('.plaza-map-timer').forEach(div => {
            const plazaId = div.dataset.plazaId;
            const reserva = reservaMap[plazaId];

            if (reserva && reserva.activa) {
                div.textContent = formatDuration(reserva.horaIngreso);
            }
        });
    }, 1000);
}

function cerrarSesion() {
    sessionStorage.clear();
    window.location.href = '/recepcion/login';
}

function toggleSubmenu(id) {
    const sub = document.getElementById(id);
    sub.style.display = sub.style.display === 'block' ? 'none' : 'block';
}


// --- Navegación y Renderizado de Vistas ---

function mostrarSeccion(seccion) {
    // Detener cronómetros al cambiar de sección
    if (cronometroInterval) clearInterval(cronometroInterval);
    if (mapeoCronometroInterval) clearInterval(mapeoCronometroInterval);

    document.querySelectorAll('.tab-content').forEach(div => div.classList.remove('active'));
    document.querySelectorAll('.nav-menu button').forEach(btn => btn.classList.remove('active'));
    
    let targetDivId = '';
    
    if (seccion === 'pagosPendientes') {
        targetDivId = 'seccion-pagosPendientes';
        cargarReservasActivas(); 
    } else if (seccion === 'mapeo') {
        targetDivId = 'seccion-mapeo';
        cargarPlazas(); 
    } else if (seccion === 'coches') {
        targetDivId = 'seccion-coches';
        cargarCochesRegistrados();
    } else if (seccion === 'pagosDia') {
        targetDivId = 'seccion-pagosDia';
        cargarPagosDia();
    } else if (seccion === 'reservasActivas') {
        targetDivId = 'seccion-reservasActivas';
        cargarReservasAdelantadas();
    } else if (seccion === 'dashboard') {
        targetDivId = 'seccion-dashboard';
        cargarDashboard();
    } else {
        targetDivId = `seccion-${seccion}`; 
    }
    
    const targetDiv = document.getElementById(targetDivId);
    if (targetDiv) {
        targetDiv.classList.add('active');
    }
    
    const activeBtn = document.querySelector(`[onclick="mostrarSeccion('${seccion}')"]`);
    if (activeBtn) activeBtn.classList.add('active');
}

function renderPlazas(plazasData, activeReservas) {
    const visualGrid = document.getElementById('recepcionMapeoGrid');
    const selectPlaza = document.getElementById('ci_plaza_id');
    
    visualGrid.innerHTML = '';
    if (selectPlaza) selectPlaza.innerHTML = ''; 

    // Find max column for dynamic CSS grid display
    const maxColumna = plazasData.reduce((max, p) => Math.max(max, p.columna || 0), 0);
    if (maxColumna > 0) {
        visualGrid.style.display = 'grid';
        visualGrid.style.gridTemplateColumns = `repeat(${maxColumna}, 60px)`;
        visualGrid.style.gap = '15px';
    } else {
        visualGrid.innerHTML = '<p>No hay plazas definidas para esta cochera.</p>';
        return;
    }
    
    // Almacenar info de reserva en un mapa para acceso rápido
    const reservaMap = activeReservas.reduce((acc, r) => {
        acc[r.plaza.id] = r;
        return acc;
    }, {});
    
    plazasData.forEach(p => {
        const isOccupied = p.ocupada;
        const currentReserva = reservaMap[p.plazaId];

        // --- 1. Visual Map Grid Element ---
        const mapItem = document.createElement('div');
        mapItem.classList.add("plaza"); 
        // Usamos el estado del backend (ROJO/VERDE)
        mapItem.classList.add(isOccupied ? 'rojo' : 'verde'); 
        
        mapItem.style.gridRowStart = p.fila;
        mapItem.style.gridColumnStart = p.columna;
        
        // Si está ocupada, le damos la funcionalidad de 'Toggle' (Cambiar a libre)
        if (isOccupied) {
            mapItem.onclick = () => toggleOcupacion(p.plazaId, p.ocupada);
            mapItem.style.cursor = 'pointer';

            // Mostrar el cronómetro real si hay una reserva activa ligada
            const timerDiv = document.createElement('div');
            timerDiv.classList.add('plaza-map-timer');
            timerDiv.dataset.plazaId = p.plazaId;
            timerDiv.style.fontSize = '0.75em';
            timerDiv.style.fontWeight = 'bold';
            timerDiv.style.marginTop = '2px';
            timerDiv.style.color = 'white'; 

            if (currentReserva) {
                // Muestra el cronómetro (Duración de la estancia)
                timerDiv.textContent = formatDuration(currentReserva.horaIngreso);
                mapItem.innerHTML = `<span style="font-size: 1em;">${p.codigo}</span>`;
                mapItem.appendChild(timerDiv);
            } else {
                // CORRECCIÓN SOLICITADA: Si está ocupada (ROJO) pero no hay registro, SOLO decimos OCUPADO
                mapItem.innerHTML = `<span style="font-size: 1em;">${p.codigo}</span><div style="font-size: 0.8em; margin-top: 2px;">OCUPADO</div>`;
            }
            
            

        } else {
            // Si está libre (VERDE)
            mapItem.style.cursor = 'default';
            mapItem.innerHTML = `<span style="font-size: 1em;">${p.codigo}</span><div style="font-size: 0.8em;">LIBRE</div>`; 
        }
        
        visualGrid.appendChild(mapItem);

        // --- 2. Check-in Select Options ---
        if (!isOccupied && selectPlaza) {
            const option = document.createElement('option');
            option.value = p.plazaId;
            option.textContent = p.codigo;
            selectPlaza.appendChild(option);
        }
    });
    
    // Iniciar el cronómetro visual en el mapa
    iniciarCronometroMapeo(activeReservas);

    // Actualizar nombre de la cochera
    document.getElementById('cocheraNombreMap').textContent = cocheraNombre;
}


function renderReservasActivas(reservas) {
    const reservasActivasContainer = document.getElementById('reservasActivasContainer');
    reservasActivasContainer.innerHTML = '';
    
    if (reservas.length === 0) {
        reservasActivasContainer.innerHTML = '<p>No hay vehículos con pago pendiente (Checkout).</p>';
        if (cronometroInterval) clearInterval(cronometroInterval);
        return;
    }
    
    const tableHtml = `
        <table class="reservas-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Placa</th>
                    <th>Conductor</th>
                    <th>Casilla</th>
                    <th>Ingreso</th>
                    <th>Tiempo (Cronómetro)</th>
                    <th>Estado Pago</th>
                    <th>Acción</th>
                </tr>
            </thead>
            <tbody>
                ${reservas.map(r => {
                    const conductorNombre = r.usuario ? r.usuario.nombreCompleto || r.usuario.nombre : 'N/A';
                    const placa = r.usuario ? r.usuario.placaVehiculo || 'N/A' : 'N/A';
                    const horaIngresoLocal = new Date(r.horaIngreso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
                    
                    const durationDisplayId = `duration-${r.id}`;
                    const durationText = formatDuration(r.horaIngreso); 
                    
                    const estadoPago = r.pagado ? 'Pagado (Anticipado)' : 'PENDIENTE';
                    const estadoColor = r.pagado ? 'green' : 'red';
                    
                    return `
                        <tr>
                            <td>${r.id}</td>
                            <td>${placa}</td>
                            <td>${conductorNombre}</td>
                            <td>${r.plaza.codigo}</td>
                            <td>${horaIngresoLocal}</td>
                            <td id="${durationDisplayId}" data-ingreso="${r.horaIngreso}">${durationText}</td>
                            <td style="color:${estadoColor}; font-weight:bold;">${estadoPago}</td>
                            <td>
                                <button onclick='iniciarCheckout(${JSON.stringify(r)})' class="role-button recepcionista" 
                                    style="background-color: ${r.pagado ? '#6c757d' : '#007bff'};" 
                                    ${r.pagado ? 'disabled' : ''}>
                                    ${r.pagado ? 'Pago Realizado' : 'Finalizar y Pagar'}
                                </button>
                            </td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
    reservasActivasContainer.innerHTML = tableHtml;
    iniciarCronometroActivas();
}


// --- Data Fetching ---

async function cargarPlazas() {
    try {
        // Cargar plazas del estado
        const plazasResponse = await fetch(`/api/cocheras/${cocheraId}/plazas`);
        if (!plazasResponse.ok) {
            throw new Error(`Error en plazas: ${plazasResponse.status}`);
        }
        const plazasData = await plazasResponse.json();

        // Cargar reservas activas (si falla, continuar con lista vacía)
        let reservasData = [];
        try {
            const reservasResponse = await fetch(`/api/cocheras/${cocheraId}/reservas/activas`);
            if (reservasResponse.ok) {
                reservasData = await reservasResponse.json();
            }
        } catch (err) {
            console.warn("No se pudieron cargar las reservas activas, continuando...", err);
            reservasData = [];
        }

        // Se usa plazasData para el renderizado visual y reservasData para el cronómetro
        renderPlazas(plazasData, reservasData); 
    } catch (err) {
        document.getElementById('recepcionMapeoGrid').innerHTML = '<p style="color:red;">Error cargando el mapa de plazas.</p>';
        console.error("Error al cargar mapa:", err);
    }
}

function cargarReservasActivas() {
    fetch(`/api/cocheras/${cocheraId}/reservas/activas`)
        .then(r => {
            if (!r.ok) {
                throw new Error(`Error ${r.status}`);
            }
            return r.json();
        })
        .then(data => {
            // Mostrar todas las reservas activas (independientemente del estado de pago)
            renderReservasActivas(data);
        })
        .catch(err => {
            document.getElementById('reservasActivasContainer').innerHTML = '<p>No hay reservas con pago pendiente.</p>';
            console.error(err);
        });
}


// --- Action Handlers ---

function toggleOcupacion(id, isCurrentlyOccupied) {
    if (!isCurrentlyOccupied) return; // Solo se puede liberar una plaza OCUPADA
    
    // Si ya existe una reserva activa, el recepcionista debe ir a la tabla de checkout.
    // Aquí solo permitimos marcar a 'Libre' si no hay una reserva registrada.
    const confirmAction = confirm("ADVERTENCIA: ¿Desea forzar la liberación de la plaza? Si hay un vehículo sin pagar registrado, debe usar la sección Checkout.");
    
    if (confirmAction) {
        const nuevoEstado = false;
        fetch(`/api/plazas/${id}/estado?ocupada=${nuevoEstado}`, { method: 'POST' })
            .then(r => {
                if (r.ok) {
                    cargarPlazas(); 
                    cargarReservasActivas();
                } else {
                    alert('Error: La plaza no pudo ser liberada. Podría tener una reserva activa pendiente de Checkout.');
                }
            });
    }
}

// Modal Control
function openCheckinModal() {
    document.getElementById('ci_ingreso').value = new Date().toTimeString().substring(0, 5);
    document.getElementById('modal-checkin').style.display = 'flex'; // Usar flex para centrar
}

function closeCheckinModal() {
    document.getElementById('modal-checkin').style.display = 'none';
}

function closeCheckoutModal() {
    document.getElementById('checkoutModal').style.display = 'none';
    reservaEnCheckout = null;
}


function realizarCheckin() {
    const nombre = document.getElementById('ci_nombre').value;
    const placa = document.getElementById('ci_placa').value;
    const plazaSelect = document.getElementById('ci_plaza_id');
    const plazaId = plazaSelect.value;
    const ingreso = document.getElementById('ci_ingreso').value;

    if (!nombre || !placa || !plazaId || !ingreso) {
        alert("Complete todos los campos de registro.");
        return;
    }
    
    const ingresoISO = getNowDateTimeString(ingreso);
    const codigoPlaza = plazaSelect.options[plazaSelect.selectedIndex].text;

    const reservaData = {
        plazaId: Number(plazaId),
        horaIngreso: ingresoISO,
        horaSalida: null, 
        unsure: true, 
        nombreConductor: nombre,
        placaVehiculo: placa,
        metodoPago: 'Efectivo_Pendiente' 
    };

    // CORRECCIÓN CRÍTICA: La URL debe incluir el cocheraId para la validación de seguridad (404 FIX)
    fetch(`/api/recepcion/${cocheraId}/checkin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reservaData)
    })
    .then(async r => {
        if (!r.ok) {
            const errorText = await r.text();
            let errorMessage = 'Error al registrar check-in.';
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorJson.error || errorMessage;
            } catch (e) {
                errorMessage = errorText;
            }
            throw new Error(errorMessage);
        }
        return r.json();
    })
    .then(() => {
        alert(`Registro manual exitoso en la plaza ${codigoPlaza}. Pago pendiente.`);
        closeCheckinModal();
        cargarPlazas(); 
        cargarReservasActivas(); 
    })
    .catch(error => {
        console.error(error);
        alert('Error en el registro: ' + error.message);
    });
}


function iniciarCheckout(reserva) {
    reservaEnCheckout = reserva.id;
    
    // Calculamos el costo final con cronómetro
    const ahoraLocal = new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    const { hours, total } = calcularTotalPagar(reserva.horaIngreso); 

    const checkoutContentDiv = document.getElementById('checkoutDetailsContent');
    checkoutContentDiv.innerHTML = `
        <h4>Finalizar Pago y Salida</h4>
        <div>
            <p><strong>Reserva #ID:</strong> ${reserva.id}</p>
            <p><strong>Conductor:</strong> ${reserva.usuario ? reserva.usuario.nombreCompleto || reserva.usuario.nombre : 'N/A'}</p>
            <p><strong>Casilla:</strong> ${reserva.plaza.codigo}</p>
            <p><strong>Ingreso:</strong> ${new Date(reserva.horaIngreso).toLocaleString('es-PE')}</p>
            <p><strong>Salida (Ahora):</strong> ${new Date().toLocaleString('es-PE')}</p>
            <hr>
            <p><strong>Tiempo Total (facturado):</strong> ${hours} horas</p>
            <p style="font-size: 1.2em; font-weight: bold; color: #dc3545;">TOTAL A PAGAR: S/. ${total}</p>
        </div>
        
        <label for="checkoutMetodoPago">Método de Pago:</label>
        <select id="checkoutMetodoPago" class="form-control">
            <option value="Efectivo">Efectivo</option>
            <option value="Tarjeta">Tarjeta</option>
            <option value="Yape">Yape</option>
        </select>
        
        <div class="modal-actions">
            <button onclick="confirmarCheckout()" class="role-button recepcionista">Confirmar Pago y Salida</button>
            <button onclick="closeCheckoutModal()" class="role-button">Cancelar</button>
        </div>
    `;

    document.getElementById('checkoutModal').style.display = 'flex';
}

function confirmarCheckout() {
    if (!reservaEnCheckout) return;

    const metodoPago = document.getElementById('checkoutMetodoPago').value;
    
    // CRÍTICO: La URL debe incluir el cocheraId para la validación de seguridad
    fetch(`/api/recepcion/${cocheraId}/checkout/${reservaEnCheckout}?metodoPago=${metodoPago}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(async r => {
        if (!r.ok) {
            const errorText = await r.text();
            let errorMessage = 'Error al finalizar el pago.';
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorJson.error || errorMessage;
            } catch (e) {
                errorMessage = errorText;
            }
            throw new Error(errorMessage);
        }
        return r.json();
    })
    .then(boleta => {
        alert(`Checkout exitoso. Total pagado: S/. ${boleta.monto.toFixed(2)}. Plaza liberada.`);
        closeCheckoutModal();
        cargarReservasActivas(); 
        cargarPlazas(); 
    })
    .catch(error => {
        console.error(error);
        alert('Error al procesar el checkout: ' + error.message);
    });
}

// --- Nuevas funciones para reportes y gestión ---

function cargarCochesRegistrados() {
    fetch(`/api/recepcion/${cocheraId}/coches-registrados`)
        .then(r => {
            if (!r.ok) throw new Error(`HTTP ${r.status}`);
            return r.json();
        })
        .then(data => renderCochesRegistrados(data))
        .catch(err => {
            document.getElementById('seccion-coches').innerHTML = '<p style="color:red;">Error cargando coches registrados.</p>';
            console.error("Error:", err);
        });
}

function renderCochesRegistrados(reservas) {
    const container = document.getElementById('seccion-coches');
    
    // Filtrar solo los registrados manualmente (unsure = true)
    const registrosManual = reservas.filter(r => r.unsure === true);
    
    if (registrosManual.length === 0) {
        container.innerHTML = '<p>No hay coches registrados manualmente en esta cochera.</p>';
        return;
    }

    const tableHtml = `
        <table class="reservas-table" style="width: 100%; margin-top: 20px;">
            <thead>
                <tr>
                    <th>ID Reserva</th>
                    <th>Conductor</th>
                    <th>Placa Vehículo</th>
                    <th>Plaza</th>
                    <th>Ingreso</th>
                    <th>Salida</th>
                    <th>Estado</th>
                    <th>Pago</th>
                </tr>
            </thead>
            <tbody>
                ${registrosManual.map(r => {
                    const conductor = r.usuario ? r.usuario.nombreCompleto || r.usuario.nombre : 'N/A';
                    const placa = r.usuario ? r.usuario.placaVehiculo : 'N/A';
                    const ingreso = new Date(r.horaIngreso).toLocaleString('es-PE');
                    const salida = r.horaSalida ? new Date(r.horaSalida).toLocaleString('es-PE') : 'En curso';
                    const estado = r.activa ? 'Activa' : 'Finalizada';
                    const pago = r.pagado ? `Pagado (${r.metodoPago})` : 'Pendiente';
                    
                    return `
                        <tr>
                            <td>${r.id}</td>
                            <td>${conductor}</td>
                            <td>${placa}</td>
                            <td>${r.plaza.codigo}</td>
                            <td>${ingreso}</td>
                            <td>${salida}</td>
                            <td>${estado}</td>
                            <td>${pago}</td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
    container.innerHTML = tableHtml;
}

function cargarPagosDia() {
    // Placeholder: En una aplicación real, obtendría boletas del día actual
    const container = document.getElementById('seccion-pagosDia');
    container.innerHTML = '<p>No hay registros de pagos completados hoy. Los pagos se registran al realizar checkout.</p>';
}

function cargarReservasAdelantadas() {
    // Placeholder: Reservas con pago anticipado
    const container = document.getElementById('seccion-reservasActivas');
    fetch(`/api/cocheras/${cocheraId}/reservas/activas`)
        .then(r => r.json())
        .then(data => {
            const conPago = data.filter(r => r.pagado);
            if (conPago.length === 0) {
                container.innerHTML = '<p>No hay reservas con pago anticipado.</p>';
                return;
            }
            
            const tableHtml = `
                <table class="reservas-table" style="width: 100%; margin-top: 20px;">
                    <thead>
                        <tr>
                            <th>Placa</th>
                            <th>Conductor</th>
                            <th>Plaza</th>
                            <th>Ingreso</th>
                            <th>Salida Programada</th>
                            <th>Monto Pagado</th>
                            <th>Método</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${conPago.map(r => {
                            const conductor = r.usuario ? r.usuario.nombreCompleto : 'N/A';
                            const placa = r.usuario ? r.usuario.placaVehiculo : 'N/A';
                            const ingreso = new Date(r.horaIngreso).toLocaleTimeString('es-PE');
                            const salida = r.horaSalida ? new Date(r.horaSalida).toLocaleTimeString('es-PE') : 'N/A';
                            
                            return `
                                <tr>
                                    <td>${placa}</td>
                                    <td>${conductor}</td>
                                    <td>${r.plaza.codigo}</td>
                                    <td>${ingreso}</td>
                                    <td>${salida}</td>
                                    <td>S/. 0.00</td>
                                    <td>${r.metodoPago}</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
            container.innerHTML = tableHtml;
        })
        .catch(err => {
            container.innerHTML = '<p style="color:red;">Error cargando reservas adelantadas.</p>';
            console.error(err);
        });
}

function cargarDashboard() {
    fetch(`/api/dashboard/stats`)
        .then(r => r.json())
        .then(data => renderDashboard(data))
        .catch(err => {
            document.getElementById('dashboardContent').innerHTML = '<p style="color:red;">Error cargando dashboard. Intente más tarde.</p>';
            console.error(err);
        });
}

function renderDashboard(datos) {
    const container = document.getElementById('dashboardContent');
    
    // Renderizar un dashboard simple con la data
    const totalIngresos = datos.ingresos.reduce((a, b) => a + b, 0);
    const totalConApp = datos.conApp || 0;
    const totalSinApp = datos.sinApp || 0;
    
    const html = `
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 30px;">
            <div style="background: #007bff; color: white; padding: 20px; border-radius: 5px; text-align: center;">
                <h3>Total Ingresos Semana</h3>
                <p style="font-size: 2em; font-weight: bold;">S/. ${totalIngresos}</p>
            </div>
            <div style="background: #28a745; color: white; padding: 20px; border-radius: 5px; text-align: center;">
                <h3>Reservas Totales</h3>
                <p style="font-size: 2em; font-weight: bold;">${totalConApp + totalSinApp}</p>
            </div>
            <div style="background: #ffc107; color: #333; padding: 20px; border-radius: 5px; text-align: center;">
                <h3>Con Aplicación</h3>
                <p style="font-size: 2em; font-weight: bold;">${totalConApp}</p>
            </div>
            <div style="background: #6c757d; color: white; padding: 20px; border-radius: 5px; text-align: center;">
                <h3>Sin Aplicación</h3>
                <p style="font-size: 2em; font-weight: bold;">${totalSinApp}</p>
            </div>
        </div>
        
        <div style="background: #f8f9fa; padding: 20px; border-radius: 5px;">
            <h4>Ingresos por Día</h4>
            <table class="reservas-table" style="width: 100%; margin-top: 10px; border-collapse: collapse;">
                <tr style="border-bottom: 1px solid #ddd;">
                    <th style="text-align: left; padding: 8px;">Día</th>
                    ${datos.dias.map(d => `<th style="text-align: center; padding: 8px;">${d}</th>`).join('')}
                </tr>
                <tr>
                    <td style="padding: 8px;"><strong>Ingresos</strong></td>
                    ${datos.ingresos.map(i => `<td style="text-align: center; padding: 8px;">S/. ${i}</td>`).join('')}
                </tr>
            </table>
        </div>
    `;
    
    container.innerHTML = html;
}