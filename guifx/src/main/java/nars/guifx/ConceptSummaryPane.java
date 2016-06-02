package nars.guifx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import nars.concept.Concept;
import nars.guifx.util.ColorMatrix;
import nars.task.Task;
import nars.truth.Truth;

import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.runLater;


public class ConceptSummaryPane extends Text {


    public final Concept concept;
    //final Label subLabel = new Label();
    final AtomicBoolean pendingUpdate = new AtomicBoolean(false);
    private ConceptSummaryPaneIcon icon;
    private float lastPri = -1.0f;
    private final ColorMatrix truthColors = new ColorMatrix(11,11,(freq, conf)-> Color.hsb(360.0 * (freq * 0.3 + 0.25),
            conf, //all the way down to gray
            0.1 + 0.9f * conf));

    public ConceptSummaryPane(Concept c, long now) {
        super(c.term().toString());

        concept = c;

        //label.getStylesheets().clear();
        setTextAlignment(TextAlignment.LEFT);

        parentProperty().addListener(e -> {

            if (getParent()!=null) {
                if (icon==null) {
                    double iconWidth = 48.0f;
                    //setGraphic(icon = new ConceptSummaryPaneIcon());
                    //icon.size(iconWidth, iconWidth);
                }

                update(true,false,now);
            }
        });


    }

    public void update(boolean priority, boolean icon, long now) {

        if (this.icon == null)
            icon = false;

        if (icon) {
            this.icon.repaint();
        }

        /*float pri = concept.getPriority();
        if (Util.equal(lastPri, pri, 0.01) )
            priority = false;*/

        //HACK //TODO add truth value / color caching
        boolean truth = true;

        if (priority && truth && pendingUpdate.compareAndSet(false, true)) {

            Color color;
            if (concept.hasBeliefs()) {
                Truth tv = concept.beliefs().top(now).truth();
                color = truthColors.get(tv.freq(), tv.conf());
            }
            else {
                color = Color.GRAY;
            }

            runLater(() -> {
                pendingUpdate.set(false);

                float pri = 1f;
                lastPri = pri;
                setStyle(JFX.fontSize(((1.0f + pri) * 100.0f)));

                setFill(color);

//                StringBuilder sb = new StringBuilder();
//
//                if (obj.hasBeliefs()) {
//                    Task topBelief = obj.getBeliefs().top();
//                    topBelief.appendTo(sb, obj.getMemory(), false);
//                }
//                if (obj.hasGoals()) {
//                    Task topGoal = obj.getGoals().top();
//                    topGoal.appendTo(sb, obj.getMemory(), false);
//                }
//
//                subLabel.setText(sb.toString());

                //layout();
            });
        }
    }

    class ConceptSummaryPaneIcon extends SummaryIcon {
        public ConceptSummaryPaneIcon() {
            repaint();
        }




        @Override
        protected void repaint() {

            double m = 2;

            double W = getWidth();
            double H = getHeight();
            if (W*H == 0) return;

            GraphicsContext g = getGraphicsContext2D();

            /*Color c = NARfx.hashColor(concept.term().hashCode(),
                    1.0f, Plot2D.ca);*/
            g.setStroke(Color.GRAY);
            g.setLineWidth(m);
            g.strokeRect(m/2, m/2, W-m, H-m);

            double Hm = H - m * 2;
            double Wm = W - m * 2;
            concept.beliefs().forEach(t-> plot(m, Wm, Hm, g, t, false));
            concept.goals().forEach(t-> plot(m, Wm, Hm, g, t, true));

        }


    }

    static void plot(double m, double Wm, double hm, GraphicsContext g, Task t, boolean type) {

        ColorMatrix ca = type ? red: blue;


        float freq = t.freq();
        double y = (1.0f - freq) * Wm;
        float cnf = t.conf();
        double x = cnf * hm;

        Color color = ca.get(freq, cnf);

        //g.setFill(color);
        //g.fillRect(m + x-wh, m + y-wh, w, w);


        g.setStroke(color);
        double cy = m + y;
        double cx = m + x;
        double w = 12;
        if (type)
            g.strokeLine(cx-w/2, cy-w/2, cx+w/2, cy+w/2);
        else
            g.strokeLine(cx+w/2, cy-w/2, cx-w/2, cy+w/2);
    }

    static final ColorMatrix red  = new ColorMatrix(8,8,(x, y) -> Color.hsb(360 * (x * 0.25 + 0.25), 0.67, 0.5 + 0.5 * y));
    static final ColorMatrix blue = new ColorMatrix(8,8,(x, y) -> Color.hsb(360 * (x * 0.25 + 0.65), 0.67, 0.5 + 0.5 * y));

}
