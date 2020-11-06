package com.cvdnn.classify;

import android.edge.classify.onboard.ClassifyOnboard;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.Outline;
import android.serialport.api.SerialInode;
import android.view.View;
import android.widget.Button;

import androidx.annotation.UiThread;

import iot.proto.DefiningDomain.StatusCodes;
import iot.proto.MultiMeaasgeInterface.UnitAttribute;

public class MainActivity extends OnDroppingActivity {

    @UiThread
    public void onControlClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        // 映射控制板实例
        ClassifyOnboard board = Outline.Hub.mapping(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();
        // 获取控制设备名称
        String label = getSelectedItemText(binding.panelOperate.spCtl);
        // 获取控制器单元属性，已知元件情况下无需使用反射
        UnitAttribute attr = getSelectedAttribute(label, box);

        // 控制器设置值
        StatusCodes code = StatusCodes.valueOf(((Button) view).getText().toString());
        board.set(attr, code);
    }

    @UiThread
    public void onSensorClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        // 映射控制板实例
        ClassifyOnboard board = Outline.Hub.mapping(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();
        // 获取传感器名称
        String label = getSelectedItemText(binding.panelOperate.spSen);
        // 获取传感器单元属性，已知元件情况下无需使用反射
        UnitAttribute attr = getSelectedAttribute(label, box);

        // 获取传感器数据
        Object obj = board.get(attr);
        if (obj != null) {
            makeLogcat("%s: %s", label, obj.toString());
        } else {
            makeLogcat("do get '%s' data error", label);
        }
    }

    @UiThread
    public void onClassifyClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        SerialInode inode = SerialInode.from(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();

        // 模拟垃圾投递过程
        onBoxSchemeHandle(inode, box);
    }

    @UiThread
    public void onTimingSwitchClicked(View view) {

    }
}
