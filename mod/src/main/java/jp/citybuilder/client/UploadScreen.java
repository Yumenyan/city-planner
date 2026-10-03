package jp.citybuilder.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Util;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 自分の .schem をサーバーへアップロードする画面。
 * ウィンドウにファイルをドラッグ&ドロップするか、下記フォルダに置いたファイルを一覧から選ぶ。
 */
public final class UploadScreen extends Screen {
    private static final Pattern ID_RE = Pattern.compile("[A-Za-z0-9_\\-]{1,48}");
    private static final int PER_PAGE = 7;

    private static Path selected;
    private static String idText = "", nameText = "", catText = "アップロード", descText = "";
    private static int page = 0;
    private static final List<Path> DROPPED = new ArrayList<>();

    private final Screen parent;
    private TextFieldWidget fId, fName, fCat, fDesc;
    private String status = "";
    private List<Path> files = new ArrayList<>();

    public UploadScreen(Screen parent) {
        super(Text.literal("建物をアップロード"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() { return false; }

    public static Path dropFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("citybuilder_upload");
    }

    private static List<Path> folders() {
        Path g = FabricLoader.getInstance().getGameDir();
        List<Path> l = new ArrayList<>();
        l.add(dropFolder());
        l.add(g.resolve("schematics"));
        l.add(g.resolve("config").resolve("worldedit").resolve("schematics"));
        return l;
    }

    private static List<Path> scan() {
        List<Path> r = new ArrayList<>(DROPPED);
        try { Files.createDirectories(dropFolder()); } catch (IOException ignored) {}
        for (Path d : folders()) {
            if (!Files.isDirectory(d)) continue;
            try (Stream<Path> s = Files.list(d)) {
                s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".schem")).sorted().forEach(p -> {
                    if (!r.contains(p)) r.add(p);
                });
            } catch (IOException ignored) {}
        }
        return r;
    }

    private static String sanitize(String stem) {
        String s = stem.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (s.replace("_", "").isEmpty()) s = "custom";
        return s.length() > 48 ? s.substring(0, 48) : s;
    }

    private void select(Path p) {
        selected = p;
        String fn = p.getFileName().toString();
        String stem = fn.contains(".") ? fn.substring(0, fn.lastIndexOf('.')) : fn;
        idText = sanitize(stem).toLowerCase();
        nameText = stem;
        rebuild();
    }

    private void rebuild() {
        if (fId != null) saveFields();
        clearChildren();
        init();
    }

    /** ウィンドウへのファイルドロップ(環境によりメソッド名が異なる場合は下の一覧から選べます) */
    public void filesDragged(List<Path> paths) {
        Path last = null;
        for (Path p : paths) {
            if (p.getFileName().toString().toLowerCase().endsWith(".schem")) {
                if (!DROPPED.contains(p)) DROPPED.add(p);
                last = p;
            }
        }
        if (last != null) select(last);
        else status = "§c.schem ファイルをドロップしてください";
    }

