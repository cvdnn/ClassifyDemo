package com.cvdnn.classify;

import android.Args;
import android.edge.classify.onboard.ClassifyOnboard;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.Outline;
import android.edge.classify.onboard.event.OnSerialEventMonitor;
import android.edge.scan.Comps;
import android.edge.scan.OnScanListener;
import android.edge.scan.Scanister;
import android.frame.context.FrameActivity;
import android.os.Bundle;
import android.reflect.Clazz;
import android.serialport.api.SerialInode;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.cvdnn.classify.databinding.ActMainBinding;
import com.ztone.Loople;

import java.util.Arrays;

import iot.proto.DefiningDomain.StatusCodes;
import iot.proto.DefiningDomain.UnitIndex;
import iot.proto.M2spLite;
import iot.proto.MultiMeaasgeInterface.UnitAttribute;
import iot.proto.MultiMeaasgeInterface.UnitMeta;
import iot.proto.serical.SerialEvent;

import static android.edge.classify.onboard.ClassifyOnboard.TEMPERATURE;
import static android.edge.classify.onboard.KegBox.LEFT;
import static android.edge.classify.onboard.KegBox.RIGHT;
import static iot.proto.DefiningDomain.StatusCodes.DOWN;
import static iot.proto.DefiningDomain.StatusCodes.UP;

public class MainActivity extends FrameActivity<ActMainBinding> {

    private final SparseArray<KegBox> mBoxArray = new SparseArray<>();
    private final ArrayMap<String, UnitAttribute> mAttrArray = new ArrayMap<>();

    @Override
    protected void onInitData() {
        super.onInitData();

        mBoxArray.append(R.id.rdo_right, RIGHT);
        mBoxArray.append(R.id.rdo_left, LEFT);

        mAttrArray.put("temperature", TEMPERATURE);

        onBoardInit();
    }

    @Override
    protected ActMainBinding onViewBinding() {
        return ActMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreateView() {
        super.onCreateView();

        binding.panelOperate.focusView.requestFocus();

        onCreateSpinnerView(binding.panelOperate.spCtl, Args.Env.Res.getStringArray(R.array.control_array));
        onCreateSpinnerView(binding.panelOperate.spSen, Args.Env.Res.getStringArray(R.array.sensor_array));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 监听HID设备
        Scanister.bind(this, mScanListener);

        SerialEvent.Motion
                .subscribe(mLeftKeySerialEventSubscriber)
                .subscribe(mRightKeySerialEventSubscriber);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SerialEvent.Motion
                .unsubscribe(mLeftKeySerialEventSubscriber)
                .unsubscribe(mRightKeySerialEventSubscriber);

        Outline.Hub.terminate();
    }

    @WorkerThread
    private void onBoardInit() {
        Loople.Task.schedule(() -> {
            // 初始化板载设备
            Outline.Hub.attach(ClassifyOnboard.class).invoke();
            // 验证控制板是否挂载成功
            if (Outline.Hub.check()) {
                // 遍历打印控制板版本号
                Outline.Hub.boards().stream().forEach((b) -> makeLogcat("[onboard]: %s: %s", b.name(), b.version().toVersion()));

                runOnUiThread(() -> {
                    onCreateSpinnerView(binding.panelOperate.spDox, Outline.Hub.inodes());
                });
            } else {
                makeLogcat("[onboard]: error, %d", Outline.Hub.inodeCount());
            }
        });
    }

    @UiThread
    public void onOnOffClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        // 映射控制板实例
        ClassifyOnboard onboard = Outline.Hub.mapping(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();
        // 获取控制设备名称
        String label = getSelectedItemText(binding.panelOperate.spCtl);
        // 获取控制器单元属性，已知元件情况下无需使用反射
        UnitAttribute attr = getSelectedAttribute(label, box);

        // 控制器设置值
        StatusCodes code = StatusCodes.valueOf(((Button) view).getText().toString());
        onboard.set(attr, code);
    }

    @UiThread
    public void onSensorClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        // 映射控制板实例
        ClassifyOnboard onboard = Outline.Hub.mapping(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();
        // 获取传感器名称
        String label = getSelectedItemText(binding.panelOperate.spSen);
        // 获取传感器单元属性，已知元件情况下无需使用反射
        UnitAttribute attr = getSelectedAttribute(label, box);

        // 获取传感器数据
        Object obj = onboard.get(attr);
        if (obj != null) {
            makeLogcat("%s: %s", label, obj.toString());
        } else {
            makeLogcat("do get '%s' data error", label);
        }
    }

    @UiThread
    public void onTimingSwitchClicked(View view) {

    }

    @WorkerThread
    protected final void onKeyDown(SerialInode inode, UnitMeta meta) {
        makeLogcat("%s: %s: %s: onKeyDown", SerialInode.name(inode), getKeyBox().name(), M2spLite.index(meta).name());
    }

    @WorkerThread
    protected final void onKeyUp(SerialInode inode, UnitMeta meta) {
        makeLogcat("%s: %s: %s: onKeyUp", SerialInode.name(inode), getKeyBox().name(), M2spLite.index(meta).name());
    }

    /**
     * 扫描监听
     */
    private final OnScanListener mScanListener = new OnScanListener() {

        @Override
        public void onScanned(Comps comps, String text) {
            // Comps.CARD: IC卡
            // Comps.XCODE：一维码，二维码
            // Comps.RECON：人脸识别
            makeLogcat("[%s]: %s", comps.name(), text);
        }

        @Override
        public void onError(int vid, int pid) {
            makeLogcat("不支持该型号配件：VID：%d，PID：%d\n请核对扫描器厂家和产品型号", vid, pid);
        }
    };

    private final SerialEvent.Subscriber mLeftKeySerialEventSubscriber = new OnSerialEventMonitor(LEFT.key) {

        @Override
        public void onEvent(SerialInode inode, KegBox box, UnitAttribute attr, UnitMeta meta) {
            StatusCodes status = M2spLite.valueOf(meta);
            if (status == DOWN) {
                onKeyDown(inode, meta);

            } else if (status == UP) {
                onKeyUp(inode, meta);
            }
        }
    };

    private final OnSerialEventMonitor mRightKeySerialEventSubscriber = new OnSerialEventMonitor(RIGHT.key) {

        @Override
        public void onEvent(SerialInode inode, KegBox box, UnitAttribute attr, UnitMeta meta) {
            String inodeName = SerialInode.name(inode);
            UnitIndex index = attr.getIndex();
            StatusCodes code = M2spLite.valueOf(meta);

            makeLogcat("%s: %s: %s: %s", inodeName, getKeyBox().name(), index.name(), code.name());
        }
    };

    private KegBox getKeyBox() {
        return mBoxArray.get(binding.panelOperate.rgBox.getCheckedRadioButtonId());
    }

    private UnitAttribute getSelectedAttribute(String text, KegBox box) {
        UnitAttribute attr = mAttrArray.get(text);
        if (attr == null) {
            attr = Clazz.getFieldValue(box, text);
        }

        return attr;
    }

    private String getSelectedItemText(@NonNull Spinner sp) {
        String text = "";
        Object item = sp.getSelectedItem();
        if (item instanceof String) {
            text = (String) item;
        }

        return text;
    }

    private void onCreateSpinnerView(Spinner sp, String[] itemArrays) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(itemArrays));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    public synchronized void makeLogcat(String format, Object... args) {
        runOnUiThread(() -> binding.panelLogcat.tvLogcatText.setText(new StringBuilder()
                .append(String.format(format, args))
                .append("\n")
                .append(binding.panelLogcat.tvLogcatText.getText())
                .toString()));
    }
}
