// ================================================================
//  api.js — Módulo de conexión con el Backend (Spring Boot)
// ================================================================
//
//  ¿CÓMO FUNCIONA LA COMUNICACIÓN?
//
//  El frontend (HTML/JS, puerto 8080) se comunica con el backend
//  (Spring Boot, puerto 8081) usando llamadas HTTP con fetch().
//
//  Frontend (navegador)          Backend (servidor Java)
//  ┌─────────────┐    HTTP       ┌─────────────────────┐
//  │  HTML / JS  │ ──fetch()──>  │  Spring Boot (8081) │
//  │  (puerto    │ <──JSON────   │   ↕                 │
//  │   8080)     │               │  MySQL (3307)       │
//  └─────────────┘               └─────────────────────┘
//
//  1. El frontend hace fetch() a una URL del backend (ej: /api/usuarios/login)
//  2. El backend procesa la petición, consulta MySQL, y responde con JSON
//  3. El frontend recibe el JSON y actualiza la página
//
// ================================================================

// URL base del backend (Corriendo localmente en Spring Boot)
const API_BASE = "http://localhost:8081/api";


// ================================================================
//  FUNCIÓN GENÉRICA PARA LLAMAR AL API
// ================================================================
// Todas las funciones de abajo usan esta función internamente.
// Se encarga de:
//   - Agregar el header Content-Type: application/json
//   - Enviar la petición con fetch()
//   - Parsear la respuesta JSON
//   - Lanzar un error si la respuesta no es exitosa (código 4xx/5xx)

async function fetchAPI(endpoint, opciones = {}) {
    const url = API_BASE + endpoint;

    // Configuración de la petición HTTP
    const config = {
        headers: {
            "Content-Type": "application/json",
            "Bypass-Tunnel-Reminder": "true"
        },
        ...opciones  // Spread: agrega method, body, etc. que se pasen como parámetro
    };

    // 1. Intentar conectar con el backend
    let respuesta;
    try {
        respuesta = await fetch(url, config);
    } catch (error) {
        // Si fetch() falla, es un error de red (backend apagado, sin internet, etc.)
        throw new Error(
            "No se puede conectar con el servidor. " +
            "Verifica que el backend esté corriendo en el puerto 8081."
        );
    }

    // 2. Intentar leer la respuesta como JSON
    let datos;
    try {
        datos = await respuesta.json();
    } catch (e) {
        // Si no se puede parsear como JSON
        if (!respuesta.ok) {
            throw new Error("Error del servidor (código " + respuesta.status + ")");
        }
        return null;
    }

    // 3. Si el servidor respondió con error (4xx, 5xx), lanzar excepción
    if (!respuesta.ok) {
        throw new Error(datos.error || "Error en el servidor");
    }

    // 4. Todo bien → devolver los datos
    return datos;
}


// ================================================================
//  USUARIOS — Registro y Login
// ================================================================

/**
 * Registrar un usuario nuevo.
 * Envía: POST /api/usuarios/registro
 * Body: { nombre, apellido, contrasena, tipoDoc, nDoc, universidad }
 * Responde: el objeto Usuario creado
 */
