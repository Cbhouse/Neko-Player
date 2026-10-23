# Implementación Android

Se mantiene Java, Activity, MediaPlayer, VideoView, AndroidX FileProvider y Gradle.
No se agrega un backend ni una capa web.

- `NekoHomeView`: inicio, acciones accesibles, estado real y animación ambiental.
- `NekoNavigation`: barra inferior compartida entre las tres pantallas.
- `NekoIcon`: iconos independientes de las fuentes y de la resolución.
- `NekoStyle`: colores, tipografías, ripple, movimiento y preferencia persistente.
- `MainActivity`: selección/reproducción existente y conexión con el nuevo inicio.

Las medidas usan las proporciones de la referencia; el contenido se desplaza
cuando el alto disponible es menor y la barra inferior permanece fuera del scroll.
Se aplican los insets del sistema en Android 15. El arte se genera aparte del texto.
No se requieren CSS, Tailwind, servicios nuevos ni librerías de animación.
