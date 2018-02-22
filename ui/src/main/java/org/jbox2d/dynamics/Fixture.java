/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.jbox2d.dynamics;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.RayCastInput;
import org.jbox2d.collision.RayCastOutput;
import org.jbox2d.collision.broadphase.BroadPhase;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.fracture.Material;
import org.jbox2d.fracture.PolygonFixture;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

/**
 * A fixture is used to attach a shape to a body for collision detection. A fixture inherits its
 * transform from its parent. Fixtures hold additional non-geometric data such as friction,
 * collision filters, etc. Fixtures are created via Body::CreateFixture.
 *
 * @author daniel
 * @warning you cannot reuse fixtures.
 */
public class Fixture {

    public float density;

    public Fixture next;
    public Body2D body;

    public Shape shape;

    public float friction;
    public float restitution;

    public FixtureProxy[] proxies;
    public int m_proxyCount;

    public final Filter filter;

    public boolean isSensor;

    public Object data;

    public Material material;

    public PolygonFixture polygon;

    public Fixture() {
        data = null;
        body = null;
        next = null;
        proxies = null;
        m_proxyCount = 0;
        shape = null;
        filter = new Filter();
        material = null;
        polygon = null;
    }

    /**
     * Get the type of the child shape. You can use this to down cast to the concrete shape.
     *
     * @return the shape type.
     */
    public ShapeType type() {
        return shape.getType();
    }

    /**
     * Get the child shape. You can modify the child shape, however you should not change the number
     * of vertices because this will crash some collision caching mechanisms.
     *
     * @return
     */
    public Shape shape() {
        return shape;
    }

    /**
     * Is this fixture a sensor (non-solid)?
     *
     * @return
     */
    public boolean isSensor() {
        return isSensor;
    }

    /**
     * Set if this fixture is a sensor.
     *
     * @param sensor
     */
    public void setSensor(boolean sensor) {
        if (sensor != isSensor) {
            body.setAwake(true);
            isSensor = sensor;
        }
    }

    /**
     * Set the contact filtering data. This is an expensive operation and should not be called
     * frequently. This will not update contacts until the next time step when either parent body is
     * awake. This automatically calls refilter.
     *
     * @param filter
     */
    public void setFilterData(final Filter filter) {
        this.filter.set(filter);

        refilter();
    }

    /**
     * Get the contact filtering data.
     *
     * @return
     */
    public Filter getFilterData() {
        return filter;
    }

    /**
     * Call this if you want to establish collision that was previously disabled by
     * ContactFilter::ShouldCollide.
     */
    public void refilter() {
        if (body == null) {
            return;
        }

        // Flag associated contacts for filtering.
        ContactEdge edge = body.contacts();
        while (edge != null) {
            Contact contact = edge.contact;
            Fixture fixtureA = contact.aFixture;
            Fixture fixtureB = contact.bFixture;
            if (fixtureA == this || fixtureB == this) {
                contact.flagForFiltering();
            }
            edge = edge.next;
        }

        Dynamics2D world = body.getWorld();

        if (world == null) {
            return;
        }

        // Touch each proxy so that new pairs may be created
        BroadPhase broadPhase = world.contactManager.broadPhase;
        for (int i = 0; i < m_proxyCount; ++i) {
            broadPhase.touchProxy(proxies[i].id);
        }
    }

    /**
     * Get the parent body of this fixture. This is NULL if the fixture is not attached.
     *
     * @return
     */
    public Body2D getBody() {
        return body;
    }

    /**
     * Get the next fixture in the parent body's fixture list.
     *
     * @return
     */
    public Fixture getNext() {
        return next;
    }

    public void setDensity(float density) {
        assert (density >= 0f);
        this.density = density;
    }

    public float getDensity() {
        return density;
    }

    /**
     * Get the user data that was assigned in the fixture definition. Use this to store your
     * application specific data.
     *
     * @return
     */
    public Object getUserData() {
        return data;
    }

    /**
     * Set the user data. Use this to store your application specific data.
     *
     * @param data
     */
    public void setUserData(Object data) {
        this.data = data;
    }

    /**
     * Test a point for containment in this fixture. This only works for convex shapes.
     *
     * @param p a point in world coordinates.
     * @return
     */
    public boolean testPoint(final Tuple2f p) {
        return shape.testPoint(body, p);
    }

    /**
     * Cast a ray against this shape.
     *
     * @param output the ray-cast results.
     * @param input  the ray-cast input parameters.
     * @param output
     * @param input
     */
    public boolean raycast(RayCastOutput output, RayCastInput input, int childIndex) {
        return shape.raycast(output, input, body, childIndex);
    }

    /**
     * Get the mass data for this fixture. The mass data is based on the density and the shape. The
     * rotational inertia is about the shape's origin.
     *
     * @return
     */
    public void getMassData(MassData massData) {
        shape.computeMass(massData, density);
    }

    /**
     * Get the coefficient of friction.
     *
     * @return
     */
    public float getFriction() {
        return friction;
    }

    /**
     * Set the coefficient of friction. This will _not_ change the friction of existing contacts.
     *
     * @param friction
     */
    public void setFriction(float friction) {
        this.friction = friction;
    }

    /**
     * Get the coefficient of restitution.
     *
     * @return
     */
    public float getRestitution() {
        return restitution;
    }

