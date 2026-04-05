package com.jvplatformer;

import com.jvplatformer.engine.Camera;
import com.jvplatformer.entity.Entity;
import com.jvplatformer.entity.Platform;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the cached-bounds contract: getBounds() always reflects current x/y,
 * and the same Rectangle object is returned each time (no allocation).
 */
class EntityBoundsTest {

    static class Box extends Entity {
        Box(double x, double y) { super(x, y, 32, 32); }
        @Override public void update(double dt, IInputHandler input) {}
        @Override public void render(Graphics2D g, Camera camera) {}
    }

    @Test
    void getBounds_reflectsCurrentPosition() {
        Box box = new Box(10, 20);
        Rectangle b = box.getBounds();
        assertEquals(10, b.x);
        assertEquals(20, b.y);
    }

    @Test
    void getBounds_updatesAfterMove() {
        Box box = new Box(0, 0);
        box.x = 100;
        box.y = 200;
        Rectangle b = box.getBounds();
        assertEquals(100, b.x, "bounds.x must track entity x after move");
        assertEquals(200, b.y, "bounds.y must track entity y after move");
    }

    @Test
    void getBounds_returnsSameObjectEachCall() {
        Box box = new Box(0, 0);
        Rectangle b1 = box.getBounds();
        Rectangle b2 = box.getBounds();
        assertSame(b1, b2, "getBounds() must return the cached instance, not a new Rectangle");
    }

    @Test
    void getBounds_dimensionsMatchConstructor() {
        Box box = new Box(0, 0);
        Rectangle b = box.getBounds();
        assertEquals(32, b.width);
        assertEquals(32, b.height);
    }

    @Test
    void platform_bounds_matchConstructorArgs() {
        Platform p = new Platform(50, 100, 200, 48);
        Rectangle b = p.getBounds();
        assertEquals(50,  b.x);
        assertEquals(100, b.y);
        assertEquals(200, b.width);
        assertEquals(48,  b.height);
    }

    @Test
    void twoEntities_doNotShareBoundsObject() {
        Box a = new Box(0, 0);
        Box b = new Box(0, 0);
        assertNotSame(a.getBounds(), b.getBounds(),
                "Each entity must have its own bounds instance");
    }
}
