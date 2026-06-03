// ================================================================
//  cop-input.js — Formateador de inputs en Pesos Colombianos (COP)
// ================================================================
//
//  CÓMO FUNCIONA:
//   - El usuario escribe dígitos → se muestran con puntos de miles
//     Ej: "1500000" → "1.500.000"
//   - Al enviar el formulario, parseCOP() extrae el número real
//     Ej: "1.500.000" → 1500000
//
//  USO:
//   1. Incluir este script después de api.js
//   2. Llamar initCOPInput(element) en cada input de monto
//   3. Usar parseCOP(element.value) para obtener el número antes de enviarlo
// ================================================================

/**
 * Convierte un valor numérico a string con formato COP.
 * 1500000 → "1.500.000"
 */
function toCOPString(n) {
    if (n === null || n === undefined || n === '' || isNaN(n)) return '';
    return Math.floor(Number(n)).toLocaleString('es-CO');
}

/**
 * Parsea un string con formato COP a número.
 * "1.500.000" → 1500000
 * "1500000"   → 1500000
 */
function parseCOP(str) {
    if (!str && str !== 0) return NaN;
    // Eliminar todos los puntos (separadores de miles en COP) y convertir
    let clean = String(str).replace(/\./g, '').replace(',', '.').trim();
    return parseFloat(clean);
}

/**
 * Inicializa un input de texto para formatear automáticamente en COP.
 * Cambia el tipo a "text" con inputmode="numeric" para mostrar el teclado
 * numérico en móviles sin las limitaciones de type="number".
 *
 * @param {HTMLInputElement} el - El elemento input a formatear
 */
function initCOPInput(el) {
    if (!el) return;

    // Cambiar a text para poder mostrar los puntos de miles
    el.type = 'text';
    el.inputMode = 'numeric';
    el.autocomplete = 'off';

    // Actualizar placeholder si está en blanco o es "0"
    if (!el.placeholder || el.placeholder === '0') {
        el.placeholder = 'Ej: 500.000';
    }

    el.addEventListener('input', function () {
        let cursorPos = this.selectionStart;
        let oldLen = this.value.length;

        // Quitar todo lo que no sea dígito
        let rawDigits = this.value.replace(/[^\d]/g, '');

        if (rawDigits === '') {
            this.value = '';
            return;
        }

        let num = parseInt(rawDigits, 10);
        let formatted = num.toLocaleString('es-CO');
        this.value = formatted;

        // Ajustar posición del cursor proporcionalmente
        let newLen = this.value.length;
        let diff = newLen - oldLen;
        let newPos = Math.max(0, cursorPos + diff);
        try { this.setSelectionRange(newPos, newPos); } catch (e) {}
    });

    // Al perder el foco, asegurar formato limpio
    el.addEventListener('blur', function () {
        let raw = parseCOP(this.value);
        if (!isNaN(raw) && raw > 0) {
            this.value = toCOPString(raw);
        } else if (this.value.trim() === '' || isNaN(raw)) {
            this.value = '';
        }
    });

    // Al enfocar, resaltar todo el texto para facilitar reemplazo
    el.addEventListener('focus', function () {
        setTimeout(() => this.select(), 30);
    });
}

/**
 * Inicializa TODOS los inputs con el atributo data-cop="true" en la página.
 * Úsalo al cargar la página: document.addEventListener('DOMContentLoaded', initAllCOPInputs)
 */
function initAllCOPInputs() {
    document.querySelectorAll('[data-cop="true"]').forEach(initCOPInput);
}
