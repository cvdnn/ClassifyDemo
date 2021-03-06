package com.cvdnn.classify;

import android.Args;
import android.Loople;
import android.app.AlertDialog;
import android.app.Dialog;
import android.assist.Assert;
import android.edge.classify.Timing;
import android.edge.classify.onboard.ClassifyOnboard;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.Outline;
import android.edge.classify.version.Semver;
import android.io.StreamUtils;
import android.serialport.api.SerialInode;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.cvdnn.classify.databinding.DlgTimingBinding;
import com.cvdnn.classify.model.WeightEntity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;

import iot.proto.DefiningDomain.StatusCodes;
import iot.proto.MultiMeaasgeInterface.UnitAttribute;

import static android.edge.classify.Timing.SUMMER_TIMING_LIGHT;
import static android.edge.classify.Timing.TAG_TIMING_POWER_OFF;
import static android.edge.classify.Timing.TAG_TIMING_POWER_ON;
import static android.edge.classify.Timing.TAG_TIMING_SUMMER_LIGHT;
import static android.edge.classify.Timing.TAG_TIMING_WINTER_LIGHT;
import static android.edge.classify.Timing.TIMING_POWER_OFF;
import static android.edge.classify.Timing.TIMING_POWER_ON;
import static android.edge.classify.Timing.WINTER_TIMING_LIGHT;
import static android.edge.classify.onboard.ClassifyOnboard.VALUE_BLUR;
import static android.edge.classify.onboard.ClassifyOnboard.WEIGHING_LEFT;
import static android.edge.classify.onboard.ClassifyOnboard.WEIGHING_RIGHT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends OnDroppingActivity {
    private static final File FILE_BIN_TEMP = new File(Args.Env.Paths.temp, "bin_temp.bin");

    private static final int DELAY_SHUTDOWN_MILLIS = 3000;

    private Dialog mTimeSwitchDialog;

    private final ArrayList<WeightEntity> mLastWeightArray = new ArrayList<>();

    @UiThread
    public final void onControlClicked(View view) {
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
    public final void onSensorClicked(View view) {
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
    public final void onClassifyClicked(View view) {
        // 获取选择的串口名称
        String ttys = getSelectedItemText(binding.panelOperate.spDox);
        SerialInode inode = SerialInode.from(ttys);

        // 获取选择的垃圾桶边
        KegBox box = getKeyBox();

        // 模拟垃圾投递过程
        Loople.Task.schedule(() -> onBoxSchemeHandle(inode, box));
    }

    @UiThread
    public final void onTimingSwitchClicked(View view) {
        if (mTimeSwitchDialog != null) {
            mTimeSwitchDialog.dismiss();
            mTimeSwitchDialog = null;
        }

        DlgTimingBinding dltBing = DlgTimingBinding.inflate(getLayoutInflater());

        dltBing.timingStart.setText(Args.Env.Cfg.get(TAG_TIMING_POWER_ON, TIMING_POWER_ON));
        dltBing.timingEnd.setText(Args.Env.Cfg.get(TAG_TIMING_POWER_OFF, TIMING_POWER_OFF));

        dltBing.summerLight.setText(Args.Env.Cfg.get(TAG_TIMING_SUMMER_LIGHT, SUMMER_TIMING_LIGHT));
        dltBing.winterLight.setText(Args.Env.Cfg.get(TAG_TIMING_WINTER_LIGHT, WINTER_TIMING_LIGHT));

        dltBing.rgPower.setOnCheckedChangeListener((RadioGroup group, int checkedId) -> {
            dltBing.settingPanel.setVisibility(checkedId == R.id.rdo_set_power ? VISIBLE : GONE);
        });

        mTimeSwitchDialog = showTimingDialog(dltBing);
    }

    @UiThread
    public final void onOTAClicked(View v) {
        Loople.Task.schedule(() -> {
            // 获取选择的串口名称
            String ttys = getSelectedItemText(binding.panelOperate.spDox);
            // 映射控制板实例
            ClassifyOnboard board = Outline.Hub.mapping(ttys);

            Semver boardSemver = board.version();
            Semver updateSemver = new Semver().parse(FILE_BIN_TEMP);

            // FIXME 必须对固件版本进行类型和版本号比对，避免刷错固件造成设备无法运行
            if (Semver.valid(boardSemver, updateSemver) && updateSemver.newness(boardSemver)) {
                InputStream binInput = null;

                try {
                    binInput = new BufferedInputStream(new FileInputStream(FILE_BIN_TEMP));
                    board.pushRom(updateSemver.code, binInput, false,
                            (total, progress) -> makeLogcat("【%s】控制板固件正在烧录，安装进度：%.1f%%", ttys, (float) progress / total * 100));
                } catch (Exception e) {
                    makeLogcat("ERROR: %s", e.getMessage());

                } finally {
                    StreamUtils.close(binInput);
                }
            } else {
                makeLogcat("固件版本已是最新版本");
            }
        });
    }

    @UiThread
    private final void onPrepareWeight(View v) {
        Loople.Task.schedule(() -> {
            Vector<Runnable> runnables = new Vector<>();
            Outline.Hub.boards().forEach(board -> {
                ClassifyOnboard cfy = ((ClassifyOnboard) board);

                runnables.add(() -> {
                    long w = cfy.getWeighing(WEIGHING_LEFT);
                    if (w != VALUE_BLUR) {
                        mLastWeightArray.add(new WeightEntity(cfy, WEIGHING_LEFT, w));
                    }
                });

                runnables.add(() -> {
                    long w = cfy.getWeighing(WEIGHING_RIGHT);
                    if (w != VALUE_BLUR) {
                        mLastWeightArray.add(new WeightEntity(cfy, WEIGHING_RIGHT, w));
                    }
                });
            });

            Loople.Task.allOf(runnables);

            makeLogcat("开始称重试验，请投递标称物");
        });
    }

    @UiThread
    private final void onInvokeWeight(View v) {
        Loople.Task.schedule(() -> {
            Vector<Runnable> runnables = new Vector<>();
            mLastWeightArray.forEach(entity -> runnables.add(() -> entity.endWeight = entity.board.getWeighing(entity.attr)));
            Loople.Task.allOf(runnables);

            StringBuilder text = new StringBuilder();
            mLastWeightArray.forEach(entity -> {
                long weighting = entity.endWeight - entity.lastWeight;
                if (entity.endWeight != VALUE_BLUR && weighting > 0) {
                    text.append(String.format(Locale.getDefault(), "[%s]: %d克\n", entity.board.tag(entity.attr), weighting));
                }
            });

            String hintText = text.toString();
            if (Assert.notEmpty(hintText)) {
                makeLogcat(hintText);
            } else {
                makeLogcat("本次尚未投递标称物");
            }

            mLastWeightArray.clear();
        });
    }

    private final AlertDialog showTimingDialog(@NonNull DlgTimingBinding dltBing) {
        return new AlertDialog.Builder(this)
                .setTitle("定时设置")
                .setView(dltBing.getRoot())
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("确定", (dialog, which) -> {
                    String powerOn = dltBing.timingStart.getText().toString(), powerOff = dltBing.timingEnd.getText().toString();
                    String summerLight = dltBing.summerLight.getText().toString(), winterLight = dltBing.winterLight.getText().toString();
                    if (Assert.notEmpty(powerOn) && Assert.notEmpty(powerOff) && Assert.notEmpty(summerLight) && Assert.notEmpty(winterLight)) {
                        Args.Env.Cfg.edit()
                                .put(TAG_TIMING_POWER_ON, powerOn)
                                .put(TAG_TIMING_POWER_OFF, powerOff)
                                .put(TAG_TIMING_SUMMER_LIGHT, summerLight)
                                .put(TAG_TIMING_WINTER_LIGHT, winterLight)
                                .apply();

                        boolean rightNow = dltBing.ckbRightNow.isChecked();
                        boolean powerClear = dltBing.rdoPowerClear.isChecked();
                        if (powerClear) {
                            Outline.power().clearPowerTime();
                            makeLogcat("定时开关机清理完成");

                        } else {
                            spreadTiming(powerOn, powerOff, rightNow);
                        }

                    } else {
                        makeToast("时间不能为空！");
                    }
                }).show();
    }

    public void spreadTiming(String powerOn, String powerOff, boolean rightNow) {
        // 折算成今天的【开机时间】
        Timing.DayTimeField powerOnTime = new Timing.DayTimeField(Timing.Epoch.parse(powerOn, TIMING_POWER_ON));
        // 折算成今天的【关机时间】
        Timing.DayTimeField powerOffTime = new Timing.DayTimeField(Timing.Epoch.parse(powerOff, TIMING_POWER_OFF));
        if (powerOnTime.isEarlyAt(powerOffTime)) {
            try {
                Timing.handlePowerSetting(powerOn, powerOff);

                // 超过自动关机时间，是否立即生效
                if (powerOffTime.isEarlyAtNow() && rightNow) {
                    makeLogcat("即将关闭设备");
                    Loople.Task.schedule(() -> {
                        makeLogcat("正在注销设备");
                        Outline.Hub.terminate();

                        Outline.rkapi().shutdown();
                    }, DELAY_SHUTDOWN_MILLIS);
                } else {
                    // 依据条件判断是否开关灯
                    Outline.Hub.boards().forEach((board) -> {
                        ((ClassifyOnboard) board).handleStartLight();
                    });

                    makeLogcat("自动开关机设置成功！");
                }
            } catch (Exception e) {
                makeLogcat("指令操作失败：时间格式错误，请重试！");
            }
        } else {
            makeLogcat("配置错误：关机时间必须小于开机时间！");
        }
    }
}
