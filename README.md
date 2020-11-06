# SDK for Classify

0. ## 集成edge-classify

- ### root build.gradle
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

- ### app build.gradle
```gradle
dependencies {
    implementation 'com.cvdnn:android-lang:0.5.3'
    implementation 'com.cvdnn:android-loople:0.1.4'
    implementation 'com.cvdnn:android-frame:0.1.3'
    implementation 'com.cvdnn:serial-port:0.3.6'
    implementation 'com.cvdnn:edge-m2sp:0.7.5'
    implementation 'com.cvdnn:edge-scan:0.3.3'
    implementation 'com.cvdnn:edge-classify:0.16.5'
}
```

0. ## 初始化控制板

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

0. ## 控制板连接监听
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

0. ## 按钮事件监听
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

0. ## 下发控制信号
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

0. ## 获取传感器数据
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

0. ## 监听IC卡/二维码
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

0. ## 监听门限位器状态: LEFT.plate, RIGHT.plate
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