package jp.citybuilder;

import net.minecraft.util.Identifier;

/** 通信チャンネル(1パケットは約32KBまでなので、アップロードは分割して送る)。S2C: catalog, plan_preview / C2S: request_catalog, place, road, area, undo, upload_begin, upload_chunk */
public final class NetIds {
    public static final Identifier CATALOG = new Identifier("citybuilder", "catalog");
    public static final Identifier REQUEST_CATALOG = new Identifier("citybuilder", "request_catalog");
    public static final Identifier PLACE = new Identifier("citybuilder", "place");
    public static final Identifier ROAD = new Identifier("citybuilder", "road");
    public static final Identifier AREA = new Identifier("citybuilder", "area");
    public static final Identifier UPLOAD_BEGIN = new Identifier("citybuilder", "upload_begin");
    public static final Identifier UPLOAD_CHUNK = new Identifier("citybuilder", "upload_chunk");
    public static final Identifier PLAN_PREVIEW = new Identifier("citybuilder", "plan_preview");
    public static final Identifier PLAN_ACTION = new Identifier("citybuilder", "plan_action");
    public static final Identifier UNDO = new Identifier("citybuilder", "undo");

    private NetIds() {}
}
