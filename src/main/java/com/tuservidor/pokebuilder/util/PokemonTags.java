package com.tuservidor.pokebuilder.util;

import com.cobblemon.mod.common.pokemon.Pokemon;

public class PokemonTags {

    // Clave NBT persistente que nunca se borrará al evolucionar
    private static final String BUILT_KEY = "PokeBuilder_IsBuilt";

    /**
     * Marca permanentemente al Pokémon como editado.
     */
    public static void markBuilt(Pokemon pokemon) {
        // Usamos PersistentData para garantizar que sobreviva evoluciones y reinicios
        pokemon.getPersistentData().putBoolean(BUILT_KEY, true);
    }

    /**
     * Comprueba si el Pokémon ha sido tocado por el mod.
     */
    public static boolean isBuilt(Pokemon pokemon) {
        if (!pokemon.getPersistentData().contains(BUILT_KEY)) {
            return false;
        }
        return pokemon.getPersistentData().getBoolean(BUILT_KEY);
    }
}
