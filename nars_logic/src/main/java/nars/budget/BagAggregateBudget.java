package nars.budget;

import nars.data.BudgetedStruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/12/15.
 */
public class BagAggregateBudget extends Budget {

    private final Iterable<? extends BudgetedStruct> budgets;
    private long time;
    private float pri = 0;
    private float dur = 0;
    private float qua = 0;

    public BagAggregateBudget(Iterable<? extends BudgetedStruct> budgets) {
        this.budgets = budgets;
    }

    @Nullable
    @Override
    public Budget zero() {
        return null;
    }

    @Override
    public void delete() {

    }

    @Override
    public float pri() {
        return pri;
    }

    @Override
    public void setPriority(float p) {
        //ignore
    }

    @Override
    public long setLastForgetTime(long currentTime) {
        long prevTime = this.time;
        long delta = currentTime - prevTime;
        if (delta > 0) {
            refresh();
        }
        return this.time = currentTime;
    }

    @NotNull
    @Override
    public String toString() {
        return getBudgetString();
    }
    private void refresh() {
        refreshMax();
    }

    private void refreshMax() {
        float totalPri = 0, totalDur = 0, totalQua = 0;
        for (BudgetedStruct b : budgets) {
            totalPri = Math.max(b.pri(), totalPri);
            totalDur = Math.max(b.dur(), totalDur);
            totalQua = Math.max(b.qua(), totalQua);
        }

        //System.out.println(getLastForgetTime() + " " + this);

        this.pri = totalPri;
        this.dur = totalDur;
        this.qua = totalQua;

    }
    private void refreshAvg() {
        float totalPri = 0, totalDur = 0, totalQua = 0;
        int n = 0;
        for (BudgetedStruct b : budgets) {
            totalPri += b.pri();
            totalDur += b.dur();
            totalQua += b.qua();
            n++;
        }

        if (n > 0) {
            totalPri /= n;
            totalDur /= n;
            totalQua /= n;
        }

        //System.out.println(getLastForgetTime() + " " + this);

        this.pri = totalPri;
        this.dur = totalDur;
        this.qua = totalQua;

    }

    @Override
    public long lastForgetTime() {
        return time;
    }

    @Override
    public void mulPriority(float factor) {

    }

    @Override
    public float dur() {
        return dur;
    }

    @Override
    public void setDurability(float d) {
        //ignore
    }

    @Override
    public float qua() {
        return qua;
    }

    @Override
    public void setQuality(float q) {
        //ignore
    }

    @NotNull
    @Override
    public Budget clone() {
        return this;
    }
}
