# SDK for Classify

- ## 集成edge-classify
1) ### root build.gradle
```gradle
allprojects {
    repositories {
        google()
        jcenter()

        maven {
            url 'https://dl.bintray.com/cvdnn/maven'
        }
    }
}
```

2) ### app build.gradle
```gradle
    // defaultConfig
    defaultConfig {
        minSdkVersion 24
        targetSdkVersion 28
    }

    // dependencies
    dependencies {
        implementation 'com.cvdnn:android-lang:0.5.35'
        implementation 'com.cvdnn:android-frame:0.1.3'

        implementation 'com.cvdnn:serial-port:0.3.8'
        implementation 'com.cvdnn:edge-m2sp:0.7.15'
        implementation 'com.cvdnn:edge-scan:0.3.5'
        implementation 'com.cvdnn:edge-classify:0.16.25'
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
```

- ## 初始化控制板
1) 设备初始化
```java
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
```

2) 推出设备运行
```java
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Outline.Hub.terminate();
    }
```

> 设备初始化或退出设备运行，设备硬件状态均恢复到默认初始状态，即：投递门关闭，照明灯关闭等

- ## 控制板连接监听
```java
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
```

- ## 按钮事件监听
```java
    @Override
    public void onKeyDown(SerialInode inode, KegBox box) {
        makeLogcat("KeyDown: %s: %s", SerialInode.name(inode), box.name());

        if (mOptState.get() == IDLE) {
            // 模拟垃圾投递过程
            onBoxSchemeHandle(inode, box);
        } else {
            makeLogcat("DEVICE: %s", mOptState.get().name());
        }
    }

    @Override
    public void onKeyUp(SerialInode inode, KegBox box) {
        makeLogcat("KeyUp: %s: %s", SerialInode.name(inode), box.name());
    }
```

- ## 下发控制信号
```java
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
```

- ## 获取传感器数据
```java
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
```

- ## 监听IC卡/二维码
```java
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
```

- ## 监听门限位器状态: LEFT.plate, RIGHT.plate
```java
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
```

- ## 固件更新
```java
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
```

> 执行OTA前需校验控制板 `型号` 和 `版本号` ，以免刷机失败或控制板无法使用

- ## 混淆规则
```proguard
# edge classify
-dontwarn android.edge.**
-keep public class android.edge.** { *; }
-keepclassmembers class android.edge.** { *; }
-keep public class android.edge.classify.onboard.Onboard$* { *; }
-keep public class * extends android.edge.classify.onboard.Onboard {
    public <init>(android.edge.classify.onboard.Onboard$Wrap);
}
-keep public class * extends android.edge.classify.event.OnSerialEventMonitor {
    <init>(iot.proto.MultiMeaasgeInterface.UnitAttribute[]);
}

-dontwarn com.ys.**
-keep public class com.ys.** { *; }

-dontwarn startest.ys.com.poweronoff.**
-keep public class startest.ys.com.poweronoff.** { *; }

# edge m2sp
-dontwarn iot.proto.**
-keep class iot.proto.serical.SerialPortKernel { *; }
-keep public class iot.proto.** { *; }

# edge serical
-dontwarn android.serialport.**
-keep class android.serialport.** { *; }
-keepclassmembernames class android.serialport.api.SerialPort { *; }

# edge scan
-dontwarn android.edge.scan.**
-keep public class android.edge.scan.** { *; }

```