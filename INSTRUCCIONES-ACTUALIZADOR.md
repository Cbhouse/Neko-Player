# Preparar el actualizador

El botón **Actualizaciones y versión** consulta la última publicación de GitHub. Si encuentra una versión más nueva, permite descargar la APK y abrir el instalador de Android. Android puede pedir que autorices a Neko Player a iniciar instalaciones y que confirmes cada actualización.

## 1. Guarda la clave de firma

La clave permanente viene en los archivos que te entregué por separado:

- `NekoPlayer-release-key.p12`: copia de respaldo privada. Guárdala en un lugar seguro.
- `NEKO_KEYSTORE_BASE64.txt`
- `NEKO_KEYSTORE_PASSWORD.txt`
- `NEKO_KEY_ALIAS.txt`
- `NEKO_KEY_PASSWORD.txt`

**No subas estos archivos al repositorio ni los compartas públicamente.** Si se pierde la clave, Android no aceptará las siguientes versiones como actualización de esta app.

## 2. Añade los secretos al repositorio

En GitHub abre **Settings → Secrets and variables → Actions → New repository secret**. Crea estos cuatro secretos y pega en cada campo **Secret** el contenido del archivo indicado:

| Nombre del secreto | Archivo con el valor |
|---|---|
| `NEKO_KEYSTORE_BASE64` | `NEKO_KEYSTORE_BASE64.txt` |
| `NEKO_KEYSTORE_PASSWORD` | `NEKO_KEYSTORE_PASSWORD.txt` |
| `NEKO_KEY_PASSWORD` | `NEKO_KEY_PASSWORD.txt` |

El flujo detecta automáticamente el alias dentro de la clave. Si ya existe el secreto `NEKO_KEY_ALIAS`, puedes dejarlo; no se utiliza.

## 3. Publica la primera versión firmada

En el repositorio abre **Actions → Publicar actualización de Neko Player → Run workflow**. Cuando termine en verde, creará una publicación con `app-release.apk`.

El repositorio y su publicación deben ser accesibles públicamente para que Neko Player pueda consultar versiones sin guardar contraseñas dentro de la aplicación.

## 4. Instala la versión actualizable una vez

La APK de prueba instalada anteriormente puede tener una firma distinta. Quizá debas desinstalar Neko Player e instalar la nueva APK desde la publicación. Los archivos de música del teléfono no se borran, aunque Android puede borrar los datos internos de la app al desinstalarla.

Después de esa primera instalación, publica las siguientes versiones con la misma clave. En Neko Player, pulsa **Actualizaciones y versión** para comprobar y descargar una actualización.
