# Walkthrough: Integración Frontend ↔ Backend — FinanzasApp

## Arquitectura General

Así funciona la comunicación entre tu frontend y backend:

```
┌──────────────────────┐         HTTP (fetch)         ┌───────────────────────────┐
│   FRONTEND           │ ──────────────────────────>   │   BACKEND                 │
│   HTML / CSS / JS    │                               │   Spring Boot (Java)      │
│   Puerto: 8080       │ <─────── JSON ────────────    │   Puerto: 8081            │
│                      │                               │   ↕                       │
│   (Navegador)        │                               │   MySQL (Puerto: 3307)    │
└──────────────────────┘                               └───────────────────────────┘
```

**El frontend ya NO guarda datos en localStorage.** Ahora todo se guarda en la base de datos MySQL a través del backend.

---

## Cambios Realizados — Paso a Paso

---

### PASO 1: Cambiar el puerto del backend

#### [application.properties](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/resources/application.properties)

render_diffs(file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/resources/application.properties)

> [!IMPORTANT]
> **¿Por qué?** Tanto el frontend como el backend usaban el **mismo puerto 8080**. Dos programas no pueden usar el mismo puerto al mismo tiempo. El frontend se queda en 8080 y el backend ahora usa 8081.

---

### PASO 2: Habilitar CORS en Spring Security

#### [SecurityConfig.java](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/SecurityConfig.java)

render_diffs(file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/SecurityConfig.java)

> [!NOTE]
> **¿Qué es CORS?** Cuando el frontend (en `localhost:8080`) intenta llamar al backend (en `localhost:8081`), el navegador lo bloquea por seguridad — son "orígenes cruzados" (Cross-Origin). La línea `.cors(Customizer.withDefaults())` le dice a Spring Security que permita estas peticiones.

---

### PASO 3: Endpoint simplificado de gastos

#### [GastoService.java](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/service/GastoService.java)

render_diffs(file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/service/GastoService.java)

#### [GastoController.java](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/controller/GastoController.java)

render_diffs(file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/controller/GastoController.java)

> [!NOTE]
> **¿Por qué un endpoint nuevo?** El endpoint original `POST /api/gastos/{usuarioId}/ingreso/{ingresoId}` **requería** asociar cada gasto a un ingreso específico. Esto complicaba el frontend. El nuevo endpoint `POST /api/gastos/{usuarioId}` permite registrar gastos sin esa restricción.

---

### PASO 4: Ocultar contraseña en respuestas JSON

#### [Usuario.java](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/model/Usuario.java)

render_diffs(file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/finanzas-backend/finanzas-backend/src/main/java/com/finanzas/finanzas/model/Usuario.java)

> [!TIP]
> **`@JsonProperty(access = WRITE_ONLY)`** significa:
> - ✅ El backend **acepta** la contraseña cuando le llega (registro, login)
> - ❌ El backend **nunca la incluye** en las respuestas JSON
> 
> Esto es una buena práctica de seguridad.

---

### PASO 5: Crear el módulo api.js (el puente entre frontend y backend)

#### [api.js](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/js/api.js) — ARCHIVO NUEVO

Este es el archivo más importante de la integración. Contiene:

| Función | ¿Qué hace? | Endpoint del backend |
|---------|------------|---------------------|
| `fetchAPI()` | Función genérica que envía peticiones HTTP | N/A (función base) |
| `registrarUsuario(datos)` | Registrar usuario nuevo | `POST /api/usuarios/registro` |
| `loginUsuario(nombre, contrasena)` | Iniciar sesión | `POST /api/usuarios/login` |
| `getIngresos(usuarioId)` | Listar ingresos | `GET /api/ingresos/{id}` |
| `agregarIngreso(usuarioId, datos)` | Crear ingreso | `POST /api/ingresos/{id}` |
| `eliminarIngreso(id)` | Borrar ingreso | `DELETE /api/ingresos/{id}` |
| `getTotalIngresos(usuarioId)` | Suma total | `GET /api/ingresos/{id}/total` |
| `getGastos(usuarioId)` | Listar gastos | `GET /api/gastos/{id}` |
| `agregarGasto(usuarioId, datos)` | Crear gasto | `POST /api/gastos/{id}` |
| `eliminarGasto(id)` | Borrar gasto | `DELETE /api/gastos/{id}` |
| `getTotalGastos(usuarioId)` | Suma total | `GET /api/gastos/{id}/total` |
| `guardarSesion(usuario)` | Guardar sesión en sessionStorage | N/A (local) |
| `getUsuarioId()` | Obtener ID del usuario logueado | N/A (local) |
| `cerrarSesion()` | Cerrar sesión y redirigir | N/A (local) |
| `verificarSesion()` | Verificar si hay sesión activa | N/A (local) |

