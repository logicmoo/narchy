package nars.link;

import nars.budget.Budget;

/**
 * Created by me on 5/29/16.
 */
public interface BLink<X> extends Budget, Link<X> {


    @Override X get();

    @Override
    default boolean isDeleted() {
        float p = pri(); //b[PRI];
        return (p!=p); //fast NaN test
    }



    //    @Override public final boolean equals(Object obj) {
////        /*if (obj instanceof Budget)*/ {
////            return equalsBudget((Budget) obj);
////        }
////        return id.equals(((BagBudget)obj).id);
//        boolean result;
//        @Nullable X x = get();
//        if (obj instanceof BLink) {
//            Object o = ((BLink) obj).get();
//            result = Objects.equal(x, o);
//        } else {
//            result = Objects.equal(x, obj);
//        }
//        return obj == this || result;
//    }
//
//
//    @Override public final int hashCode() {
//        Object x = get();
//        return x == null ? 0 : x.hashCode();
//    }



}