    /**
     * Set the coefficient of restitution. This will _not_ change the restitution of existing
     * contacts.
     *
     * @param restitution
     */
    public void setRestitution(float restitution) {
        this.restitution = restitution;
    }

    /**
     * Get the fixture's AABB. This AABB may be enlarge and/or stale. If you need a more accurate
     * AABB, compute it using the shape and the body transform.
     *
     * @return
     */
    public AABB getAABB(int childIndex) {
        assert (childIndex >= 0 && childIndex < m_proxyCount);
        return proxies[childIndex].aabb;
    }

    /**
     * Compute the distance from this fixture.
     *
     * @param p a point in world coordinates.
     * @return distance
     */
    public float computeDistance(Tuple2f p, int childIndex, v2 normalOut) {
        return shape.computeDistanceToOut(body, p, childIndex, normalOut);
    }

    // We need separation create/destroy functions from the constructor/destructor because
    // the destructor cannot access the allocator (no destructor arguments allowed by C++).

    public void create(Body2D body, FixtureDef def) {
        data = def.userData;
        friction = def.friction;
        restitution = def.restitution;

        this.body = body;
        next = null;


        filter.set(def.filter);

        isSensor = def.isSensor;


        //moj doplneny kod
        material = def.material;
        polygon = def.polygon;
        density = def.density;

        setShape(def.shape.clone());
    }

    public void setShape(Shape shape) {
        this.shape = shape;

        // Reserve proxy space
        int childCount = this.shape.getChildCount();
        if (proxies == null || proxies.length!=childCount) {
            proxies = new FixtureProxy[childCount];
            for (int i = 0; i < childCount; i++) {
                proxies[i] = new FixtureProxy();
                proxies[i].fixture = null;
                proxies[i].id = BroadPhase.NULL_PROXY;
            }
        }

        if (proxies.length < childCount) {
            FixtureProxy[] old = proxies;
            int newLen = MathUtils.max(old.length * 2, childCount);
            proxies = new FixtureProxy[newLen];
            System.arraycopy(old, 0, proxies, 0, old.length);
            for (int i = 0; i < newLen; i++) {
                if (i >= old.length) {
                    proxies[i] = new FixtureProxy();
                }
                proxies[i].fixture = null;
                proxies[i].id = BroadPhase.NULL_PROXY;
            }
        }
        m_proxyCount = 0;
    }

    public void destroy() {
        // The proxies must be destroyed before calling this.
        assert (m_proxyCount == 0);

        // Free the child shape.
        shape = null;
        proxies = null;
        next = null;

        // TODO pool shapes
        // TODO pool fixtures
    }

    // These support body activation/deactivation.
    public void createProxies(BroadPhase broadPhase, final Transform xf) {
        assert (m_proxyCount == 0);

        // Create proxies in the broad-phase.
        m_proxyCount = shape.getChildCount();

        for (int i = 0; i < m_proxyCount; ++i) {
            FixtureProxy proxy = proxies[i];
            shape.computeAABB(proxy.aabb, xf, i);
            proxy.id = broadPhase.createProxy(proxy.aabb, proxy);
            proxy.fixture = this;
            proxy.childIndex = i;
        }
    }

    /**
     * Internal method
     *
     * @param broadPhase
     */
    public void destroyProxies(BroadPhase broadPhase) {
        // Destroy proxies in the broad-phase.
        for (int i = 0; i < m_proxyCount; ++i) {
            FixtureProxy proxy = proxies[i];
            broadPhase.destroyProxy(proxy.id);
            proxy.id = BroadPhase.NULL_PROXY;
        }

        m_proxyCount = 0;
    }

    private final AABB pool1 = new AABB();
    private final AABB pool2 = new AABB();
    private final Tuple2f displacement = new Vec2();

    /**
     * Internal method
     *
     * @param broadPhase
     * @param xf1
     * @param xf2
     */
    protected void synchronize(BroadPhase broadPhase, final Transform transform1,
                               final Transform transform2) {
        if (m_proxyCount == 0) {
            return;
        }

        for (int i = 0; i < m_proxyCount; ++i) {
            FixtureProxy proxy = proxies[i];

            // Compute an AABB that covers the swept shape (may miss some rotation effect).
            final AABB aabb1 = pool1;
            final AABB aab = pool2;
            shape.computeAABB(aabb1, transform1, proxy.childIndex);
            shape.computeAABB(aab, transform2, proxy.childIndex);

            proxy.aabb.lowerBound.x =
                    aabb1.lowerBound.x < aab.lowerBound.x ? aabb1.lowerBound.x : aab.lowerBound.x;
            proxy.aabb.lowerBound.y =
                    aabb1.lowerBound.y < aab.lowerBound.y ? aabb1.lowerBound.y : aab.lowerBound.y;
            proxy.aabb.upperBound.x =
                    aabb1.upperBound.x > aab.upperBound.x ? aabb1.upperBound.x : aab.upperBound.x;
            proxy.aabb.upperBound.y =
                    aabb1.upperBound.y > aab.upperBound.y ? aabb1.upperBound.y : aab.upperBound.y;
            displacement.x = transform2.pos.x - transform1.pos.x;
            displacement.y = transform2.pos.y - transform1.pos.y;

            broadPhase.moveProxy(proxy.id, proxy.aabb, displacement);
        }
    }
}