# Publicar y comprobar las reglas de SportPro

**Proyecto confirmado desde la configuración Android local: `sportpro-df6e0`.** La autorización para este corte es publicar sólo `firestore.rules`. No publicar Storage, Hosting, Functions ni cambiar Auth o documentos.

## Estado de esta entrega

La máquina de construcción no tiene una cuenta autenticada en Firebase CLI ni credencial de despliegue configurada. **No se respaldaron ni publicaron reglas remotas desde aquí.** Se entrega este procedimiento para Henry, según el fallback autorizado. `firebase.json` declara exclusivamente Firestore. Las reglas candidatas tienen pruebas del emulador; eso no acredita que ya estén instaladas en el proyecto.

## 1. Respaldar primero, con tu cuenta

1. Abrir Firebase Console con tu cuenta autorizada y seleccionar explícitamente **sportpro-df6e0**. Entrar a Firestore Database → Rules.
2. Copiar **las reglas actualmente publicadas**, completas, a un archivo local fuera del repositorio, por ejemplo Documentos/SportPro-backup-firestore.rules. Anotar la fecha y el proyecto. Conservar también una captura del historial de publicación.
3. Calcular su hash: en PowerShell `Get-FileHash "$env:USERPROFILE/Documents/SportPro-backup-firestore.rules" -Algorithm SHA256`; en Linux/macOS `sha256sum ~/Documents/SportPro-backup-firestore.rules`. Si no puedes leer/guardar el respaldo, **no publicar todavía**. No sobrescribir el respaldo con la versión nueva.

## 2. Revisar y publicar la candidata

Desde el repositorio en la rama `feat/semana12` actualizada:

```sh
npm --prefix tests/firestore ci --ignore-scripts --no-audit --no-fund
npm --prefix tests/firestore test
npm exec --prefix tests/firestore -- firebase login
npm exec --prefix tests/firestore -- firebase projects:list
npm exec --prefix tests/firestore -- firebase deploy --only firestore:rules --project sportpro-df6e0 --config firebase.json
```

Usar el navegador de login con tu propia cuenta, no la de otro equipo. Verificar que el proyecto figure en tu acceso. Si la cuenta no puede publicar, hacerlo con un propietario autorizado; no ampliar permisos ni crear una cuenta de servicio sólo para esta entrega.

La alternativa sin CLI es pegar la candidata `firestore.rules` en Rules y publicar **después del respaldo**. Guardar el recibo de publicación y el SHA256 de la candidata con `Get-FileHash firestore.rules -Algorithm SHA256` o `sha256sum firestore.rules`. No cambiar a otro proyecto para que el comando funcione. No subir `local.properties`, claves privadas ni `google-services.json` a Git.

## 3. Comprobación real después de publicar

Debe existir una cuenta de prueba autorizada de la app y su documento `users/{uid}`. No crear datos privados ni cuentas de terceros para esta comprobación.

```sh
python tools/verify_deployed_rules.py
```

El programa usa tu configuración Android local, solicita el email y la contraseña sin eco, obtiene un token de Firebase en memoria y realiza dos **lecturas reales** del mismo perfil: autenticada y anónima. No imprime ni guarda contraseña, token, API key o contenido del perfil. Sólo informa códigos HTTP:

- `own_profile_authenticated_status: 200` → lectura propia permitida.
- `same_profile_anonymous_status: 401` o `403` → lectura anónima denegada.
- `pass: true`. Si ambos se permiten o ambos se deniegan, **falla**. No basta con tener un 403 aislado.

Guardar sólo ese JSON saneado junto al recibo de publicación. Las pruebas sintéticas del helper se ejecutan con `python tools/verify_deployed_rules.py --self-test`; no equivalen a la comprobación remota.

Luego hacer el recorrido de `SEMANA12_DEMO.md` con entrenador y jugador/padre: equipo propio, evento con historial y borrador privado. Las dos lecturas REST anteriores comprueban acceso propio/anónimo; no sustituyen toda la matriz de roles.

## Si falla

Conservar los códigos y el recibo, no repetir envíos ni modificar cuentas a ciegas. El propietario puede restaurar **el respaldo exacto** desde Rules → publicar o desde el historial de Firebase Console. Confirmar después las dos lecturas de nuevo. No usar una regla temporal abierta `allow read, write: if true` para desbloquear la demostración.
