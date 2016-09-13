package nars.experiment.rover;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.nar.Default;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.math.v3;
import spacegraph.obj.Maze;
import spacegraph.phys.Collisions;
import spacegraph.phys.Dynamics;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CylinderShape;
import spacegraph.render.Draw;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {

    public static class Retina extends Collisions.RayResultCallback {
        public v3 localPosition, worldPosition;
        public v3 localDirection, worldTarget, worldHit = v();
        float r, g, b;
        public float rangeMax;
        private SimpleSpatial parent;

        public void update(Dynamics d, SimpleSpatial parent) {
            this.parent = parent;
            Transform x = parent.transform();

            worldPosition = x.transform(v(localPosition));

            worldTarget = v(localDirection);
            worldTarget.scale(rangeMax); //TODO limit by contact point
            worldTarget.add(localPosition);
            x.transform(worldTarget);


            r = g = b = 0;
            worldHit.set(worldTarget);

            d.rayTest(worldPosition, worldTarget, this);
        }

        public void render(GL2 gl) {

            gl.glColor3f(r, g, b);
            gl.glLineWidth(2f);
            Draw.line(gl, worldPosition, worldTarget);
        }

        @Override
        public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
            Object target = rayResult.collidable.data();
            if (target != parent) {
                float dist = v3.dist(worldPosition, rayResult.hitNormal);
                //System.out.println(rayResult.collidable.data() + " " + dist);
                worldHit.set(rayResult.hitNormal);
                if (target instanceof SimpleSpatial) {
                    SimpleSpatial ss = ((SimpleSpatial) target);
                    r = ss.shapeColor[0];
                    g = ss.shapeColor[1];
                    b = ss.shapeColor[2];
                }
            }
            return 0;
        }
    }

    public static class Rover extends SimpleSpatial {

        private final NAR nar;

        final List<Retina> retinas = $.newArrayList();

        public Rover(NAR nar) {
            super(nar);
            this.nar = nar;

            shapeColor[0] = 1f;
            shapeColor[1] = 0.1f;
            shapeColor[2] = 0.5f;
            shapeColor[3] = 1f;

            for (int i = 0; i < 32; i++) {
                Retina r = new Retina();

                r.localPosition = v();

                r.localDirection = v((float)Math.random()-0.5f, (float)Math.random()-0.5f, (float)Math.random()-0.5f)
                        .normalized();

                r.rangeMax = 8;

                retinas.add(r);
            }
        }

        @Override
        public void update(Dynamics world) {
            for (Retina r : retinas)
                r.update(world, this);

            super.update(world);
        }

        @Override
        protected void renderAbsolute(GL2 gl) {
            for (Retina r : retinas)
                r.render(gl);

            super.renderAbsolute(gl);
        }

        @Override
        protected CollisionShape newShape() {
            //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
            return new CylinderShape(v(1, 1, 1));
        }
    }

    public static void main(String[] args) {
        new SpaceGraph<>(
                new Maze("x", 20, 20),
                new Rover(new Default())
        ).setGravity(v(0, 0, -5)).show(1000, 1000);
    }

}
