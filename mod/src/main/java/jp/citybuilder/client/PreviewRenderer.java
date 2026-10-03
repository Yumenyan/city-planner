package jp.citybuilder.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置プレビュー。クライアントは建物のブロックデータを持たないので、
 * 半透明の占有ボリューム + ワイヤーフレーム + 1段目の床面 + 正面マーカーで表現する。
 */
public final class PreviewRenderer {
    private PreviewRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(PreviewRenderer::render);
    }

    private static final class Box {
        final double x1, y1, z1, x2, y2, z2;
        final float r, g, b, fill;
        Box(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float fill) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1; this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.r = r; this.g = g; this.b = b; this.fill = fill;
        }
    }

    private static final class Line {
        final double x1, y1, z1, x2, y2, z2;
        final float r, g, b;
        Line(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1; this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.r = r; this.g = g; this.b = b;
        }
    }

    private static void addPlacement(List<Box> boxes, List<Line> lines) {
        boolean ok = ClientCatalog.canBuild;
        float cr = ok ? 0.2f : 1.0f, cg = ok ? 0.9f : 0.2f, cb = ok ? 0.4f : 0.2f;
        BlockPos t = PlacementMode.target;

        switch (PlacementMode.kind) {
            case BUILDING: {
                BlockPos o = PlacementMode.origin();
                if (o == null) return;
                int w = PlacementMode.rw(), l = PlacementMode.rl(), h = PlacementMode.entry.h;
                boxes.add(new Box(o.getX(), o.getY(), o.getZ(), o.getX() + w, o.getY() + h, o.getZ() + l, cr, cg, cb, 0.12f));
                boxes.add(new Box(o.getX(), o.getY(), o.getZ(), o.getX() + w, o.getY() + 1, o.getZ() + l, 1f, 1f, 0.3f, 0.25f));
                // 正面(回転0=南)。時計回りに rot 回転: 南→西→北→東
                int fdx = 0, fdz = 1;
                switch (PlacementMode.rot & 3) {
                    case 1: fdx = -1; fdz = 0; break;
                    case 2: fdx = 0; fdz = -1; break;
                    case 3: fdx = 1; fdz = 0; break;
                    default:
                }
                double cx = o.getX() + w / 2.0, cz = o.getZ() + l / 2.0, y = o.getY() + 1.02;
                double ex = cx + fdx * (w / 2.0), ez = cz + fdz * (l / 2.0);      // 正面の辺の中央
                double tx = ex + fdx * 4, tz = ez + fdz * 4;                        // 矢印の先
                lines.add(new Line(cx, y, cz, tx, y, tz, 1f, 0.9f, 0.1f));
                lines.add(new Line(tx, y, tz, tx - fdx * 1.5 - fdz * 1.5, y, tz - fdz * 1.5 + fdx * 1.5, 1f, 0.9f, 0.1f));
                lines.add(new Line(tx, y, tz, tx - fdx * 1.5 + fdz * 1.5, y, tz - fdz * 1.5 - fdx * 1.5, 1f, 0.9f, 0.1f));
                break;
            }
            case ROAD: {
                List<BlockPos> pts = PlacementMode.points;
                double hw = PlacementMode.roadWidth / 2.0;
                for (BlockPos p : pts) {
                    boxes.add(new Box(p.getX() + 0.5 - hw, p.getY(), p.getZ() + 0.5 - hw, p.getX() + 0.5 + hw, p.getY() + 1, p.getZ() + 0.5 + hw, 0.9f, 0.9f, 0.9f, 0.10f));
                }
                for (int i = 0; i + 1 < pts.size(); i++) {
                    BlockPos a = pts.get(i), b = pts.get(i + 1);
                    lines.add(new Line(a.getX() + 0.5, a.getY() + 1.05, a.getZ() + 0.5, b.getX() + 0.5, a.getY() + 1.05, b.getZ() + 0.5, 1f, 1f, 1f));
                }
                if (t != null) {
                    double y = pts.isEmpty() ? t.getY() : pts.get(0).getY();
                    boxes.add(new Box(t.getX() + 0.5 - hw, y, t.getZ() + 0.5 - hw, t.getX() + 0.5 + hw, y + 1, t.getZ() + 0.5 + hw, cr, cg, cb, 0.18f));
                    if (!pts.isEmpty()) {
                        BlockPos a = pts.get(pts.size() - 1);
                        lines.add(new Line(a.getX() + 0.5, y + 1.05, a.getZ() + 0.5, t.getX() + 0.5, y + 1.05, t.getZ() + 0.5, cr, cg, cb));
                    }
                }
                break;
            }
            case AREA: {
                BlockPos a = PlacementMode.areaFirst;
                if (a != null && t != null) {
                    int x1 = Math.min(a.getX(), t.getX()), x2 = Math.max(a.getX(), t.getX());
                    int z1 = Math.min(a.getZ(), t.getZ()), z2 = Math.max(a.getZ(), t.getZ());
                    boxes.add(new Box(x1, a.getY(), z1, x2 + 1, a.getY() + 1, z2 + 1, cr, cg, cb, 0.25f));
                } else if (t != null) {
                    boxes.add(new Box(t.getX(), t.getY(), t.getZ(), t.getX() + 1, t.getY() + 1, t.getZ() + 1, cr, cg, cb, 0.3f));
                }
                break;
            }
            default:
                return;
        }
    }

    /** /citybuilder plan load で出したプレビュー(建物=青、道路=白、区画=緑、全体の範囲=黄) */
    private static void addPlan(List<Box> boxes, List<Line> lines) {
        for (int[] s : PlanPreview.shapes) {
            switch (s[0]) {
                case 0: boxes.add(new Box(s[1], s[2], s[3], s[4] + 1, s[5] + 1, s[6] + 1, 0.3f, 0.7f, 1.0f, 0.12f)); break;
                case 1: boxes.add(new Box(s[1], s[2], s[3], s[4] + 1, s[5] + 1, s[6] + 1, 0.95f, 0.95f, 0.95f, 0.15f)); break;
                default: boxes.add(new Box(s[1], s[2], s[3], s[4] + 1, s[5] + 1, s[6] + 1, 0.4f, 1.0f, 0.4f, 0.15f));
            }
        }
        int[] b = PlanPreview.bounds;
        if (b != null) {
            double y = PlanPreview.baseY + 0.05, top = y + 40;
            double x1 = b[0], z1 = b[1], x2 = b[2] + 1, z2 = b[3] + 1;
            lines.add(new Line(x1, y, z1, x2, y, z1, 1f, 0.85f, 0.1f));
            lines.add(new Line(x2, y, z1, x2, y, z2, 1f, 0.85f, 0.1f));
            lines.add(new Line(x2, y, z2, x1, y, z2, 1f, 0.85f, 0.1f));
            lines.add(new Line(x1, y, z2, x1, y, z1, 1f, 0.85f, 0.1f));
            lines.add(new Line(x1, y, z1, x1, top, z1, 1f, 0.85f, 0.1f));
            lines.add(new Line(x2, y, z1, x2, top, z1, 1f, 0.85f, 0.1f));
            lines.add(new Line(x2, y, z2, x2, top, z2, 1f, 0.85f, 0.1f));
            lines.add(new Line(x1, y, z2, x1, top, z2, 1f, 0.85f, 0.1f));
        }
        BlockPos o = PlanPreview.origin;
        if (o != null) { // 原点マーカー(赤い十字)
            double y = o.getY() + 1.1;
            lines.add(new Line(o.getX() - 2, y, o.getZ() + 0.5, o.getX() + 3, y, o.getZ() + 0.5, 1f, 0.2f, 0.2f));
            lines.add(new Line(o.getX() + 0.5, y, o.getZ() - 2, o.getX() + 0.5, y, o.getZ() + 3, 1f, 0.2f, 0.2f));
        }
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        List<Box> boxes = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        if (PlanPreview.active()) addPlan(boxes, lines);
        if (PlacementMode.active()) addPlacement(boxes, lines);
        if (boxes.isEmpty() && lines.isEmpty()) return;

        MatrixStack ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f pos = ms.peek().getPositionMatrix();
        Matrix3f nrm = ms.peek().getNormalMatrix();

        // この時点でRenderSystem側のモデルビューにカメラ回転が残っていると二重に回転して傾いて見える。
        // 頂点は上で変換済みなので、描画中だけ単位行列にする。
        MatrixStack mv = RenderSystem.getModelViewStack();
        mv.push();
        mv.peek().getPositionMatrix().loadIdentity();
        mv.peek().getNormalMatrix().loadIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        // 半透明の面
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Box b : boxes) fillBox(bb, pos, b);
        tess.draw();
        RenderSystem.depthMask(true);

        // 線: 地形に隠れる部分は薄く、見える部分は濃く
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
        RenderSystem.lineWidth(3.0f);
        RenderSystem.disableDepthTest();
        drawLines(tess, pos, nrm, boxes, lines, 0.35f);
        RenderSystem.enableDepthTest();
        drawLines(tess, pos, nrm, boxes, lines, 1.0f);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        mv.pop();
        RenderSystem.applyModelViewMatrix();
        ms.pop();
    }

    private static void drawLines(Tessellator tess, Matrix4f pos, Matrix3f nrm, List<Box> boxes, List<Line> lines, float alpha) {
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        for (Box b : boxes) {
            float a = alpha * (b.fill >= 0.2f ? 0.8f : 1.0f);
            double[] xs = {b.x1, b.x2}, ys = {b.y1, b.y2}, zs = {b.z1, b.z2};
            for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                line(bb, pos, nrm, xs[0], ys[i], zs[j], xs[1], ys[i], zs[j], b.r, b.g, b.b, a);
                line(bb, pos, nrm, xs[i], ys[0], zs[j], xs[i], ys[1], zs[j], b.r, b.g, b.b, a);
                line(bb, pos, nrm, xs[i], ys[j], zs[0], xs[i], ys[j], zs[1], b.r, b.g, b.b, a);
            }
        }
        for (Line l : lines) line(bb, pos, nrm, l.x1, l.y1, l.z1, l.x2, l.y2, l.z2, l.r, l.g, l.b, alpha);
        tess.draw();
    }

    private static void line(BufferBuilder bb, Matrix4f pos, Matrix3f nrm,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float r, float g, float b, float a) {
        float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) return;
        nx /= len; ny /= len; nz /= len;
        bb.vertex(pos, (float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(nrm, nx, ny, nz).next();
        bb.vertex(pos, (float) x2, (float) y2, (float) z2).color(r, g, b, a).normal(nrm, nx, ny, nz).next();
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float r, float g, float b, float a,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        bb.vertex(m, (float) ax, (float) ay, (float) az).color(r, g, b, a).next();
        bb.vertex(m, (float) bx, (float) by, (float) bz).color(r, g, b, a).next();
        bb.vertex(m, (float) cx, (float) cy, (float) cz).color(r, g, b, a).next();
        bb.vertex(m, (float) dx, (float) dy, (float) dz).color(r, g, b, a).next();
    }

    private static void fillBox(BufferBuilder bb, Matrix4f m, Box b) {
        float r = b.r, g = b.g, bl = b.b, a = b.fill;
        double x1 = b.x1, y1 = b.y1, z1 = b.z1, x2 = b.x2, y2 = b.y2, z2 = b.z2;
        quad(bb, m, r, g, bl, a, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // 下
        quad(bb, m, r, g, bl, a, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // 上
        quad(bb, m, r, g, bl, a, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // 北
        quad(bb, m, r, g, bl, a, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // 南
        quad(bb, m, r, g, bl, a, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // 西
        quad(bb, m, r, g, bl, a, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // 東
    }
}
