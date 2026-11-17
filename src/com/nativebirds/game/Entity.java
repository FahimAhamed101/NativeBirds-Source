package com.nativebirds.game;

/** Game entities. Pure Java so level and gameplay logic can be tested off-device. */
public abstract class Entity {
    public Body body;
    public boolean alive = true;

    public float x() {
        return body.position.x;
    }

    public float y() {
        return body.position.y;
    }
}
