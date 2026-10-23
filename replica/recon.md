# Neko Player: menú de referencia

Alcance autorizado: reconstruir el inicio de la aplicación Android existente según
`screens/home-reference.png`, crear un logo con ImageGen y agregar animaciones.
Se conserva el nombre, la paleta y los textos solicitados por el usuario.

Referencia de 698 × 1471: encabezado con logo y ajustes; hero con gatito y mensaje;
acciones de audio y video; estado de reproducción; aviso de fuentes futuras;
decoración musical; navegación Inicio / Reproductor / Ajustes.

Flujos: Inicio → selector Android de audio/video → reproductor existente.
Inicio → ajustes mediante encabezado o barra inferior. Estado vacío y estado de
archivo seleccionado se alimentan del reproductor real. Cancelar el selector
conserva la pantalla. YouTube y Spotify son un aviso, no integraciones activas.

La implementación anterior mostraba una captura estirada con regiones táctiles.
La nueva usa controles nativos, textos accesibles, desplazamiento y arte separado.
