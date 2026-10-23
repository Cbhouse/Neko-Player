# Diseño y movimiento

Escala de referencia: 698 px de ancho. Se convierte al ancho físico de la pantalla.
Fuente nativa sans-serif; regular, medium y bold. Android conserva su escala de texto.

| Componente | Medidas de referencia | Estados |
| --- | --- | --- |
| Encabezado | 110 alto, logo 91 × 88, ajustes 66 × 66 | botón enfocable |
| Hero | 390 alto, radio 34 | estático o gatito/notas animados |
| Acción de archivo | 138 alto, radio 31, badge 100 | normal, pulsado, focus, ripple |
| Reproducción | 216 alto, tarjeta interior 130 | vacío, preparando, reproduciendo, pausa, video |
| Fuentes futuras | 168 alto, radio 30 | informativo |
| Barra inferior | mínimo 84 + padding | pestaña activa, inactiva, pulsada |

Movimiento: personalidad alegre y suave; entrada 340 ms con desaceleración y
escalonado de 55 ms; pulsación 130 ms, recuperación 220 ms; ambiente de 4200 ms
con desplazamientos sinusoidales pequeños. Se cancelan animadores al salir de la
ventana o pausar la Activity. Ajustes permite desactivarlos y se respeta la escala
de duración del sistema. El contenido sigue funcionando sin animaciones.

El morado del botón principal es ligeramente más oscuro que la referencia para
mantener el contraste del texto blanco. Logo y mascota son ilustraciones nuevas.
Los equivalentes nativos de los tokens viven en `NekoStyle.java`; las medidas
proporcionales se encuentran en los componentes Android, sin una capa CSS.
