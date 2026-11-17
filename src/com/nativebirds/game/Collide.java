package com.nativebirds.game;

/**
 * Narrow-phase collision detection.
 * Circles use closest-point; boxes use SAT with reference-face selection and
 * incident-edge clipping (2 contact points), following Box2D-lite's Collide.cpp.
 */
public final class Collide {

    private static final Vec2[] VA = new Vec2[4];
    private static final Vec2[] VB = new Vec2[4];
    private static final Vec2[] NA = new Vec2[4];
    private static final Vec2[] NB = new Vec2[4];
    private static final Vec2[] CLIP = new Vec2[4];
    private static final int[] EDGE = new int[1];

    static {
        for (int i = 0; i < 4; i++) {
            VA[i] = new Vec2();
            VB[i] = new Vec2();
            NA[i] = new Vec2();
            NB[i] = new Vec2();
            CLIP[i] = new Vec2();
        }
    }

    private Collide() {
    }

    public static int collide(Body a, Body b, Arbiter.Contact[] out) {
        if (a.shape == Body.CIRCLE) {
            if (b.shape == Body.CIRCLE) {
                return circleCircle(a, b, out);
            }
            return circleBox(a, b, out, false);
        }
        if (b.shape == Body.CIRCLE) {
            return circleBox(b, a, out, true);
        }
        return boxBox(a, b, out);
    }

    // ---------------------------------------------------------------- circles

    private static int circleCircle(Body a, Body b, Arbiter.Contact[] out) {
        float dx = b.position.x - a.position.x;
        float dy = b.position.y - a.position.y;
        float r = a.radius + b.radius;
        float d2 = dx * dx + dy * dy;
        if (d2 > r * r) {
            return 0;
        }
        float d = (float) Math.sqrt(d2);
        Arbiter.Contact c = out[0];
        if (d > 1e-5f) {
            c.normal.set(dx / d, dy / d);
        } else {
            c.normal.set(1, 0);
        }
        c.separation = d - r;
        c.position.set(a.position).addScaled(c.normal, a.radius);
        c.feature.set(0, 0, 0, 0);
        return 1;
    }

    /** Normal points from the box toward the circle. If boxIsA the arbiter normal is flipped. */
    private static int circleBox(Body circle, Body box, Arbiter.Contact[] out, boolean boxIsA) {
        float cs = (float) Math.cos(box.rotation), sn = (float) Math.sin(box.rotation);
        float dx = circle.position.x - box.position.x;
        float dy = circle.position.y - box.position.y;

        // circle centre in box local space
        float lx = cs * dx + sn * dy;
        float ly = -sn * dx + cs * dy;

        float qx = Math.max(-box.hw, Math.min(box.hw, lx));
        float qy = Math.max(-box.hh, Math.min(box.hh, ly));

        float nx, ny, sep;
        float ddx = lx - qx, ddy = ly - qy;
        float d2 = ddx * ddx + ddy * ddy;

        if (d2 > 1e-8f) {
            float d = (float) Math.sqrt(d2);
            sep = d - circle.radius;
            if (sep > 0f) {
                return 0;
            }
            nx = ddx / d;
            ny = ddy / d;
        } else {
            // centre inside the box - push out along the shallowest axis
            float px = box.hw - Math.abs(lx);
            float py = box.hh - Math.abs(ly);
            if (px < py) {
                nx = lx >= 0 ? 1f : -1f;
                ny = 0f;
                sep = -px - circle.radius;
            } else {
                nx = 0f;
                ny = ly >= 0 ? 1f : -1f;
                sep = -py - circle.radius;
            }
            qx = nx * box.hw;
            qy = ny * box.hh;
        }

        // world-space outward normal (box -> circle)
        float wnx = cs * nx - sn * ny;
        float wny = sn * nx + cs * ny;

        float px = box.position.x + cs * qx - sn * qy;
        float py = box.position.y + sn * qx + cs * qy;

        Arbiter.Contact c = out[0];
        c.separation = sep;
        c.position.set(px, py);
        if (boxIsA) {
            c.normal.set(wnx, wny);      // A = box, B = circle
        } else {
            c.normal.set(-wnx, -wny);    // A = circle, B = box
        }
        c.feature.set(0, 0, 0, 0);
        return 1;
    }

    // ------------------------------------------------------------------- boxes

    private static int boxBox(Body a, Body b, Arbiter.Contact[] out) {
        buildBox(a, VA, NA);
        buildBox(b, VB, NB);

        float sepA = maxSeparation(VA, NA, VB);
        int edgeA = EDGE[0];
        if (sepA > 0f) {
            return 0;
        }
        float sepB = maxSeparation(VB, NB, VA);
        int edgeB = EDGE[0];
        if (sepB > 0f) {
            return 0;
        }

        Vec2[] refV, incV, refN, incN;
        int refEdge;
        boolean flip;
        if (sepB > sepA + 5e-4f) {
            refV = VB;
            refN = NB;
            incV = VA;
            incN = NA;
            refEdge = edgeB;
            flip = true;      // reference is B, so the face normal points B -> A
        } else {
            refV = VA;
            refN = NA;
            incV = VB;
            incN = NB;
            refEdge = edgeA;
            flip = false;
        }

        Vec2 refNormal = refN[refEdge];

        // incident edge = the edge of the other box most anti-parallel to the reference normal
        int incIdx = 0;
        float minDot = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float d = refNormal.x * incN[i].x + refNormal.y * incN[i].y;
            if (d < minDot) {
                minDot = d;
                incIdx = i;
            }
        }