    @Override
    protected void init() {
        files = scan();
        int left = 10;
        int pages = Math.max(1, (files.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int y = 44;
        for (int i = page * PER_PAGE; i < Math.min(files.size(), (page + 1) * PER_PAGE); i++) {
            final Path p = files.get(i);
            String label = (p.equals(selected) ? "▶ " : "") + p.getFileName();
            addDrawableChild(new ButtonWidget(left, y, 230, 20, Text.literal(label), b -> select(p)));
            y += 22;
        }
        int py = 44 + PER_PAGE * 22;
        addDrawableChild(new ButtonWidget(left, py, 40, 20, Text.literal("◀"), b -> { if (page > 0) { page--; rebuild(); } }));
        addDrawableChild(new ButtonWidget(left + 190, py, 40, 20, Text.literal("▶"), b -> { if (page < pages - 1) { page++; rebuild(); } }));
        addDrawableChild(new ButtonWidget(left, py + 24, 230, 20, Text.literal("アップロード用フォルダを開く"), b -> {
            try {
                Files.createDirectories(dropFolder());
                Util.getOperatingSystem().open(dropFolder().toFile());
            } catch (IOException | RuntimeException e) {
                status = "§cフォルダを開けませんでした: " + dropFolder();
            }
        }));

        int x = 260, fw = Math.max(120, Math.min(220, width - x - 20));
        fId = field(x, 60, fw, "ID", idText);
        fName = field(x, 100, fw, "表示名", nameText);
        fCat = field(x, 140, fw, "カテゴリ", catText);
        fDesc = field(x, 180, fw, "説明", descText);
        fId.setMaxLength(48); fName.setMaxLength(40); fCat.setMaxLength(20); fDesc.setMaxLength(120);
        addDrawableChild(new ButtonWidget(x, 210, fw, 20, Text.literal("アップロード"), b -> send()));
        addDrawableChild(new ButtonWidget(x, height - 30, 80, 20, Text.literal("戻る"), b -> client.setScreen(parent)));
    }

    private TextFieldWidget field(int x, int y, int w, String label, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 18, Text.literal(label));
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private void saveFields() {
        idText = fId.getText().trim();
        nameText = fName.getText().trim();
        catText = fCat.getText().trim();
        descText = fDesc.getText().trim();
    }

    private void send() {
        saveFields();
        if (!ClientCatalog.canUpload) { status = "§cこのサーバーではアップロードできません(権限または設定)"; return; }
        if (selected == null) { status = "§c.schem ファイルを選んでください"; return; }
        if (!ID_RE.matcher(idText).matches()) { status = "§cIDは半角英数字と _ - の48文字以内にしてください"; return; }
        if (UploadTask.active()) { status = "§c送信中です"; return; }
        try {
            long size = Files.size(selected);
            if (size > ClientCatalog.maxUploadBytes) {
                status = "§cファイルが大きすぎます(最大 " + ClientCatalog.maxUploadBytes / 1000 + " KB / このファイル " + size / 1000 + " KB)";
                return;
            }
            byte[] data = Files.readAllBytes(selected);
            if (data.length < 4 || (data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B) {
                status = "§c.schem(Sponge Schematic)ではないようです";
                return;
            }
            UploadTask.start(idText, nameText.isEmpty() ? idText : nameText, catText, descText, data);
            client.setScreen(null);
        } catch (IOException e) {
            status = "§c読み込みに失敗しました: " + e.getMessage();
        }
    }

    @Override
    public void render(MatrixStack m, int mx, int my, float delta) {
        renderBackground(m);
        drawCenteredText(m, textRenderer, Text.literal("建物をアップロード"), width / 2, 8, 0xFFFFFF);
        textRenderer.draw(m, Text.literal("§7.schem をこの画面にドラッグ&ドロップ、または一覧から選択"), 10, 28, 0xAAAAAA);
        int x = 260;
        textRenderer.draw(m, Text.literal("ID (半角英数字 _ -)"), x, 50, 0xFFFFFF);
        textRenderer.draw(m, Text.literal("表示名"), x, 90, 0xFFFFFF);
        textRenderer.draw(m, Text.literal("カテゴリ"), x, 130, 0xFFFFFF);
        textRenderer.draw(m, Text.literal("説明"), x, 170, 0xFFFFFF);
        if (files.isEmpty()) {
            textRenderer.draw(m, Text.literal("§7(一覧が空です)"), 14, 50, 0xAAAAAA);
            textRenderer.draw(m, Text.literal("§7置き場所: " + dropFolder().getFileName() + " / schematics / config/worldedit/schematics"), 10, height - 44, 0xAAAAAA);
        }
        if (!ClientCatalog.canUpload) {
            textRenderer.draw(m, Text.literal("§cこのサーバーではアップロード権限がありません"), x, 236, 0xFF5555);
        } else {
            textRenderer.draw(m, Text.literal("§7最大 " + ClientCatalog.maxUploadBytes / 1000 + " KB"), x, 236, 0xAAAAAA);
        }
        if (!status.isEmpty()) textRenderer.draw(m, Text.literal(status), x, 250, 0xFFFFFF);
        super.render(m, mx, my, delta);
    }
}
