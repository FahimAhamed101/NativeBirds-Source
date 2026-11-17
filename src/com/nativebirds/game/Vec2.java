package com.nativebirds.game;

/** Minimal mutable 2D vector used by the physics solver. */
public final class Vec2 {
    public float x, y;

    public Vec2() {
    }

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(Vec2 v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vec2 set(float nx, float ny) {
        x = nx;
        y = ny;
        return this;
    }

    public Vec2 set(Vec2 v) {
        x = v.x;
        y = v.y;
        return this;
    }

    public Vec2 add(Vec2 v) {
        x += v.x;
        y += v.y;
        return this;
    }

    public Vec2 sub(Vec2 v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    public Vec2 mul(float s) {
        x *= s;
        y *= s;
        return this;
    }

    public Vec2 addScaled(Vec2 v, float s) {
        x += v.x * s;
        y += v.y * s;
        return this;
    }

    public float dot(Vec2 v) {
        return x * v.x + y * v.y;
    }

    public float cross(Vec2 v) {
        return x * v.y - y * v.x;
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float lengthSquared() {
        return x * x + y * y;
    }

    public Vec2 normalize() {
        float len = length();
        if (len > 1e-8f) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public Vec2 copyFrom(float nx, float ny) {
        x = nx;
        y = ny;
        return this;
    }
}
