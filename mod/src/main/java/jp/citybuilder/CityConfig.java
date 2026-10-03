package jp.citybuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/citybuilder/config.json */
public final class CityConfig {
    /** コマンド/GUI配置に必要なOPレベル。0なら全員。 */
    public int permissionLevel = 2;
    /** OPレベルに関係なく許可するプレイヤー名 */
    public List<String> allowedPlayers = new ArrayList<>();
    /** GUI(パケット)からの配置にクリエイティブモードを要求する */
    public boolean requireCreative = true;
    /** プレイヤーから配置範囲までの最大距離(ブロック) */
    public int maxPlaceDistance = 160;
    /** 1tickあたりに処理するブロック数 */
    public int blocksPerTick = 4000;
    /** 同一プレイヤーの配置リクエスト間隔(tick) */
    public int cooldownTicks = 10;
    /** 1回の配置で許す最大体積 */
    public int maxVolume = 2_000_000;
    /** キューに溜められる最大ブロック数 */
    public long maxPendingBlocks = 5_000_000L;
    /** /citybuilder undo で戻せる回数(プレイヤーごと) */
    public int historyDepth = 10;
    /** 道路・区画の上方を空気にする高さ */
    public int roadClearAbove = 12;
    /** 道路・区画の下の空洞を埋める */
    public boolean roadFoundation = true;
    /** 建物の下の空洞を埋める */
    public boolean templateFoundation = true;
    /** 基礎で埋める最大深さ */
    public int foundationDepth = 8;
    public List<String> allowedDimensions = new ArrayList<>(List.of("minecraft:overworld"));
    /** 空でなければ、この範囲内にだけ建築できる */
    public List<IntBox> buildAreas = new ArrayList<>();
    /** この範囲には建築できない */
    public List<IntBox> protectedAreas = new ArrayList<>();

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("citybuilder");
    }

    public static CityConfig load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path file = dir().resolve("config.json");
        CityConfig c = null;
        try {
            Files.createDirectories(dir());
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    c = gson.fromJson(r, CityConfig.class);
                }
            }
        } catch (Exception e) {
            CityBuilderMod.LOG.warn("config.json を読めませんでした。既定値を使います: {}", e.toString());
        }
        if (c == null) c = new CityConfig();
        if (c.allowedPlayers == null) c.allowedPlayers = new ArrayList<>();
        if (c.allowedDimensions == null) c.allowedDimensions = new ArrayList<>();
        if (c.buildAreas == null) c.buildAreas = new ArrayList<>();
        if (c.protectedAreas == null) c.protectedAreas = new ArrayList<>();
        for (IntBox b : c.buildAreas) b.normalize();
        for (IntBox b : c.protectedAreas) b.normalize();
        c.blocksPerTick = Math.max(100, c.blocksPerTick);
        c.maxPlaceDistance = Math.max(8, Math.min(512, c.maxPlaceDistance));
        c.historyDepth = Math.max(0, c.historyDepth);
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(c, w); // 欠けている項目を既定値で書き戻す
        } catch (IOException e) {
            CityBuilderMod.LOG.warn("config.json を書けませんでした: {}", e.toString());
        }
        return c;
    }
}
