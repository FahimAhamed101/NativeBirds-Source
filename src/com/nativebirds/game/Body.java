package com.nativebirds.game;

/**
 * A rigid body: circle or oriented box, with mass properties derived from density.
 * Pure Java (no Android imports) so the solver can be unit-tested on the desktop JVM.
 */
public final class Body {
    public static final int CIRCLE = 0;
    public static final int BOX = 1;

    private static int nextId = 1;

    public final int id = nextId++;
    public int shape = BOX;

    public final Vec2 position = new Vec2();
    public float rotation;
    public final Vec2 velocity = new Vec2();
    public float angularVelocity;

    public final Vec2 force = new Vec2();
    public float torque;

    public float mass = 1f, invMass = 1f;
    public float inertia = 1f, invInertia = 1f;

    /** Circle radius. */
    public float radius;
    /** Box half extents. */
    public float hw, hh;

    public float friction = 0.55f;
    public float restitution = 0.02f;
    public float linearDamping = 0.04f;
    public float angularDamping = 0.08f;

    public boolean isStatic;
    /** Accumulated normal impulse during the last step (used for damage + sound). */
    public float impact;
    public boolean dead;
    public Object userData;

    // cached AABB
    public float minX, minY, maxX, maxY;

    public Body setCircle(float r, float density) {
        shape = CIRCLE;
        radius = r;
        float m = (float) Math.PI * r * r * density;
        setMass(m, 0.5f * m * r * r);
        return this;
    }

    public Body setBox(float w, float h, float density) {
        shape = BOX;
        hw = w * 0.5f;
        hh = h * 0.5f;
        float m = w * h * density;
        setMass(m, m * (w * w + h * h) / 12f);
        return this;
    }

    public Body setMass(float m, float i) {
        mass = m;
        inertia = i;
        invMass = m > 0 ? 1f / m : 0f;
        invInertia = i > 0 ? 1f / i : 0f;
        return this;
    }

    public Body setStatic() {
        isStatic = true;
        mass = inertia = 0f;
        invMass = invInertia = 0f;
        velocity.set(0, 0);
        angularVelocity = 0;
        return this;
    }

    public Body set(float x, float y, float angle) {
        position.set(x, y);
        rotation = angle;
        return this;
    }

    public Vec2 worldPoint(float lx, float ly, Vec2 out) {
        float c = (float) Math.cos(rotation), s = (float) Math.sin(rotation);
        out.x = position.x + c * lx - s * ly;
        out.y = position.y + s * lx + c * ly;
        return out;
    }

    public void updateAABB() {
        if (shape == CIRCLE) {
            minX = position.x - radius;
            maxX = position.x + radius;
            minY = position.y - radius;
            maxY = position.y + radius;
        } else {
            float c = (float) Math.cos(rotation), s = (float) Math.sin(rotation);
            float ex = Math.abs(c) * hw + Math.abs(s) * hh;
            float ey = Math.abs(s) * hw + Math.abs(c) * hh;
            minX = position.x - ex;
            maxX = position.x + ex;
            minY = position.y - ey;
            maxY = position.y + ey;
        }
    }

    public boolean containsPoint(float x, float y) {
        float dx = x - position.x, dy = y - position.y;
        if (shape == CIRCLE) {
            return dx * dx + dy * dy <= radius * radius;
        }
        float c = (float) Math.cos(rotation), s = (float) Math.sin(rotation);
        float lx = c * dx + s * dy;
        float ly = -s * dx + c * dy;
        return Math.abs(lx) <= hw && Math.abs(ly) <= hh;
    }

    public float speed() {
        return velocity.length();
    }

    /** Total kinetic energy proxy - used to detect that the level has settled. */
    public float motion() {
        return velocity.lengthSquared() + angularVelocity * angularVelocity * 0.15f;
    }

    public void wake() {
        // no sleeping implemented; bodies are always simulated
    }
}
