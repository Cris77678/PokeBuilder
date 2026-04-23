package com.tuservidor.pokebuilder.config;

import com.google.gson.GsonBuilder;
import com.tuservidor.pokebuilder.PokeBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.*;

@Getter
@Setter
public class PokeBuilderLang {

    private String prefix = "&7[&6PokeBuilder&7] ";
    private String menuTitle = "&6PokeBuilder &8| &7%pokemon%";
    private String selectPokemonTitle = "&6Selecciona un Pokémon";

    private String msgNotEnoughFunds = "%prefix% &cNo tienes suficientes &e%coin%&c. Necesitas &e%cost%&c.";
    private String msgSuccess = "%prefix% &aEdición aplicada a &6%pokemon%&a. Cobrado: &e%cost%&a.";
    private String msgBlacklisted = "%prefix% &cEste Pokémon no puede ser editado.";
    private String msgReload = "%prefix% &aConfiguración recargada.";
    private String msgBalance = "%prefix% &7Tu saldo: &e%balance%";
    private String msgShinyAlready = "%prefix% &cEste Pokémon ya es shiny.";

    // Sacrifice messages
    private String msgSacrificeBuilt       = "%prefix% &cEste Pokémon fue modificado con PokeBuilder y no puede ser sacrificado.";
    private String msgSacrificeNotEligible = "%prefix% &cEste Pokémon no es shiny ni legendario.";
    private String msgSacrificeConfirm     = "%prefix% &e¿Seguro? &7Haz click de nuevo para confirmar el sacrificio de &6%pokemon%&7.";
    private String msgSacrificeSuccess     = "%prefix% &6%pokemon% &7fue sacrificado. Recibiste &e%amount% %coin%&7.";
    private String msgSacrificeCancelled   = "%prefix% &7Sacrificio cancelado.";

    private String lorePrice = "&7Precio: &e%cost%";
    private String loreCurrent = "&7Actual: &f%value%";
    private String loreClick = "&aClick para editar";
    private String loreBack = "&7Volver";

    public void init() {
        Path path = Path.of(PokeBuilder.PATH_LANG + "es.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                PokeBuilderLang loaded = gson.fromJson(Files.readString(path), PokeBuilderLang.class);
                if (loaded != null) {
                    PokeBuilder.lang = loaded;
                }
            }
            Files.writeString(path, gson.toJson(PokeBuilder.lang));
        } catch (IOException e) {
            PokeBuilder.LOGGER.error("Failed to load lang", e);
        }
    }

    public String format(String msg, Object... replacements) {
        // [FIX] Evita el NullPointerException si falta un mensaje en la configuración
        if (msg == null) return ""; 

        String pfx = this.prefix != null ? this.prefix : "";
        String coin = PokeBuilder.config != null && PokeBuilder.config.getCoinName() != null 
                        ? PokeBuilder.config.getCoinName() : "Monedas";

        msg = msg.replace("%prefix%", pfx);
        msg = msg.replace("%coin%", coin);
        
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = String.valueOf(replacements[i]);
            String val = String.valueOf(replacements[i + 1]);
            if (key != null && val != null) {
                msg = msg.replace(key, val);
            }
        }
        return msg;
    }
}
