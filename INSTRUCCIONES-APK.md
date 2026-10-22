# Neko Player — obtener el APK sin Android Studio

Este proyecto está configurado para que GitHub compile el APK en la nube. No necesitas instalar Java, Gradle ni Android Studio en tu computador.

## Pasos para obtener el APK

1. Entra a [github.com](https://github.com) e inicia sesión o crea una cuenta.
2. Crea un repositorio nuevo. Si quieres mantener el proyecto privado, elige **Private**.
3. Sube el contenido de esta carpeta al repositorio. Deben quedar en la raíz carpetas como `app` y `.github`, y archivos como `build.gradle` y `settings.gradle`.
4. Abre la pestaña **Actions** del repositorio. Si GitHub muestra una confirmación para permitir flujos de trabajo, acéptala.
5. Selecciona **Crear APK de Neko Player** y pulsa **Run workflow**. También se compilará automáticamente cuando subas cambios.
6. Cuando termine con una marca verde, abre esa ejecución y descarga el archivo **Neko-Player-APK-debug** en la sección **Artifacts**.
7. Extrae el ZIP descargado. Dentro encontrarás `app-debug.apk`; envíalo a tu teléfono e instálalo. Android puede pedirte permiso para instalar aplicaciones desde esa fuente.

El APK debug sirve para instalar y probar la aplicación. Esta configuración no publica la app en Google Play.

## ¿Puede Codex hacerlo por ti?

Sí. Si conectas GitHub a Codex, puedo subir el proyecto al repositorio y lanzar la compilación por ti. Después podrás descargar aquí el APK que genere GitHub Actions. Sin esa conexión, puedo dejar preparado el proyecto, pero no acceder a tu cuenta ni subir archivos en tu nombre.
