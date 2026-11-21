# FinanzasApp (Finanzas Estudiantiles)

FinanzasApp es una aplicación web full-stack diseñada para ayudar a los estudiantes a gestionar sus finanzas, realizar un seguimiento de sus ingresos y gastos, establecer metas de ahorro y monitorear su progreso financiero a lo largo del semestre académico.

## 🚀 Tecnologías Utilizadas

### Frontend
*   **HTML5, CSS3, JavaScript (Vanilla)**
*   **Diseño Responsivo:** Interfaz moderna y adaptable con tema claro profesional.
*   **Gestión de Estado:** Uso de `sessionStorage` para el manejo de sesiones de usuario.

### Backend
*   **Java 17**
*   **Spring Boot 3.2.0**
*   **Spring Web** (API RESTful)
*   **Spring Data JPA** (Persistencia de datos con Hibernate)
*   **Lombok** (Reducción de código repetitivo)

### Base de Datos
*   **MySQL** (Puerto 3307)

---

## 📂 Estructura del Proyecto

El repositorio está dividido principalmente en dos carpetas:

```text
├── frontend/                     # Código fuente del Frontend (HTML, CSS, JS)
│   ├── index.html                # Página de inicio / Login / Registro
│   ├── dashboard.html            # Panel principal
│   ├── ingresos.html             # Gestión de ingresos
│   ├── gastos.html               # Gestión de gastos
│   ├── metas.html                # Metas de ahorro
│   ├── semestre.html             # Configuración del semestre
│   ├── reportes.html             # Reportes y análisis integral
│   ├── css/                      # Hojas de estilo
│   └── js/
│       └── api.js                # Cliente API centralizado
├── finanzas-backend/             # Código fuente del Backend (Spring Boot)
│   └── finanzas-backend/
│       ├── src/main/java/...     # Controladores, Servicios, Modelos y Repositorios
│       ├── src/main/resources/
│       │   ├── application.properties # Configuración de Spring Boot
│       │   └── schema.sql        # Script de inicialización de la Base de Datos
│       └── pom.xml               # Dependencias de Maven
├── backend/                      # (Obsoleto/No utilizado - No modificar)
└── FinanzasApp_Postman.json      # Colección de Postman para probar la API
```

---

## 🛠️ Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

1.  **Java Development Kit (JDK) 17** o superior.
2.  **Maven** (El proyecto incluye el wrapper `mvnw`, por lo que no es estrictamente necesario instalarlo globalmente).
3.  **MySQL Server**. Debe estar configurado para ejecutarse en el puerto **3307**.
4.  Un servidor web local para el frontend (como la extensión **Live Server** de VS Code) o simplemente puedes abrir el archivo HTML directamente en el navegador.

---

## 🐳 Ejecutar con Docker (recomendado)

Con Docker no necesitas instalar Java, Maven ni MySQL. Un solo comando levanta los tres servicios:

```bash
docker compose up -d --build
```

| Servicio   | URL / Puerto                       | Imagen                         |
|------------|------------------------------------|--------------------------------|
| Frontend   | http://localhost:8080              | `nginx:1.27-alpine`            |
| Backend    | http://localhost:8081/api          | build de `finanzas-backend/…`  |
| MySQL      | `localhost:3307` (root / 1234)     | `mysql:8.4`                    |

*   La base de datos se crea automáticamente con `schema.sql` la **primera vez** (cuando el volumen está vacío). Los datos persisten en el volumen `db_data`.
*   Para cambiar puertos o contraseñas, copia `.env.example` a `.env` y edítalo. Si cambias `BACKEND_PORT`, actualiza también `API_BASE` en `frontend/js/api.js`.

Comandos útiles:

```bash
docker compose logs -f backend   # ver logs del backend
docker compose down              # detener (conserva los datos)
docker compose down -v           # detener y BORRAR la base de datos
docker compose up -d --build backend   # reconstruir solo el backend tras cambios
```

---

## ⚙️ Configuración e Instalación

### 1. Configuración de la Base de Datos

