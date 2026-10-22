# Neko Player — Android starter 0.2.0

Proyecto Android nativo mínimo en Java, preparado para abrirse y compilarse con Android Studio.

## Qué funciona en esta versión
- Interfaz lavanda inspirada en el diseño de Neko Player, con navegación inferior y tarjetas redondeadas.
- Gatito dibujado y animado dentro de la pantalla del reproductor.
- Selección y reproducción de audio local, con barra de progreso, pausa y detener.
- Selección y reproducción de video local mediante los controles de Android.
- Comprobación de nuevas versiones publicadas en GitHub y descarga de la APK para instalarla con Android.

## Qué todavía no está implementado
- Conexión real con YouTube o Spotify.
- Subtítulos, selección de calidad de streaming o gato flotante sobre otras apps.
- Publicación inicial firmada. Sigue `INSTRUCCIONES-ACTUALIZADOR.md` para configurar la clave privada en GitHub.

## Compilar el APK
1. Instala Android Studio y un JDK 17.
2. Abre la carpeta `NekoPlayer` en Android Studio.
3. Permite que Gradle sincronice y descargue Android Gradle Plugin y el SDK de Android 35.
4. En el menú selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio mostrará el enlace a `app-debug.apk` cuando la compilación termine.

Para publicar una actualización, configura los secretos de firma y ejecuta **Publicar actualización de Neko Player** en GitHub Actions. Conserva una copia privada de la clave de firma.

## Próximos pasos
1. Compilar y probar el reproductor local.
2. Añadir pruebas y gestión de errores.
3. Revisar las vías oficiales permitidas para integraciones de YouTube/Spotify.
4. Publicar versiones firmadas con GitHub Releases. Android abrirá su instalador y puede pedir confirmación para completar cada actualización.
