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
    /** 1tickに建築へ使う最大時間(ミリ秒)。超えると残りは次tickへ(サーバーのラグ防止) */
    public int maxMsPerTick = 30;
    /** 1人が同時に積める作業の数 */
    public int maxJobsPerPlayer = 6;
    /** 1人あたりの undo 履歴の合計ブロック数の上限(メモリ保護) */
    public long historyMaxBlocks = 4_000_000L;
    /** アップロードされた建物の最大登録数 */
    public int maxUploadedTemplates = 300;
    /** アップロードできるプランの最大項目数 */
    public int maxPlanItems = 5000;
    /** 同じプレイヤーのアップロード開始の最小間隔(tick) */
    public int uploadCooldownTicks = 40;
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
    /** クライアントからの .schem アップロードを許可する */
    public boolean allowUpload = true;
    /** アップロードに必要なOPレベル(allowedPlayers に載っていれば不要) */
    public int uploadPermissionLevel = 2;
    /** アップロードできる .schem の最大サイズ(バイト) */
    public int maxUploadBytes = 2_000_000;
    /** 同じIDのテンプレートの上書きを許可する */
    public boolean allowUploadOverwrite = false;
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
        c.maxMsPerTick = Math.max(5, Math.min(45, c.maxMsPerTick));
        c.maxJobsPerPlayer = Math.max(1, c.maxJobsPerPlayer);
        c.historyMaxBlocks = Math.max(100_000L, c.historyMaxBlocks);
        c.maxPlanItems = Math.max(10, Math.min(50_000, c.maxPlanItems));
        c.maxUploadBytes = Math.max(10_000, Math.min(16_000_000, c.maxUploadBytes));
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(c, w); // 欠けている項目を既定値で書き戻す
        } catch (IOException e) {
            CityBuilderMod.LOG.warn("config.json を書けませんでした: {}", e.toString());
        }
        return c;
    }
}
