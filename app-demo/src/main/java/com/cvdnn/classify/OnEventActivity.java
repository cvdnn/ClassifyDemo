package com.cvdnn.classify;

import android.edge.classify.event.Events;
import android.edge.classify.event.KeyEvent;
import android.edge.classify.event.OnSerialEventMonitor;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.Outline;
import android.edge.scan.Comps;
import android.edge.scan.OnScanListener;
import android.edge.scan.Scanister;
import android.os.Bundle;
import android.serialport.api.SerialInode;

import androidx.annotation.Nullable;

import iot.proto.DefiningDomain;
import iot.proto.M2spLite;
import iot.proto.MultiMeaasgeInterface;
import iot.proto.serical.SerialEvent;

import static android.edge.classify.onboard.KegBox.LEFT;
import static android.edge.classify.onboard.KegBox.RIGHT;

/**
 * 控制板事件订阅
 */
public abstract class OnEventActivity extends OnBoardActivity implements KeyEvent.OnKeyListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 监听HID设备
        Scanister.bind(this, mScanListener);
    }

    @Override
    protected void onBoardInit() {
        // 监听控制板连接情况
        Events.Connect.set(mSerialConnectListener);

        super.onBoardInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Events.Serial.unsubscribe(mPlateSerialEventSubscriber);
        Events.Key.remove(this);

        Outline.Hub.terminate();
    }

    /**
     * 控制板链接监听
     */
    protected final SerialEvent.OnSerialConnectListener mSerialConnectListener = new SerialEvent.OnSerialConnectListener() {

        @Override
        public void onConnected(SerialInode inode) {
            // 注册串口事件监听
            Events.Serial.subscribe(mPlateSerialEventSubscriber);

            // 注册按钮事件监听
            Events.Key.add(OnEventActivity.this);
        }

        @Override
        public void onDisconnected(SerialInode inode, int code, String text) {
            makeLogcat("Disconnected: %s: %d: %s", SerialInode.name(inode), code, text);
        }
    };

    /**
     * 监听门限位器状态
     */
    protected final OnSerialEventMonitor mPlateSerialEventSubscriber = new OnSerialEventMonitor(LEFT.plate, RIGHT.plate) {

        @Override
        public void onEvent(SerialInode inode, KegBox box, MultiMeaasgeInterface.UnitAttribute attr, MultiMeaasgeInterface.UnitMeta meta) {
            String inodeName = SerialInode.name(inode);
            DefiningDomain.StatusCodes code = M2spLite.valueOf(meta);

            makeLogcat("PLATE: %s: %s: %s", inodeName, box.name(), code.name());
        }
    };

    /**
     * 扫描监听
     * <p>
     * Comps.CARD: IC卡
     * Comps.XCODE：一维码，二维码
     * Comps.RECON：人脸识别
     */
    protected final OnScanListener mScanListener = new OnScanListener() {

        @Override
        public void onScanned(Comps comps, String text) {
            makeLogcat("[%s]: %s", comps.name(), text);
        }

        @Override
        public void onError(int vid, int pid) {
            makeLogcat("不支持该型号配件：VID：%d，PID：%d\n请核对扫描器厂家和产品型号", vid, pid);
        }
    };
}
