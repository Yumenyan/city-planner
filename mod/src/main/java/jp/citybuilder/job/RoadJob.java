package jp.citybuilder.job;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * 折れ線の道路。幅は奇数推奨(中心線の左右対称)。
 * y は路面ブロックの高さ。交差点: 後から引く道路の路面が既存の線を上書きし、歩道は既存の道路に重ならない。
 */
public final class RoadJob extends Job {
    public static final int SIDEWALK_W = 2;

    private final int[] px, pz;
    private final double halfW;
    private final boolean sidewalk;
    private final Styles.Road style;
    private final String lines;
    private final int y, clearAbove, foundDepth;
    private final boolean foundation;
    private final int minX, maxX, minZ, maxZ;
    private final double[] cum;
    private int cx, cz;
    private double nx, nz; // 直近の classify で最寄りだった線分の単位法線

    public RoadJob(UUID owner, ServerWorld world, int[] px, int[] pz, int width, boolean sidewalk,
                   Styles.Road style, String lines, int y, int clearAbove, boolean foundation, int foundDepth) {
        super(owner, "road", world);
        this.px = px; this.pz = pz;
        this.halfW = (Math.max(1, width) - 1) / 2.0;
        this.sidewalk = sidewalk;
        this.style = style;
        this.lines = (width >= 5 && lines != null) ? lines : "none";
        this.y = y; this.clearAbove = clearAbove; this.foundation = foundation; this.foundDepth = foundDepth;
        int mnx = Integer.MAX_VALUE, mxx = Integer.MIN_VALUE, mnz = Integer.MAX_VALUE, mxz = Integer.MIN_VALUE;
        for (int i = 0; i < px.length; i++) {
            mnx = Math.min(mnx, px[i]); mxx = Math.max(mxx, px[i]);
            mnz = Math.min(mnz, pz[i]); mxz = Math.max(mxz, pz[i]);
        }
        int m = (int) Math.ceil(halfW) + (sidewalk ? SIDEWALK_W : 0) + 1;
        minX = mnx - m; maxX = mxx + m; minZ = mnz - m; maxZ = mxz + m;
        cum = new double[px.length];
        for (int i = 1; i < px.length; i++) {
            cum[i] = cum[i - 1] + Math.hypot(px[i] - px[i - 1], pz[i] - pz[i - 1]);
        }
        cx = minX; cz = minZ;
    }

    public static int[] bounds(int[] px, int[] pz, int width, boolean sidewalk) {
        int mnx = Integer.MAX_VALUE, mxx = Integer.MIN_VALUE, mnz = Integer.MAX_VALUE, mxz = Integer.MIN_VALUE;
        for (int i = 0; i < px.length; i++) {
            mnx = Math.min(mnx, px[i]); mxx = Math.max(mxx, px[i]);
            mnz = Math.min(mnz, pz[i]); mxz = Math.max(mxz, pz[i]);
        }
        int m = (int) Math.ceil((Math.max(1, width) - 1) / 2.0) + (sidewalk ? SIDEWALK_W : 0) + 1;
        return new int[]{mnx - m, mnz - m, mxx + m, mxz + m};
    }

    @Override
    public long estimate() {
        return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    private static final int NONE = 0, SURFACE = 1, WHITE = 2, YELLOW = 3, WALK = 4;

    private int classify(int x, int z) {
        double best = Double.MAX_VALUE, along = 0;
        nx = 0; nz = 0;
        if (px.length == 1) {
            best = Math.hypot(x - px[0], z - pz[0]);
        } else {
            for (int i = 0; i + 1 < px.length; i++) {
                double ax = px[i], az = pz[i], dx = px[i + 1] - ax, dz = pz[i + 1] - az;
                double len2 = dx * dx + dz * dz;
                double t = len2 == 0 ? 0 : ((x - ax) * dx + (z - az) * dz) / len2;
                t = Math.max(0, Math.min(1, t));
                double d = Math.hypot(x - (ax + t * dx), z - (az + t * dz));
                if (d < best) {
                    best = d; along = cum[i] + t * Math.sqrt(len2);
                    double len = Math.sqrt(len2);
                    if (len > 0) { nx = -dz / len; nz = dx / len; }
                }
            }
        }
        if (best <= halfW + 1e-6) {
            switch (lines) {
                case "dashed":
                    if (best < 0.5 && (((int) Math.floor(along)) % 8) < 4) return WHITE;
                    break;
                case "solid":
                    if (best < 0.5) return WHITE;
                    break;
                case "double":
                    if (best >= 0.5 && best < 1.5) return YELLOW;
                    break;
                default:
            }
            return SURFACE;
        }
        if (sidewalk && best <= halfW + SIDEWALK_W + 1e-6) return WALK;
        return NONE;
    }

    /**
     * 交差判定: この道路の両脇(歩道の外側)に別の道路の路面があれば、他の道路と交わっているとみなす。
     * 既存の道路ブロックの上に引き直した場合など、交差していないときは中央線を消さない。
     */
    private boolean crossing(int x, int z) {
        if (nx == 0 && nz == 0) return false;
        double d = halfW + (sidewalk ? SIDEWALK_W : 0) + 1.5;
        for (int s = -1; s <= 1; s += 2) {
            int qx = (int) Math.round(x + s * nx * d);
            int qz = (int) Math.round(z + s * nz * d);
            if (Styles.isRoadBlock(get(qx, y, qz))) return true;
        }
        return false;
    }

    @Override
    public int step(int budget) {
        int used = 0;
        while (used < budget) {
            if (cz > maxZ) { done = true; break; }
            used++;
            int code = classify(cx, cz);
            if (code != NONE) {
                BlockState cur = get(cx, y, cz);
                boolean existingRoad = Styles.isRoadBlock(cur);
                BlockState place = null;
                if (code == SURFACE) place = style.surface;
                else if (code == WHITE) place = (existingRoad && crossing(cx, cz)) ? style.surface : style.white;
                else if (code == YELLOW) place = (existingRoad && crossing(cx, cz)) ? style.surface : style.yellow;
                else if (code == WALK && !existingRoad) place = style.sidewalk;
                if (place != null) {
                    used += put(cx, y, cz, place);
                    for (int d = 1; d <= clearAbove; d++) {
                        if (!get(cx, y + d, cz).isAir()) used += put(cx, y + d, cz, Blocks.AIR.getDefaultState());
                    }
                    if (foundation) used += foundation(cx, y, cz, foundDepth, Blocks.STONE.getDefaultState());
                }
            }
            cx++;
            if (cx > maxX) { cx = minX; cz++; }
        }
        if (cz > maxZ) done = true;
        return used;
    }
}
