# PokeBuilder + PokemonViewAPI — Guía de compilación

## Estructura de carpetas recomendada

```
MiServidor/
  PokeBuilder-mejorado/    ← mod server-side
  PokemonViewAPI/          ← mod client-side (API pública)
```

---

## Paso 1 — Compilar PokemonViewAPI primero

```bat
cd PokemonViewAPI
.\gradlew.bat build
```

El JAR se genera en:
```
PokemonViewAPI\build\libs\PokemonViewAPI-fabric-1.0.0-1.0.0.jar
```

---

## Paso 2 — Copiar el JAR al libs/ de PokeBuilder

**Opción A (automática):** La tarea `copyApiJar` lo hace por ti:

```bat
cd ..\PokeBuilder-mejorado
.\gradlew.bat copyApiJar build
```

**Opción B (manual):** Copia el JAR a mano:

```
PokemonViewAPI\build\libs\PokemonViewAPI-fabric-1.0.0-1.0.0.jar
        ↓ copiar a
PokeBuilder-mejorado\libs\PokemonViewAPI-fabric-1.0.0-1.0.0.jar
```

Luego compila PokeBuilder:

```bat
cd PokeBuilder-mejorado
.\gradlew.bat build
```

---

## Paso 3 — Instalar en el servidor / cliente

| Archivo | Dónde va |
|---------|----------|
| `PokeBuilder-fabric-1.0.0-1.0.0.jar` | Carpeta `mods/` del **servidor** |
| `PokemonViewAPI-fabric-1.0.0-1.0.0.jar` | Carpeta `mods/` de cada **cliente** |

> PokeBuilder es server-side únicamente.  
> PokemonViewAPI es client-side únicamente.  
> Si el cliente no tiene PokemonViewAPI, el botón "Ver en 3D" simplemente no hace nada.

---

## Cómo funciona

```
[Servidor - PokeBuilder]                [Cliente - PokemonViewAPI]
   Jugador abre menú PokeBuilder
   → Click en "✦ Ver en 3D"
   → PacketHelper.sendOpenViewer(...)   →→→  paquete pokemonviewapi:open_pokemon_view
                                              PokemonViewAPIClient lo recibe
                                              PokemonViewScreen.open()
                                              drawProfilePokemon() ← igual que la PC de Cobblemon
```

---

## Usar la API desde otro mod

Si quieres que tu propio mod también abra la pantalla 3D:

1. Agrega `PokemonViewAPI-fabric-1.0.0-1.0.0.jar` a tu `libs/`
2. Llama desde el servidor:

```java
PacketHelper.sendOpenViewer(
    player,
    pokemon.getUuid(),
    pokemon.getSpecies().getResourceIdentifier(),
    pokemon.getLevel(),
    pokemon.getShiny(),
    pokemon.getNature().getName().getPath(),
    pokemon.getAbility().getName(),
    pokemon.getScaleModifier(),
    pokemon.getGender().name(),   // "MALE" / "FEMALE" / "GENDERLESS"
    moveNames,                    // List<String>
    ivHp, ivAtk, ivDef, ivSpA, ivSpD, ivSpe,
    heldItemName,
    pokemon.getAspects()          // Set<String>
);
```
