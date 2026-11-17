package com.nativebirds.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sequential-impulse rigid body world.
 * Fixed-step integration with warm-started contact caching.
 */
public final class World {
    public final Vec2 gravity = new Vec2(0f, -22f);
    public final List<Body> bodies = new ArrayList<Body>();

    public int velocityIterations = 10;
    public float allowedPenetration = 0.008f;
    public float biasFactor = 0.22f;

    private final Map<Long, Arbiter> arbiters = new HashMap<Long, Arbiter>();
    private final Map<Long, Arbiter> fresh = new HashMap<Long, Arbiter>();
    private final Arbiter.Contact[] scratch = new Arbiter.Contact[]{
            new Arbiter.Contact(), new Arbiter.Contact()};

    /** Total normal impulse resolved during the most recent step (all contacts). */
    public float totalImpact;

    public Body add(Body b) {
        bodies.add(b);
        return b;
    }

    public void remove(Body b) {
        bodies.remove(b);
        b.dead = true;
    }

    public void clear() {
        bodies.clear();
        arbiters.clear();
        fresh.clear();
    }

    private static long key(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xffffffffL);
    }

    public void step(float dt) {
        totalImpact = 0f;
        int n = bodies.size();
        for (int i = 0; i < n; i++) {
            Body b = bodies.get(i);
            b.impact = 0f;
            b.updateAABB();
        }

        // ---- broad + narrow phase -------------------------------------------
        fresh.clear();
        for (int i = 0; i < n; i++) {
            Body a = bodies.get(i);
            if (a.dead) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                Body b = bodies.get(j);
                if (b.dead || (a.isStatic && b.isStatic)) {
                    continue;
                }
                if (a.maxX < b.minX || b.maxX < a.minX || a.maxY < b.minY || b.maxY < a.minY) {
                    continue;
                }
                int count = Collide.collide(a, b, scratch);
                if (count == 0) {
                    continue;
                }
                long k = key(a.id, b.id);
                Arbiter arb = arbiters.get(k);
                if (arb == null) {
                    arb = new Arbiter(a, b);
                }
                arb.update(scratch, count);
                if (arb.numContacts > 0) {
                    fresh.put(k, arb);
                }
            }
        }
        arbiters.clear();
        arbiters.putAll(fresh);

        // ---- integrate forces ------------------------------------------------
        for (int i = 0; i < n; i++) {
            Body b = bodies.get(i);
            if (b.isStatic) {
                continue;
            }
            b.velocity.x += dt * (gravity.x + b.force.x * b.invMass);
            b.velocity.y += dt * (gravity.y + b.force.y * b.invMass);
            b.angularVelocity += dt * b.torque * b.invInertia;

            float ld = 1f / (1f + dt * b.linearDamping);
            float ad = 1f / (1f + dt * b.angularDamping);
            b.velocity.mul(ld);
            b.angularVelocity *= ad;
        }

        // ---- solve velocity constraints -------------------------------------
        float invDt = dt > 0f ? 1f / dt : 0f;
        for (Arbiter a : arbiters.values()) {
            a.preStep(invDt, allowedPenetration, biasFactor);
        }
        for (int it = 0; it < velocityIterations; it++) {
            for (Arbiter a : arbiters.values()) {
                a.applyImpulse();
            }
        }

        // ---- integrate positions ---------------------------------------------
        for (int i = 0; i < n; i++) {
            Body b = bodies.get(i);
            if (b.isStatic) {
                continue;
            }
            b.position.x += dt * b.velocity.x;
            b.position.y += dt * b.velocity.y;
            b.rotation += dt * b.angularVelocity;

            b.force.set(0f, 0f);
            b.torque = 0f;

            float sp2 = b.velocity.lengthSquared();
            if (sp2 > 4900f) {
                b.velocity.mul(70f / (float) Math.sqrt(sp2));
            }
            if (b.angularVelocity > 45f) {
                b.angularVelocity = 45f;
            } else if (b.angularVelocity < -45f) {
                b.angularVelocity = -45f;
            }
            b.impact += 0f;
            totalImpact += b.impact;
        }
    }

    /** True when every dynamic body is nearly at rest. */
    public boolean isSettled(float threshold) {
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            if (b.isStatic || b.dead) {
                continue;
            }
            if (b.motion() > threshold) {
                return false;
            }
        }
        return true;
    }

    public void applyRadialImpulse(float x, float y, float radius, float strength) {
        for (int i = 0; i < bodies.size(); i++) {
            Body b = bodies.get(i);
            if (b.isStatic || b.dead) {
                continue;
            }
            float dx = b.position.x - x;
            float dy = b.position.y - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > radius) {
                continue;
            }
            float falloff = 1f - d / radius;
            float inv = d > 1e-3f ? 1f / d : 0f;
            float imp = strength * falloff * b.mass;
            b.velocity.x += dx * inv * imp * b.invMass;
            b.velocity.y += dy * inv * imp * b.invMass;
            b.angularVelocity += (float) (Math.random() - 0.5) * 4f * falloff;
        }
    }
}
