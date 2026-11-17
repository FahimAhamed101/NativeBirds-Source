import com.nativebirds.game.*;

/** Headless validation of the solver: stacking, resting, impact, NaN safety. */
public class PhysicsTest {

    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name + "   " + detail);
        if (!ok) failures++;
    }

    static World worldWithGround() {
        World w = new World();
        Body g = new Body().setBox(200f, 2f, 1f).set(0f, -1f, 0f);
        g.setStatic();
        g.friction = 0.9f;
        w.add(g);
        return w;
    }

    static void run(World w, float seconds) {
        int steps = (int) (seconds * 120f);
        for (int i = 0; i < steps; i++) w.step(1f / 120f);
    }

    public static void main(String[] args) {
        // 1. single box rests
        {
            World w = worldWithGround();
            Body b = w.add(new Body().setBox(1f, 1f, 1f).set(0f, 3f, 0f));
            run(w, 3f);
            float rest = b.position.y;
            check("box rests on ground", Math.abs(rest - 0.5f) < 0.02f,
                    "y=" + rest + " (expect 0.5)");
            check("box stops moving", b.velocity.length() < 0.02f, "|v|=" + b.velocity.length());
        }

        // 2. stack of 6 boxes stays put
        {
            World w = worldWithGround();
            Body[] bs = new Body[6];
            for (int i = 0; i < 6; i++) {
                bs[i] = w.add(new Body().setBox(1f, 1f, 1f).set(0f, 0.5f + i * 1.0f, 0f));
            }
            run(w, 4f);
            float top = bs[5].position.y;
            float drift = Math.abs(bs[5].position.x);
            check("6-high stack keeps height", Math.abs(top - 5.5f) < 0.06f,
                    "top y=" + top + " (expect 5.5)");
            check("6-high stack does not slide", drift < 0.05f, "x drift=" + drift);
            boolean finite = true;
            for (Body b : bs) {
                if (Float.isNaN(b.position.y) || Math.abs(b.position.y) > 100) finite = false;
            }
            check("stack values finite", finite, "");
        }

        // 3. circle drops and rests without bouncing forever
        {
            World w = worldWithGround();
            Body c = w.add(new Body().setCircle(1f, 1f).set(0f, 8f, 0f));
            c.restitution = 0.15f;
            run(w, 5f);
            check("ball settles on ground", Math.abs(c.position.y - 1f) < 0.05f, "y=" + c.position.y);
            check("ball stops bouncing", c.velocity.length() < 0.05f, "|v|=" + c.velocity.length());
        }

        // 4. fast projectile topples a tower without exploding
        {
            World w = worldWithGround();
            for (int i = 0; i < 5; i++) {
                w.add(new Body().setBox(1f, 1f, 1f).set(6f, 0.5f + i, 0f));
            }
            Body ball = w.add(new Body().setCircle(0.9f, 3f).set(0f, 2f, 0f));
            ball.velocity.set(28f, 4f);
            float maxSpeed = 0f;
            for (int i = 0; i < 120 * 4; i++) {
                w.step(1f / 120f);
                for (Body b : w.bodies) {
                    if (!b.isStatic) maxSpeed = Math.max(maxSpeed, b.velocity.length());
                }
            }
            check("no solver explosion", maxSpeed < 80f, "peak speed=" + maxSpeed);
            float sumX = 0;
            int n = 0;
            for (Body b : w.bodies) {
                if (!b.isStatic && b.shape == Body.BOX) { sumX += b.position.x; n++; }
            }
            check("tower actually toppled", sumX / n > 6.3f, "avg x=" + (sumX / n));
        }

        // 5. a tilted plank falls flat and rests on top of the ground (no sinking)
        {
            World w = worldWithGround();
            Body b = w.add(new Body().setBox(1f, 4f, 1f).set(0f, 2f, 0.35f));
            run(w, 4f);
            boolean flat = Math.abs(Math.abs(b.rotation) - Math.PI / 2f) < 0.25f;
            check("tilted plank falls flat and rests", flat && b.position.y > 0.45f
                            && b.position.y < 0.56f && !Float.isNaN(b.position.y),
                    "y=" + b.position.y + " rot=" + b.rotation);
        }

        // 6. friction stops a sliding box
        {
            World w = worldWithGround();
            Body b = w.add(new Body().setBox(1f, 1f, 1f).set(0f, 0.5f, 0f));
            b.friction = 0.7f;
            b.velocity.set(12f, 0f);
            run(w, 3f);
            check("friction stops sliding box", Math.abs(b.velocity.x) < 0.4f, "vx=" + b.velocity.x);
        }

        System.out.println(failures == 0 ? "\nALL PHYSICS TESTS PASSED" : "\n" + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
