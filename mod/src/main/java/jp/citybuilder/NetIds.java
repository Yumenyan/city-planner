package jp.citybuilder;

import net.minecraft.util.Identifier;

/** 通信チャンネル。S2C: catalog / C2S: request_catalog, place, road, area, undo */
public final class NetIds {
    public static final Identifier CATALOG = new Identifier("citybuilder", "catalog");
    public static final Identifier REQUEST_CATALOG = new Identifier("citybuilder", "request_catalog");
    public static final Identifier PLACE = new Identifier("citybuilder", "place");
    public static final Identifier ROAD = new Identifier("citybuilder", "road");
    public static final Identifier AREA = new Identifier("citybuilder", "area");
    public static final Identifier UNDO = new Identifier("citybuilder", "undo");

    private NetIds() {}
}
