# Neko Player — Android starter 0.1.0

Proyecto Android nativo mínimo en Java, preparado para abrirse y compilarse con Android Studio.

## Qué funciona en esta versión
- Interfaz sencilla con estética de gatitos y colores pastel.
- Selección de un archivo de audio local mediante el selector seguro de Android.
- Reproducción, pausa y detener/volver al inicio.
- Pantalla informativa de versión/actualizaciones como punto de partida.

## Qué todavía no está implementado
- Conexión real con YouTube o Spotify.
- Vídeo, subtítulos, calidad de streaming o gato flotante sobre otras apps.
- Servidor de actualizaciones. La pantalla de actualizaciones es informativa; no descarga ni instala nada todavía.

## Compilar el APK
1. Instala Android Studio y un JDK 17.
2. Abre la carpeta `NekoPlayer` en Android Studio.
3. Permite que Gradle sincronice y descargue Android Gradle Plugin y el SDK de Android 35.
4. En el menú selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio mostrará el enlace a `app-debug.apk` cuando la compilación termine.

Para generar una versión de distribución, configura una firma propia en Android Studio. No compartas claves privadas de firma.

## Próximos pasos
1. Compilar y probar el reproductor local.
2. Añadir pruebas y gestión de errores.
3. Revisar las vías oficiales permitidas para integraciones de YouTube/Spotify.
4. Publicar un endpoint HTTPS de metadatos de versión y diseñar actualizaciones seguras. Las actualizaciones de código Android normalmente requieren instalación autorizada o distribución por una tienda.
