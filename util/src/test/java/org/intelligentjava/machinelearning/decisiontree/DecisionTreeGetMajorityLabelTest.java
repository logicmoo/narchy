package org.intelligentjava.machinelearning.decisiontree;

import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;

import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DecisionTreeGetMajorityLabelTest {
    
    // TODO fix handling of empty lists
//    @Test
//    public void testGetLabelOnEmptyList() {
//        DecisionTree tree = new DecisionTree();
//        List<DataSample> data = Lists.newArrayList();
//        Assert.assertNull(tree.getMajorityLabel(data));
//    }
    
    @Test
    public void testGetMajorityLabel() {
        DecisionTree<String, Object> tree = new DecisionTree();
        List<Function<String,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        Assert.assertEquals("false", DecisionTree.majority(null, data).toString());
    }

    @Test
    public void testGetMajorityLabelWhenEqualCounts() {
        DecisionTree<String, Object> tree = new DecisionTree();
        List<Function<String,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        Assert.assertEquals("false", DecisionTree.majority(null, data).toString());
    }
}