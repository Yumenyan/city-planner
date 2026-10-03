package jp.citybuilder;

/** X/Z の矩形範囲(両端を含む)。config の buildAreas / protectedAreas で使用。 */
public final class IntBox {
    public int x1, z1, x2, z2;

    public IntBox() {}

    public IntBox(int x1, int z1, int x2, int z2) {
        this.x1 = x1; this.z1 = z1; this.x2 = x2; this.z2 = z2;
        normalize();
    }

    public void normalize() {
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        if (z1 > z2) { int t = z1; z1 = z2; z2 = t; }
    }

    public boolean intersects(int ax1, int az1, int ax2, int az2) {
        return ax1 <= x2 && ax2 >= x1 && az1 <= z2 && az2 >= z1;
    }

    public boolean contains(int ax1, int az1, int ax2, int az2) {
        return ax1 >= x1 && ax2 <= x2 && az1 >= z1 && az2 <= z2;
    }
}