        Vec2 v1 = refV[refEdge];
        Vec2 v2 = refV[(refEdge + 1) & 3];

        CLIP[0].set(incV[incIdx]);
        CLIP[1].set(incV[(incIdx + 1) & 3]);
        int clipCount = 2;

        // side planes of the reference face
        float sx = v2.x - v1.x, sy = v2.y - v1.y;
        float sl = (float) Math.sqrt(sx * sx + sy * sy);
        if (sl < 1e-6f) {
            return 0;
        }
        sx /= sl;
        sy /= sl;

        clipCount = clip(-sx, -sy, -(sx * v1.x + sy * v1.y), clipCount);
        if (clipCount < 2) {
            return 0;
        }
        clipCount = clip(sx, sy, sx * v2.x + sy * v2.y, clipCount);
        if (clipCount < 2) {
            return 0;
        }

        float refC = refNormal.x * v1.x + refNormal.y * v1.y;
        int n = 0;
        for (int i = 0; i < 2; i++) {
            Vec2 p = CLIP[i];
            float sep = refNormal.x * p.x + refNormal.y * p.y - refC;
            if (sep <= 0.002f) {
                Arbiter.Contact c = out[n];
                c.separation = Math.min(sep, 0f);
                if (flip) {
                    c.normal.set(-refNormal.x, -refNormal.y);
                } else {
                    c.normal.set(refNormal);
                }
                c.position.set(p.x - sep * refNormal.x, p.y - sep * refNormal.y);
                c.feature.set(refEdge, i, incIdx, flip ? 1 : 0);
                n++;
                if (n == 2) {
                    break;
                }
            }
        }
        return n;
    }

    private static void buildBox(Body body, Vec2[] v, Vec2[] nrm) {
        float c = (float) Math.cos(body.rotation), s = (float) Math.sin(body.rotation);
        float hw = body.hw, hh = body.hh;
        float[] lx = {-hw, hw, hw, -hw};
        float[] ly = {-hh, -hh, hh, hh};
        for (int i = 0; i < 4; i++) {
            v[i].set(body.position.x + c * lx[i] - s * ly[i],
                    body.position.y + s * lx[i] + c * ly[i]);
        }
        for (int i = 0; i < 4; i++) {
            Vec2 p = v[i], q = v[(i + 1) & 3];
            float dx = q.x - p.x, dy = q.y - p.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-6f) {
                len = 1e-6f;
            }
            nrm[i].set(dy / len, -dx / len);
        }
    }

    private static float maxSeparation(Vec2[] va, Vec2[] na, Vec2[] vb) {
        float best = -Float.MAX_VALUE;
        int bestI = 0;
        for (int i = 0; i < 4; i++) {
            float nx = na[i].x, ny = na[i].y;
            float vx = va[i].x, vy = va[i].y;
            float minS = Float.MAX_VALUE;
            for (int j = 0; j < 4; j++) {
                float s = nx * (vb[j].x - vx) + ny * (vb[j].y - vy);
                if (s < minS) {
                    minS = s;
                }
            }
            if (minS > best) {
                best = minS;
                bestI = i;
            }
        }
        EDGE[0] = bestI;
        return best;
    }

    private static final Vec2[] CLIP_OUT = new Vec2[]{new Vec2(), new Vec2()};

    /** Keep the part of the segment on the negative side of the plane, preserving order. */
    private static int clip(float nx, float ny, float c, int count) {
        Vec2 v0 = CLIP[0], v1 = CLIP[1];
        float d0 = nx * v0.x + ny * v0.y - c;
        float d1 = nx * v1.x + ny * v1.y - c;

        int n = 0;
        if (d0 <= 0f) {
            CLIP_OUT[n++].set(v0);
        }
        if (d1 <= 0f) {
            CLIP_OUT[n++].set(v1);
        }
        if (d0 * d1 < 0f) {
            float t = d0 / (d0 - d1);
            float ix = v0.x + (v1.x - v0.x) * t;
            float iy = v0.y + (v1.y - v0.y) * t;
            if (d0 <= 0f) {
                CLIP_OUT[n++].set(ix, iy);
            } else {
                // intersection precedes the surviving endpoint
                CLIP_OUT[1].set(CLIP_OUT[0]);
                CLIP_OUT[0].set(ix, iy);
                n = 2;
            }
        }
        for (int i = 0; i < n; i++) {
            CLIP[i].set(CLIP_OUT[i]);
        }
        return n;
    }
}
