package com.cvdnn.classify;

import android.Loople;
import android.edge.classify.onboard.ClassifyOnboard;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.Outline;
import android.os.Bundle;
import android.reflect.Clazz;
import android.util.ArrayMap;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import iot.proto.MultiMeaasgeInterface.UnitAttribute;

import static android.edge.classify.onboard.ClassifyOnboard.TEMPERATURE;
import static android.edge.classify.onboard.KegBox.LEFT;
import static android.edge.classify.onboard.KegBox.RIGHT;

/**
 * 控制板加载过程
 */
public abstract class OnBoardActivity extends UIActivity {

    private final SparseArray<KegBox> mBoxArray = new SparseArray<>();
    private final ArrayMap<String, UnitAttribute> mAttrArray = new ArrayMap<>();

    @Override
    protected void onInitData() {
        super.onInitData();

        mBoxArray.append(R.id.rdo_right, RIGHT);
        mBoxArray.append(R.id.rdo_left, LEFT);

        mAttrArray.put("temperature", TEMPERATURE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onBoardInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Outline.Hub.terminate();
    }

    /**
     * 初始化控制板
     */
    protected void onBoardInit() {
        makeLogcat(":: START INIT BOARD ::");
        Loople.Task.schedule(() -> {
            // 初始化板载设备
            Outline.Hub.attach(ClassifyOnboard.class).invoke();
            // 验证控制板是否挂载成功
            if (Outline.Hub.check()) {
                // 遍历打印控制板版本号
                Outline.Hub.boards().stream().forEach((b) -> makeLogcat("[onboard]: %s: %s", b.name(), b.version().toVersion()));

                runOnUiThread(() -> onCreateSpinnerView(binding.panelOperate.spDox, Outline.Hub.inodes()));
            } else {
                makeLogcat("[onboard]: error, %d", Outline.Hub.inodeCount());
            }
        });
    }

    protected final KegBox getKeyBox() {
        return mBoxArray.get(binding.panelOperate.rgBox.getCheckedRadioButtonId());
    }

    protected final UnitAttribute getSelectedAttribute(String text, KegBox box) {
        UnitAttribute attr = mAttrArray.get(text);
        if (attr == null) {
            attr = Clazz.getFieldValue(box, text);
        }

        return attr;
    }
}
