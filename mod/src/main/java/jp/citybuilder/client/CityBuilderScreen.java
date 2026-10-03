package jp.citybuilder.client;

import jp.citybuilder.NetIds;
import jp.citybuilder.job.Styles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** City Builder のメニュー。建物 / 道路 / 区画 の3タブ。 */
public final class CityBuilderScreen extends Screen {
    private static final String[] LINES = {"none", "dashed", "solid", "double"};
    private static final String[] LINE_JA = {"なし", "破線", "実線", "二重線"};
    private static final int[] WIDTHS = {3, 5, 7, 9, 11, 13, 15};

    private static int tab = 0;
    private static int catIdx = 0;
    private static int page = 0;

    private final List<ClientCatalog.Entry> shown = new ArrayList<>();
    private final List<int[]> rects = new ArrayList<>();
    private int hovered = -1;

    public CityBuilderScreen() {
        super(Text.literal("City Builder"));
    }

    @Override
    public boolean shouldPause() { return false; }

    private void tabButton(int x, String label, int idx) {
        ButtonWidget b = new ButtonWidget(x, 22, 60, 20, Text.literal((tab == idx ? "▶ " : "") + label), btn -> {
            tab = idx;
            page = 0;
            rebuild();
        });
        addDrawableChild(b);
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    @Override
    protected void init() {
        shown.clear();
        rects.clear();
        int left = 10;
        tabButton(left, "建物", 0);
        tabButton(left + 64, "道路", 1);
        tabButton(left + 128, "区画", 2);

        addDrawableChild(new ButtonWidget(left + 192, 22, 80, 20, Text.literal("アップロード"), b -> client.setScreen(new UploadScreen(this))));
        addDrawableChild(new ButtonWidget(width - 110, 22, 100, 20, Text.literal("元に戻す(undo)"), b ->
                ClientPlayNetworking.send(NetIds.UNDO, PacketByteBufs.create())));
        addDrawableChild(new ButtonWidget(width - 220, 22, 100, 20, Text.literal("カタログ更新"), b ->
                ClientPlayNetworking.send(NetIds.REQUEST_CATALOG, PacketByteBufs.create())));

        if (tab == 0) initBuildings();
        else if (tab == 1) initRoad();
        else initArea();
    }

    private void initBuildings() {
        List<String> cats = ClientCatalog.categories();
        if (cats.isEmpty()) return;
        if (catIdx >= cats.size()) catIdx = 0;
        int y = 50;
        for (int i = 0; i < cats.size(); i++) {
            final int idx = i;
            String label = (i == catIdx ? "▶ " : "") + cats.get(i);
            addDrawableChild(new ButtonWidget(10, y, 110, 20, Text.literal(label), b -> {
                catIdx = idx;
                page = 0;
                rebuild();
            }));
            y += 22;
        }
        List<ClientCatalog.Entry> list = ClientCatalog.inCategory(cats.get(catIdx));
        int perPage = Math.max(3, (height - 50 - 40) / 22);
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;
        int from = page * perPage;
        int ly = 50;
        for (int i = from; i < Math.min(list.size(), from + perPage); i++) {
            ClientCatalog.Entry e = list.get(i);
            shown.add(e);
            rects.add(new int[]{130, ly, 250, 20});
            String label = e.name + "  (" + e.w + "×" + e.l + ")";
            addDrawableChild(new ButtonWidget(130, ly, 206, 20, Text.literal(label), b -> {
                PlacementMode.startBuilding(e);
                close();
            }));
            boolean custom = !PlacementMode.materialsFor(e.id).isEmpty();
            addDrawableChild(new ButtonWidget(338, ly, 42, 20, Text.literal(custom ? "§e建材" : "建材"), b ->
                    client.setScreen(new MaterialScreen(this, e))));
            ly += 22;
        }
        if (pages > 1) {
            int py = height - 28;
            addDrawableChild(new ButtonWidget(130, py, 40, 20, Text.literal("◀"), b -> { if (page > 0) { page--; rebuild(); } }));
            addDrawableChild(new ButtonWidget(340, py, 40, 20, Text.literal("▶"), b -> { if (page < pages - 1) { page++; rebuild(); } }));
        }
    }

    private static <T> int indexOf(T[] arr, T v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }

    private void initRoad() {
        int x = 130, y = 60;
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("幅: " + PlacementMode.roadWidth), b -> {
            int i = 0;
            for (int k = 0; k < WIDTHS.length; k++) if (WIDTHS[k] == PlacementMode.roadWidth) i = k;
            PlacementMode.roadWidth = WIDTHS[(i + 1) % WIDTHS.length];
            b.setMessage(Text.literal("幅: " + PlacementMode.roadWidth));
        }));
        y += 26;
        List<String> styles = new ArrayList<>(Styles.ROADS.keySet());
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("路面: " + PlacementMode.roadStyle), b -> {
            int i = Math.max(0, styles.indexOf(PlacementMode.roadStyle));
            PlacementMode.roadStyle = styles.get((i + 1) % styles.size());
            b.setMessage(Text.literal("路面: " + PlacementMode.roadStyle));
        }));
        y += 26;
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("中央線: " + LINE_JA[indexOf(LINES, PlacementMode.lines)]), b -> {
            int i = indexOf(LINES, PlacementMode.lines);
            PlacementMode.lines = LINES[(i + 1) % LINES.length];
            b.setMessage(Text.literal("中央線: " + LINE_JA[indexOf(LINES, PlacementMode.lines)]));
        }));
        y += 34;
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("道路ツールを開始"), b -> {
            PlacementMode.startRoad();
            close();
        }));
    }

    private void initArea() {
        int x = 130, y = 60;
        List<String> styles = new ArrayList<>(Styles.AREAS.keySet());
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("種類: " + PlacementMode.areaStyle), b -> {
            int i = Math.max(0, styles.indexOf(PlacementMode.areaStyle));
            PlacementMode.areaStyle = styles.get((i + 1) % styles.size());
            b.setMessage(Text.literal("種類: " + PlacementMode.areaStyle));
        }));
        y += 34;
        addDrawableChild(new ButtonWidget(x, y, 250, 20, Text.literal("区画ツールを開始"), b -> {
            PlacementMode.startArea();
            close();
        }));
    }

    @Override
    public void render(MatrixStack m, int mx, int my, float delta) {
        renderBackground(m);
        drawCenteredText(m, textRenderer, Text.literal("City Builder"), width / 2, 8, 0xFFFFFF);
        super.render(m, mx, my, delta);

        if (!ClientCatalog.canBuild) {
            drawCenteredText(m, textRenderer, Text.literal("§cこのサーバーでは建築権限がありません"), width / 2, height - 12, 0xFF5555);
        }
        if (tab == 0) {
            if (ClientCatalog.categories().isEmpty()) {
                textRenderer.draw(m, Text.literal("建物カタログを受信していません。「カタログ更新」を押してください。"), 130, 60, 0xFFAA00);
            }
            hovered = -1;
            for (int i = 0; i < rects.size(); i++) {
                int[] r = rects.get(i);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) hovered = i;
            }
            if (hovered >= 0) {
                ClientCatalog.Entry e = shown.get(hovered);
                int x = 395, y = 56;
                textRenderer.draw(m, Text.literal("§e" + e.name), x, y, 0xFFFFFF); y += 12;
                textRenderer.draw(m, Text.literal("ID: " + e.id), x, y, 0xAAAAAA); y += 12;
                textRenderer.draw(m, Text.literal("寸法: " + e.w + " × " + e.h + " × " + e.l + " (幅×高さ×奥行)"), x, y, 0xFFFFFF); y += 12;
                if (e.floors > 0) { textRenderer.draw(m, Text.literal("階数: " + e.floors), x, y, 0xFFFFFF); y += 12; }
                int maxW = Math.max(80, width - x - 10);
                for (var line : textRenderer.wrapLines(Text.literal(e.description), maxW)) {
                    textRenderer.draw(m, line, x, y, 0xCCCCCC);
                    y += 11;
                }
            }
        } else if (tab == 1) {
            textRenderer.draw(m, Text.literal("右クリック: 点を追加 / 同じ点をもう一度右クリック: 確定(2点以上)"), 130, 190, 0xCCCCCC);
            textRenderer.draw(m, Text.literal("道路幅は奇数が左右対称になります。歩道(幅2)は自動で付きます。"), 130, 202, 0xAAAAAA);
        } else {
            textRenderer.draw(m, Text.literal("1回目の右クリック: 角を指定 / 2回目: 反対の角で確定"), 130, 150, 0xCCCCCC);
            textRenderer.draw(m, Text.literal("Shift+右クリック: 角の指定を取り消し(もう一度で終了)"), 130, 162, 0xAAAAAA);
        }
    }
}
