package com.tuservidor.pokebuilder.util;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiUtils {

    private static final Map<Character, String> SMALL_CAPS = new HashMap<>();
    static {
        String normal = "abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
        String[] caps = {"ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ñ","ᴏ","ᴘ","ꞯ","ʀ","s","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ",
                         "ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ñ","ᴏ","ᴘ","ꞯ","ʀ","s","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ"};
        for (int i = 0; i < normal.length(); i++) SMALL_CAPS.put(normal.charAt(i), caps[i]);
    }

    /** Convierte texto a la fuente estilizada de PokeLand */
    public static String toSmallCaps(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(SMALL_CAPS.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    public static Text smallText(String text) {
        return AdventureTranslator.toNative(toSmallCaps(text));
    }

    // --- MÉTODOS DE UTILIDAD PARA MENÚS ---

    public static GooeyButton filler(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(" "));
        return GooeyButton.builder().display(stack).onClick(a -> {}).build();
    }

    public static GooeyButton filler() {
        return filler(Items.GRAY_STAINED_GLASS_PANE);
    }

    /** Crea un botón que aplica automáticamente la fuente Small Caps al nombre */
    public static GooeyButton button(Item item, String name, List<?> lore, Runnable onClick) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(toSmallCaps(name)));
        
        // Convertimos el lore a texto nativo de Adventure
        List<String> stringLore = (List<String>) lore;
        stack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(stringLore)));
        
        return GooeyButton.builder()
                .display(stack)
                .onClick(a -> PokeBuilder.runAsync(onClick))
                .build();
    }

    /** Rellena los huecos vacíos de un menú */
    public static void fillEmpty(ChestTemplate template, int rows) {
        GooeyButton f = filler();
        for (int i = 0; i < rows * 9; i++) {
            try {
                var slot = template.getSlot(i);
                if (slot == null || slot.getButton().isEmpty()) {
                    template.set(i, f);
                }
            } catch (Exception ignored) {}
        }
    }
}