> [!IMPORTANT]
> **¿Cómo funciona `fetchAPI()`?**
> ```javascript
> // 1. Construye la URL completa: http://localhost:8081/api + endpoint
> // 2. Envía la petición HTTP con fetch()
> // 3. Lee la respuesta JSON del backend
> // 4. Si hay error (4xx, 5xx), lanza una excepción
> // 5. Si todo OK, devuelve los datos
> ```

---

### PASO 6: Reescribir las páginas HTML

#### Mapeo de campos Frontend ↔ Backend

Los nombres de los campos cambiaron para coincidir con el modelo Java:

| Frontend (antes) | Backend (ahora) | Ejemplo |
|-----------------|----------------|---------|
| `nombre` | `descripcion` | "Salario mensual" |
| `valor` | `monto` | 5000.00 |
| `categoria` | `categoria` | "Salario" (sin cambio) |
| `fecha` | `fecha` | "2026-04-08" (sin cambio) |
| índice del array | `id` | 15 (ID de la BD) |

#### Login ([index.html](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/index.html))
- **Antes:** Comparaba contra `localStorage.getItem("cedula")`
- **Ahora:** Envía `POST /api/usuarios/login` con `{nombre, contrasena}`
- El campo "Cédula" se cambió a "Contraseña" (`type="password"`)
- Si el login es exitoso, guarda `usuarioId` en `sessionStorage`

#### Registro ([registro.html](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/registro.html))
- **Antes:** Solo 4 campos, guardaba en localStorage
- **Ahora:** 7 campos que coinciden con el modelo `Usuario` de Java:
  - Nombre de usuario, Apellido, Contraseña, Confirmar contraseña, Tipo doc, Nº doc, Universidad
- Envía `POST /api/usuarios/registro`

#### Ingresos ([ingresos.html](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/ingresos.html))
- **Cargar:** `GET /api/ingresos/{usuarioId}` en vez de `localStorage.getItem`
- **Agregar:** `POST /api/ingresos/{usuarioId}` en vez de `localStorage.setItem`
- **Eliminar:** `DELETE /api/ingresos/{id}` en vez de `splice` en array

#### Gastos ([gastos.html](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/gastos.html))
- Exactamente igual que ingresos, pero con endpoints de `/api/gastos/`

#### Dashboard ([menu.html](file:///c:/Users/Reinel V/Downloads/Proyecto-aula/Proyecto-aula/frontend/menu.html))
- **Totales:** Usa `GET /api/ingresos/{id}/total` y `GET /api/gastos/{id}/total`
- **Últimos movimientos:** Combina resultados de ambos endpoints, ordena por fecha

---

## Cómo Probar

### 1. Iniciar el backend
```bash
cd finanzas-backend/finanzas-backend
mvnw spring-boot:run
```
> El backend debe iniciar en el **puerto 8081**. Verifica que MySQL esté corriendo en el puerto 3307.

### 2. Abrir el frontend
Abre `frontend/index.html` en el navegador (puerto 8080 con Live Server o similar).

### 3. Flujo de prueba
1. Ir a **Registro** → crear una cuenta con todos los campos
2. Ir a **Login** → iniciar sesión con nombre + contraseña
3. En **Ingresos** → agregar un ingreso y verificar que aparece
4. En **Gastos** → agregar un gasto y verificar que aparece
5. En **Inicio** → verificar que los totales y últimos movimientos son correctos
6. **Cerrar sesión** → verificar que redirige al login

---

## Archivos Modificados

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `application.properties` | Backend | Puerto 8080 → 8081 |
| `SecurityConfig.java` | Backend | Habilitar CORS |
| `GastoService.java` | Backend | Método `registrarSimple` |
| `GastoController.java` | Backend | Endpoint `POST /{usuarioId}` |
| `Usuario.java` | Backend | `@JsonProperty` en contraseña |
| `js/api.js` | Frontend | **NUEVO** — módulo de conexión |
| `css/estilos.css` | Frontend | Estilos para select en registro |
| `index.html` | Frontend | Login con fetch |
| `registro.html` | Frontend | 7 campos + fetch |
| `ingresos.html` | Frontend | fetch en vez de localStorage |
| `gastos.html` | Frontend | fetch en vez de localStorage |
| `menu.html` | Frontend | fetch en vez de localStorage |
