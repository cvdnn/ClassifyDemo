package com.cvdnn.classify.model;

import android.edge.classify.onboard.ClassifyOnboard;

import iot.proto.MultiMeaasgeInterface.*;

public class WeightEntity {
    public ClassifyOnboard board;
    public UnitAttribute attr;
    public long lastWeight;
    public long endWeight;

    public WeightEntity(ClassifyOnboard board, UnitAttribute attr, long weight) {
        this.board = board;
        this.attr = attr;
        this.lastWeight = weight;
    }
}
