# Demo App - Physiological Data via MQTT

Este proyecto contiene una aplicación Android dividida en módulos principales:

- **`DeviceMenuActivity`**  
  Se encarga de manejar la conexión con el sensor fisiológico, gestionar permisos y mostrar las opciones al usuario.

- **`MqttForegroundService`**  
  Servicio en segundo plano que crea y mantiene una conexión **MQTT** persistente, incluso con la pantalla bloqueada.  
  - Envía tramas JSON con los datos del sensor.  
  - Usa el puerto **8883 (MQTT over SSL/TLS)** para seguridad.  
  - Requiere certificados o autenticación mediante **SAS Token** para comunicación segura con el broker en la nube (ej. Azure IoT Hub).

- **`Cuestionario`**  
  Clase encargada de mostrar un formulario al usuario (tipo encuesta/cuestionario) para asociar respuestas subjetivas con los datos fisiológicos recolectados.

---

## Requisitos

- **Android Studio:** 4.1.3 o superior  
- **Gradle:** 6.1.1 (wrapper incluido)  
- **JDK:** 1.8  
- **NDK:** 21.3.6528147  
- **CMake:** 3.10.2.4988404  
- **SDK mínimo:** 21  
- **SDK objetivo:** 30  

---

## Configuración del entorno

1. Definir variables de entorno en **Windows PowerShell**:

```powershell
setx JAVA_HOME "C:\Program Files\Java\jdk1.8.0_281" /M
setx ANDROID_HOME "D:\develop\SDK" /M
setx ANDROID_NDK_HOME "D:\develop\SDK\ndk\21.3.6528147" /M
```

2. Agregar al `PATH`:

```powershell
setx PATH "%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools" /M
```

---


## Solución de problemas

- **Error de memoria en `mergeDexDebug`**  
  Editar `gradle.properties` y agregar:  
  ```
  org.gradle.jvmargs=-Xmx6000m -Dfile.encoding=UTF-8
  ```

- **Limpiar completamente el proyecto**  
  ```bash
  ./gradlew clean
  rd /s /q .gradle
  rd /s /q .cxx
  rd /s /q build
  ```

- **Problemas con permisos de OneDrive**  

  - Si Jetifier falla, deshabilitarlo para esas dependencias en `gradle.properties`:  
    ```
    android.jetifier.blacklist=butterknife-compiler
    ```

---

## Notas de seguridad MQTT

- Usa **TLS/SSL** en puerto **8883**.  
- Compatible con autenticación mediante certificados X.509 o tokens (ej. SAS en Azure).  
- Recomendado validar expiración de certificados/tokens antes de reconectar.

---

## Licencia

Proyecto interno de demostración. No usar en producción sin revisión de seguridad. 
