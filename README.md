# Emerald Builder 1.20.4
Mod cliente Fabric para Minecraft 1.20.4.

## Controles
- **P**: abre configuración.
- **O**: inicia/detiene.

## Configuración
Define X mínima/máxima, Z mínima/máxima y Y mínima/máxima. El recorrido se genera en espiral, girando a la derecha y cerrándose hacia el interior.

## Funcionamiento
1. Usa el slot 1 para bloques de esmeralda.
2. Se centra en la columna objetivo, mira abajo, salta y coloca bloques hasta Y máxima.
3. Arriba se desplaza a la siguiente columna y cae.
4. Repite hasta completar el área.
5. Si el slot 1 queda vacío, mueve otro stack de bloques de esmeralda del inventario.
6. Si se terminan: `/home bloques`, intenta abrir el cofre al frente, extrae bloques de esmeralda, cierra, `/back` y continúa.

## GitHub
Sube **todo el contenido de esta carpeta** a la raíz del repositorio. GitHub Actions ejecutará `gradle build` con Java 17 y Gradle 8.7. El JAR aparecerá como artifact `EmeraldBuilder-1.20.4`.

## Nota
La automatización del cofre presupone que `/home bloques` te deja frente y al alcance del cofre. Servidores con anticheat, menús personalizados, delays de teleport o reglas propias pueden requerir ajustar tiempos.