async function registrarUsuario(datos) {
    return fetchAPI("/usuarios/registro", {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

/**
 * Iniciar sesión.
 * Envía: POST /api/usuarios/login
 * Body: { nombre, contrasena }
 * Responde: el objeto Usuario si las credenciales son correctas
 * Lanza error si el usuario o contraseña son incorrectos
 */
async function loginUsuario(nombre, contrasena) {
    return fetchAPI("/usuarios/login", {
        method: "POST",
        body: JSON.stringify({ nombre, contrasena })
    });
}


// ================================================================
//  SEMESTRE — Configuración del período académico
// ================================================================

/**
 * Obtener configuración del semestre actual.
 * GET /api/semestre/{usuarioId}
 * Responde: { semestreActual, fechaInicioSemestre, fechaFinSemestre, porcentajeAhorro, montoAhorrado }
 */
async function getSemestre(usuarioId) {
    return fetchAPI("/semestre/" + usuarioId);
}

/**
 * Configurar semestre y ahorro automático.
 * PUT /api/semestre/{usuarioId}
 * Body: { semestreActual: 2, fechaInicioSemestre: "2026-01-15", fechaFinSemestre: "2026-06-30", porcentajeAhorro: 15 }
 */
async function configurarSemestre(usuarioId, datos) {
    return fetchAPI("/semestre/" + usuarioId, {
        method: "PUT",
        body: JSON.stringify(datos)
    });
}

/**
 * Agregar ahorro manual.
 * POST /api/semestre/{usuarioId}/ahorrar
 * Body: { monto: 50000 }
 */
async function agregarAhorro(usuarioId, monto) {
    return fetchAPI("/semestre/" + usuarioId + "/ahorrar", {
        method: "POST",
        body: JSON.stringify({ monto })
    });
}

/**
 * Retirar ahorro.
 * POST /api/semestre/{usuarioId}/retirar-ahorro
 * Body: { monto: 20000 }
 */
async function retirarAhorro(usuarioId, monto) {
    return fetchAPI("/semestre/" + usuarioId + "/retirar-ahorro", {
        method: "POST",
        body: JSON.stringify({ monto })
    });
}


// ================================================================
//  INGRESOS — CRUD
// ================================================================

/**
 * Obtener todos los ingresos de un usuario.
 * GET /api/ingresos/{usuarioId}
 * Responde: array de objetos Ingreso
 */
async function getIngresos(usuarioId) {
    return fetchAPI("/ingresos/" + usuarioId);
}

/**
 * Obtener ingresos por semestre.
 * GET /api/ingresos/{usuarioId}/semestre/{semestre}
 */
async function getIngresosPorSemestre(usuarioId, semestre) {
    return fetchAPI("/ingresos/" + usuarioId + "/semestre/" + semestre);
}

/**
 * Agregar un ingreso nuevo (con ahorro automático si está configurado).
 * POST /api/ingresos/{usuarioId}
 * Body: { monto, descripcion, fecha, categoria }
 */
async function agregarIngreso(usuarioId, datos) {
    return fetchAPI("/ingresos/" + usuarioId, {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

/**
 * Eliminar un ingreso por su ID.
 * DELETE /api/ingresos/{id}
 */
async function eliminarIngreso(id) {
    return fetchAPI("/ingresos/" + id, { method: "DELETE" });
}

/**
 * Obtener el total de ingresos de un usuario.
 * GET /api/ingresos/{usuarioId}/total
 * Responde: { total: 12345.0 }
 */
async function getTotalIngresos(usuarioId) {
    const resp = await fetchAPI("/ingresos/" + usuarioId + "/total");
    return resp.total;
}

/**
 * Obtener total de ingresos por semestre.
 * GET /api/ingresos/{usuarioId}/total/semestre/{semestre}
 */
async function getTotalIngresosPorSemestre(usuarioId, semestre) {
    return fetchAPI("/ingresos/" + usuarioId + "/total/semestre/" + semestre);
}


// ================================================================
//  GASTOS — CRUD
// ================================================================

/**
 * Obtener todos los gastos de un usuario.
 * GET /api/gastos/{usuarioId}
 */
async function getGastos(usuarioId) {
    return fetchAPI("/gastos/" + usuarioId);
}

/**
 * Obtener gastos por semestre.
 * GET /api/gastos/{usuarioId}/semestre/{semestre}
 */
async function getGastosPorSemestre(usuarioId, semestre) {
    return fetchAPI("/gastos/" + usuarioId + "/semestre/" + semestre);
}

/**
 * Agregar un gasto nuevo (sin asociar a ingreso).
 * POST /api/gastos/{usuarioId}
 * Body: { monto, descripcion, fecha, categoria }
 */
async function agregarGasto(usuarioId, datos) {
    return fetchAPI("/gastos/" + usuarioId, {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

/**
 * Eliminar un gasto por su ID.
 * DELETE /api/gastos/{id}
 */
async function eliminarGasto(id) {
    return fetchAPI("/gastos/" + id, { method: "DELETE" });
}

/**
 * Obtener el total de gastos de un usuario.
 * GET /api/gastos/{usuarioId}/total
 * Responde: { total: 5678.0 }
 */
async function getTotalGastos(usuarioId) {
    const resp = await fetchAPI("/gastos/" + usuarioId + "/total");
    return resp.total;
}

/**
 * Obtener total de gastos por semestre.
 * GET /api/gastos/{usuarioId}/total/semestre/{semestre}
 */
async function getTotalGastosPorSemestre(usuarioId, semestre) {
    return fetchAPI("/gastos/" + usuarioId + "/total/semestre/" + semestre);
}

/**
 * Obtener resumen semanal (para notificaciones).
 * GET /api/gastos/{usuarioId}/resumen-semanal
 * Responde: { fechaInicio, fechaFin, totalGastado, numeroGastos, gastos[], porCategoria: {} }
 */
async function getResumenSemanal(usuarioId) {
    return fetchAPI("/gastos/" + usuarioId + "/resumen-semanal");
}

/**
 * Obtener gastos semanales por categoría.
 * GET /api/gastos/{usuarioId}/semanal/categoria?fecha=2026-04-22
 */
async function getGastosSemanalesPorCategoria(usuarioId, fecha) {
    const param = fecha ? "?fecha=" + fecha : "";
    return fetchAPI("/gastos/" + usuarioId + "/semanal/categoria" + param);
}


// ================================================================
//  METAS DE AHORRO — CRUD
// ================================================================

/**
 * Obtener todas las metas de ahorro de un usuario.
 * GET /api/metas/{usuarioId}
 */
async function getMetas(usuarioId) {
    return fetchAPI("/metas/" + usuarioId);
}

/**
 * Verificar descuentos de productos asociados a metas (DummyJSON).
 * GET /api/metas/{usuarioId}/descuentos
 */
async function verificarDescuentos(usuarioId) {
    return fetchAPI("/metas/" + usuarioId + "/descuentos");
}

/**
 * Crear una nueva meta de ahorro.
 * POST /api/metas/{usuarioId}
 * Body: { nombre, montoObjetivo, fechaLimite }
 */
async function crearMeta(usuarioId, datos) {
    return fetchAPI("/metas/" + usuarioId, {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

/**
 * Abonar dinero a una meta existente.
 * PUT /api/metas/{id}/abonar
 * Body: { monto }
 */
async function abonarMeta(id, monto) {
    return fetchAPI("/metas/" + id + "/abonar", {
        method: "PUT",
        body: JSON.stringify({ monto: monto })
    });
}

/**
 * Eliminar una meta de ahorro.
 * DELETE /api/metas/{id}
 */
async function eliminarMeta(id) {
    return fetchAPI("/metas/" + id, { method: "DELETE" });
}

// ── Seguimiento Inteligente ML ──

/**
 * Buscar productos en Mercado Libre.
 * POST /api/metas/{usuarioId}/buscar-productos
 */
async function buscarProductosML(usuarioId, query) {
    return fetchAPI("/metas/" + usuarioId + "/buscar-productos", {
        method: "POST",
        body: JSON.stringify({ query: query })
    });
}

/**
 * Activar o actualizar seguimiento inteligente de un producto en ML.
 * PUT /api/metas/{id}/activar-seguimiento
 */
async function activarSeguimiento(id, externalProductId, precioObjetivoCompra, activo = true) {
    return fetchAPI("/metas/" + id + "/activar-seguimiento", {
        method: "PUT",
        body: JSON.stringify({
            externalProductId: externalProductId,
            precioObjetivoCompra: precioObjetivoCompra,
            activo: activo
        })
    });
}

/**
 * Obtener el historial de precios de una meta.
 * GET /api/metas/{id}/historial-precios
 */
async function getHistorialPrecios(id) {
    return fetchAPI("/metas/" + id + "/historial-precios");
}

// ================================================================
//  NOTIFICACIONES — Notificaciones del sistema
// ================================================================

/**
 * Obtener las notificaciones del usuario.
 * GET /api/notificaciones/{usuarioId}
 */
async function getNotificaciones(usuarioId) {
    return fetchAPI("/notificaciones/" + usuarioId);
}

/**
 * Marcar una notificación como leída.
 * PUT /api/notificaciones/{id}/leer
 */
async function marcarNotificacionLeida(id) {
    return fetchAPI("/notificaciones/" + id + "/leer", { method: "PUT" });
}

/**
 * Contar las notificaciones no leídas de un usuario.
 * GET /api/notificaciones/{usuarioId}/no-leidas/count
 */
async function getNotificacionesNoLeidasCount(usuarioId) {
    const resp = await fetchAPI("/notificaciones/" + usuarioId + "/no-leidas/count");
    return resp.count;
}


// ================================================================
//  MONEDA — Conversión de cambio
// ================================================================

/**
 * Convertir un monto de una moneda a otra.
 * POST /api/moneda/convertir
 * Body: { monto, monedaOrigen, monedaDestino }
 * Responde: { montoOriginal, monedaOrigen, montoConvertido, monedaDestino, tasa }
 */
async function convertirMoneda(monto, monedaOrigen, monedaDestino) {
    return fetchAPI("/moneda/convertir", {
        method: "POST",
        body: JSON.stringify({ monto, monedaOrigen, monedaDestino })
    });
}


// ================================================================
//  SESIÓN — Manejo del usuario logueado
// ================================================================
// Usamos sessionStorage para guardar el ID y nombre del usuario.
// sessionStorage se borra cuando se cierra la pestaña del navegador.

/**
 * Guardar los datos del usuario en la sesión del navegador.
 * Se llama después de un login exitoso.
 */
function guardarSesion(usuario) {
    sessionStorage.setItem("usuarioId", usuario.id);
    sessionStorage.setItem("nombreUsuario", usuario.nombre);
}

/**
 * Obtener el ID del usuario logueado.
 * Retorna null si no hay sesión activa.
 */
function getUsuarioId() {
    return sessionStorage.getItem("usuarioId");
}

/**
 * Obtener el nombre del usuario logueado.
 */
function getNombreUsuario() {
    return sessionStorage.getItem("nombreUsuario");
}

/**
 * Cerrar sesión: limpia sessionStorage y redirige al login.
 */
function cerrarSesion() {
    sessionStorage.clear();
    window.location.href = "index.html";
}

/**
 * Verificar si hay un usuario logueado.
 * Si no hay sesión, redirige automáticamente al login.
 * Se llama al inicio de cada página protegida (menu, ingresos, gastos).
 * Si el usuario está en primeraVez y no está en semestre.html, redirige a semestre.
 */
async function verificarSesion() {
    if (!getUsuarioId()) {
        window.location.href = "index.html";
        return;
    }
    // Si es primera vez y no está en semestre.html, verificar si realmente necesita configurar
    let esPrimera = sessionStorage.getItem("primeraVez") === "true";
    let estaEnSemestre = window.location.href.includes("semestre.html");
    if (esPrimera && !estaEnSemestre) {
        // Verificar con el backend si ya tiene semestre configurado
        try {
            let semestre = await getSemestre(getUsuarioId());
            if (semestre && semestre.fechaInicioSemestre) {
                // Ya tiene semestre configurado, limpiar flag y continuar
                sessionStorage.removeItem("primeraVez");
                return; // No redirigir, dejar que cargue la página actual
            }
        } catch (e) {
            // Si falla la consulta, no redirigir para evitar loops
            console.error("Error verificando semestre:", e);
            return;
        }
        window.location.href = "semestre.html";
    }
}

// ================================================================
//  REPORTES — Guardado de snapshots
// ================================================================

/**
 * Calcular el valor promedio diario de gasto usando Regla del Trapecio
 * (Calculo Integral).
 * GET /api/reportes/{usuarioId}/promedio-diario?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD
 */
async function getPromedioDiarioIntegral(usuarioId, fechaInicio, fechaFin) {
    return fetchAPI("/reportes/" + usuarioId + "/promedio-diario" +
        "?fechaInicio=" + fechaInicio + "&fechaFin=" + fechaFin);
}

/**
 * Guardar un reporte semanal o mensual
 * POST /api/reportes/{usuarioId}
 */
async function guardarReporte(usuarioId, datos) {
    return fetchAPI("/reportes/" + usuarioId, {
        method: "POST",
        body: JSON.stringify(datos)
    });
}

/**
 * Obtener todos los reportes guardados
 * GET /api/reportes/{usuarioId}
 */
async function getReportesGuardados(usuarioId) {
    return fetchAPI("/reportes/" + usuarioId);
}

/**
 * Eliminar un reporte guardado
 * DELETE /api/reportes/{id}
 */
async function eliminarReporte(id) {
    return fetchAPI("/reportes/" + id, { method: "DELETE" });
}


// ================================================================
//  SEMESTRE — Finalizar e historial
// ================================================================

/**
 * Finalizar el semestre actual.
 * Guarda un snapshot con totales y resetea ingresos, gastos y ahorro a 0.
 * POST /api/semestre/{usuarioId}/finalizar
 */
async function finalizarSemestre(usuarioId, asignaturasPerdidas = 0) {
    return fetchAPI("/semestre/" + usuarioId + "/finalizar", {
        method: "POST",
        body: JSON.stringify({ asignaturasPerdidas })
    });
}

/**
 * Obtener historial de semestres finalizados.
 * GET /api/semestre/{usuarioId}/historial
 */
async function getHistorialSemestres(usuarioId) {
    return fetchAPI("/semestre/" + usuarioId + "/historial");
}