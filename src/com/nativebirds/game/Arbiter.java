package com.nativebirds.game;

/**
 * Contact manifold between two bodies with warm-started accumulated impulses.
 * Follows the Box2D-lite design (Erin Catto, "Iterative Dynamics with Temporal Coherence").
 */
public final class Arbiter {
    public static final int MAX_CONTACTS = 2;

    public final Body a, b;
    public final Contact[] contacts = new Contact[]{new Contact(), new Contact()};
    public int numContacts;
    public float friction;
    public float restitution;

    private static final Contact[] SCRATCH = new Contact[]{new Contact(), new Contact()};

    public Arbiter(Body a, Body b) {
        this.a = a;
        this.b = b;
    }

    /** Identifies a contact across frames so accumulated impulses can be reused. */
    public static final class FeaturePair {
        public int inEdge1, outEdge1, inEdge2, outEdge2;

        public int key() {
            return inEdge1 | (outEdge1 << 8) | (inEdge2 << 16) | (outEdge2 << 24);
        }

        public void set(int i1, int o1, int i2, int o2) {
            inEdge1 = i1;
            outEdge1 = o1;
            inEdge2 = i2;
            outEdge2 = o2;
        }
    }

    public static final class Contact {
        public final Vec2 position = new Vec2();
        public final Vec2 normal = new Vec2();
        public final Vec2 r1 = new Vec2();
        public final Vec2 r2 = new Vec2();
        public float separation;
        public float Pn, Pt, Pnb;
        public float massNormal, massTangent, bias;
        public final FeaturePair feature = new FeaturePair();
    }

    /** Merge a fresh manifold into this arbiter, keeping impulses for matching features. */
    public void update(Contact[] fresh, int count) {
        int merged = 0;
        for (int i = 0; i < count; i++) {
            Contact src = fresh[i];
            Contact dst = SCRATCH[merged];
            dst.position.set(src.position);
            dst.normal.set(src.normal);
            dst.separation = src.separation;
            dst.feature.set(src.feature.inEdge1, src.feature.outEdge1,
                    src.feature.inEdge2, src.feature.outEdge2);
            dst.Pn = 0f;
            dst.Pt = 0f;
            dst.Pnb = 0f;
            for (int j = 0; j < numContacts; j++) {
                if (contacts[j].feature.key() == dst.feature.key()) {
                    dst.Pn = contacts[j].Pn;
                    dst.Pt = contacts[j].Pt;
                    dst.Pnb = contacts[j].Pnb;
                    break;
                }
            }
            merged++;
        }
        for (int i = 0; i < merged; i++) {
            Contact dst = contacts[i];
            Contact src = SCRATCH[i];
            dst.position.set(src.position);
            dst.normal.set(src.normal);
            dst.separation = src.separation;
            dst.feature.set(src.feature.inEdge1, src.feature.outEdge1,
                    src.feature.inEdge2, src.feature.outEdge2);
            dst.Pn = src.Pn;
            dst.Pt = src.Pt;
            dst.Pnb = src.Pnb;
        }
        numContacts = merged;
    }

    public void preStep(float invDt, float allowedPenetration, float biasFactor) {
        float invMassSum = a.invMass + b.invMass;
        if (invMassSum == 0f) {
            return;
        }
        friction = (float) Math.sqrt(a.friction * b.friction);
        restitution = Math.max(a.restitution, b.restitution);

        for (int i = 0; i < numContacts; i++) {
            Contact c = contacts[i];
            c.r1.set(c.position).sub(a.position);
            c.r2.set(c.position).sub(b.position);

            float rn1 = c.r1.cross(c.normal);
            float rn2 = c.r2.cross(c.normal);
            float kNormal = invMassSum + a.invInertia * rn1 * rn1 + b.invInertia * rn2 * rn2;
            c.massNormal = kNormal > 0 ? 1f / kNormal : 0f;

            float tx = c.normal.y, ty = -c.normal.x;
            float rt1 = c.r1.x * ty - c.r1.y * tx;
            float rt2 = c.r2.x * ty - c.r2.y * tx;
            float kTangent = invMassSum + a.invInertia * rt1 * rt1 + b.invInertia * rt2 * rt2;
            c.massTangent = kTangent > 0 ? 1f / kTangent : 0f;

            // Baumgarte-style positional bias, clamped so deep overlaps don't explode.
            c.bias = -biasFactor * invDt * Math.min(0f, c.separation + allowedPenetration);
            if (c.bias > 3.0f) {
                c.bias = 3.0f;
            }

            // warm start
            float px = c.normal.x * c.Pn + tx * c.Pt;
            float py = c.normal.y * c.Pn + ty * c.Pt;
            a.velocity.x -= px * a.invMass;
            a.velocity.y -= py * a.invMass;
            a.angularVelocity -= a.invInertia * (c.r1.x * py - c.r1.y * px);
            b.velocity.x += px * b.invMass;
            b.velocity.y += py * b.invMass;
            b.angularVelocity += b.invInertia * (c.r2.x * py - c.r2.y * px);
        }
    }

    public void applyImpulse() {
        for (int i = 0; i < numContacts; i++) {
            Contact c = contacts[i];
            float nx = c.normal.x, ny = c.normal.y;
            float tx = ny, ty = -nx;

            // relative velocity at contact
            float dvx = b.velocity.x - b.angularVelocity * c.r2.y - a.velocity.x + a.angularVelocity * c.r1.y;
            float dvy = b.velocity.y + b.angularVelocity * c.r2.x - a.velocity.y - a.angularVelocity * c.r1.x;

            float vn = dvx * nx + dvy * ny;

            float dPn = c.massNormal * (-vn + c.bias);
            float Pn0 = c.Pn;
            c.Pn = Math.max(Pn0 + dPn, 0f);
            dPn = c.Pn - Pn0;

            float px = nx * dPn, py = ny * dPn;
            a.velocity.x -= px * a.invMass;
            a.velocity.y -= py * a.invMass;
            a.angularVelocity -= a.invInertia * (c.r1.x * py - c.r1.y * px);
            b.velocity.x += px * b.invMass;
            b.velocity.y += py * b.invMass;
            b.angularVelocity += b.invInertia * (c.r2.x * py - c.r2.y * px);

            if (dPn > 0f) {
                a.impact += dPn;
                b.impact += dPn;
            }

            // friction
            dvx = b.velocity.x - b.angularVelocity * c.r2.y - a.velocity.x + a.angularVelocity * c.r1.y;
            dvy = b.velocity.y + b.angularVelocity * c.r2.x - a.velocity.y - a.angularVelocity * c.r1.x;
            float vt = dvx * tx + dvy * ty;

            float dPt = c.massTangent * (-vt);
            float maxPt = friction * c.Pn;
            float oldPt = c.Pt;
            c.Pt = Math.max(-maxPt, Math.min(maxPt, oldPt + dPt));
            dPt = c.Pt - oldPt;

            px = tx * dPt;
            py = ty * dPt;
            a.velocity.x -= px * a.invMass;
            a.velocity.y -= py * a.invMass;
            a.angularVelocity -= a.invInertia * (c.r1.x * py - c.r1.y * px);
            b.velocity.x += px * b.invMass;
            b.velocity.y += py * b.invMass;
            b.angularVelocity += b.invInertia * (c.r2.x * py - c.r2.y * px);
        }
    }
}
