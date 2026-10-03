package jp.citybuilder.client;

import jp.citybuilder.Materials;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 建物の建材を置き換える画面。「元の建材 → 置き換え先のブロックID」を指定する。
 * 向き・半ブロックなどの状態は引き継がれる。空欄は置き換えなし。
 */
public final class MaterialScreen extends Screen {
    private static final String[] COLORS = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
    private static final List<String> PRESETS = new ArrayList<>();

    static {
        for (String c : COLORS) PRESETS.add(c + "_concrete");
        for (String s : new String[]{"smooth_quartz", "quartz_block", "stone_bricks", "bricks", "smooth_sandstone", "smooth_stone",
                "stone", "andesite", "polished_andesite", "polished_diorite", "polished_granite", "white_terracotta",
                "light_gray_terracotta", "terracotta", "red_terracotta", "brown_terracotta", "oak_planks", "spruce_planks",
                "birch_planks", "dark_oak_planks", "deepslate_tiles", "polished_blackstone", "iron_block", "copper_block"}) {
            PRESETS.add(s);
        }
    }

    private final Screen parent;
    private final ClientCatalog.Entry entry;
    private final Map<String, String> map;
    private final List<TextFieldWidget> fields = new ArrayList<>();

    public MaterialScreen(Screen parent, ClientCatalog.Entry entry) {
        super(Text.literal("建材を変更"));
        this.parent = parent;
        this.entry = entry;
        this.map = PlacementMode.materialsFor(entry.id);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static String shortName(String id) {
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private static boolean valid(String text) {
        Optional<Block> b = Materials.block(text);
        return b.isPresent() && !b.get().getDefaultState().hasBlockEntity();
    }

    @Override
    protected void init() {
        fields.clear();
        int rows = Math.min(entry.materials.size(), Math.max(1, (height - 100) / 24));
        int x = Math.max(10, width / 2 - 210);
        for (int i = 0; i < rows; i++) {
            final String from = (String) entry.materials.get(i)[0];
            int y = 40 + i * 24;
            TextFieldWidget f = new TextFieldWidget(textRenderer, x + 200, y, 170, 18, Text.literal(from));
            f.setMaxLength(60);
            String cur = map.get(from);
            f.setText(cur == null ? "" : shortName(cur));
            f.setEditableColor(cur == null || valid(cur) ? 0xFFFFFF : 0xFF5555);
            f.setChangedListener(text -> {
                String t = text.trim();
                if (t.isEmpty() || shortName(from).equals(t)) {
                    map.remove(from);
                    f.setEditableColor(0xFFFFFF);
                } else if (valid(t)) {
                    map.put(from, t);
                    f.setEditableColor(0xFFFFFF);
                } else {
                    map.remove(from);
                    f.setEditableColor(0xFF5555);
                }
            });
            fields.add(f);
            addDrawableChild(f);
            addDrawableChild(new ButtonWidget(x + 374, y - 1, 20, 20, Text.literal("◀"), b -> cycle(f, -1)));
            addDrawableChild(new ButtonWidget(x + 396, y - 1, 20, 20, Text.literal("▶"), b -> cycle(f, 1)));
        }
        int by = height - 30;
        addDrawableChild(new ButtonWidget(x, by, 110, 20, Text.literal("すべてリセット"), b -> {
            map.clear();
            for (TextFieldWidget f : fields) f.setText("");
        }));
        addDrawableChild(new ButtonWidget(x + 120, by, 150, 20, Text.literal("この建材で配置開始"), b -> {
            PlacementMode.startBuilding(entry);
            client.setScreen(null);
        }));
        addDrawableChild(new ButtonWidget(x + 280, by, 80, 20, Text.literal("戻る"), b -> client.setScreen(parent)));
    }

    private void cycle(TextFieldWidget f, int dir) {
        String t = f.getText().trim();
        int i = PRESETS.indexOf(t);
        int n = PRESETS.size();
        int next = i < 0 ? (dir > 0 ? 0 : n - 1) : (i + dir + n) % n;
        f.setText(PRESETS.get(next));
    }

    @Override
    public void render(MatrixStack m, int mx, int my, float delta) {
        renderBackground(m);
        drawCenteredText(m, textRenderer, Text.literal("建材を変更: " + entry.name), width / 2, 10, 0xFFFFFF);
        drawCenteredText(m, textRenderer, Text.literal("§7置き換え先のブロックIDを入力(空欄=そのまま)。◀▶で候補を切り替え。向きなどは引き継がれます。"), width / 2, 24, 0xAAAAAA);
        int x = Math.max(10, width / 2 - 210);
        int rows = Math.min(entry.materials.size(), Math.max(1, (height - 100) / 24));
        for (int i = 0; i < rows; i++) {
            Object[] mat = entry.materials.get(i);
            textRenderer.draw(m, Text.literal(shortName((String) mat[0]) + " §7×" + mat[1] + " §f→"), x, 45 + i * 24, 0xFFFFFF);
        }
        super.render(m, mx, my, delta);
    }
}