Primero, debes crear la base de datos en MySQL. El backend intentará actualizar las tablas (gracias a `hibernate.ddl-auto=update`), pero la base de datos `finanzas_estudiantiles` debe existir.

Abre tu cliente de MySQL (o consola) y ejecuta el script proporcionado:

```bash
mysql -u root -p1234 -P 3307 < finanzas-backend/finanzas-backend/src/main/resources/schema.sql
```

*Nota:* Si tienes credenciales diferentes (usuario/contraseña diferentes a `root` y `1234`), deberás actualizar el archivo `application.properties` en el backend antes de iniciarlo.

### 2. Ejecutar el Backend (Spring Boot)

Abre una terminal, navega a la carpeta del backend y ejecuta la aplicación usando el wrapper de Maven:

```bash
cd finanzas-backend/finanzas-backend

# En Windows:
.\mvnw spring-boot:run

# En Linux/Mac:
./mvnw spring-boot:run
```

El servidor backend se iniciará y estará escuchando en **`http://localhost:8081`**.

*(Opcional) Para empaquetar el proyecto en un archivo JAR:*
```bash
.\mvnw package
```

### 3. Ejecutar el Frontend

Dado que el frontend no utiliza herramientas de compilación (como Webpack o Vite), puedes ejecutarlo de dos maneras:

*   **Opción A (Recomendada):** Usa la extensión **Live Server** en Visual Studio Code. Haz clic derecho sobre el archivo `frontend/index.html` y selecciona "Open with Live Server". Se abrirá en el puerto **`8080`** por defecto.
*   **Opción B:** Simplemente arrastra o haz doble clic en el archivo `frontend/index.html` para abrirlo en tu navegador (`file://...`).

El frontend está configurado para comunicarse con la API en `http://localhost:8081/api` (definido en `frontend/js/api.js`).

---

## 📡 Endpoints de la API Principal

La API está montada bajo `/api` en el puerto `8081`. Algunos de los endpoints más importantes son:

*   **Usuarios:**
    *   `POST /api/usuarios/registro` - Registrar un nuevo usuario.
    *   `POST /api/usuarios/login` - Iniciar sesión.
*   **Transacciones (Ingresos y Gastos):**
    *   `GET /api/ingresos/{usuarioId}` - Obtener ingresos de un usuario.
    *   `POST /api/gastos` - Registrar un gasto.
*   **Metas de Ahorro:**
    *   `GET /api/metas/{usuarioId}` - Listar metas.
    *   `POST /api/metas` - Crear una nueva meta.
    *   `PUT /api/metas/{id}/abonar` - Sumar fondos a una meta específica.
*   **Semestre Académico:**
    *   `PUT /api/semestre/{usuarioId}` - Configurar fechas y presupuesto del semestre.
    *   `POST /api/semestre/finalizar` - Finalizar el semestre actual.

Para ver todos los endpoints con detalle, puedes importar la colección de pruebas que se encuentra en `FinanzasApp_Postman.json`.

---

## 🔒 Notas de Arquitectura y Seguridad

*   **Autenticación:** La aplicación maneja el registro y el inicio de sesión manualmente mediante el `UsuarioService`. En esta versión, Spring Security está configurado con `permitAll()`, por lo que las peticiones a la API no requieren tokens (JWT).
*   **CORS:** Cross-Origin Resource Sharing está habilitado globalmente en `SecurityConfig.java` para permitir que el frontend interactúe con el backend desde distintos orígenes (por ejemplo, puerto 8080 al 8081).
*   **Sesiones Frontend:** Las sesiones de los usuarios activos se manejan a través del `sessionStorage` del navegador, garantizando que los datos temporales se limpien al cerrar la pestaña.
*   **Contraseñas:** En esta versión, por motivos de desarrollo académico, las contraseñas se gestionan en texto plano. La propiedad de contraseñas de los usuarios no se envía en las respuestas JSON hacia el frontend gracias a `@JsonProperty(access = WRITE_ONLY)`.
