# SDK for Classify

## 集成edge-classify
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

## 初始化控制板
```java
     // 初始化板载设备
    Outline.Hub.attach(ClassifyOnboard.class).invoke();
    // 验证控制板是否挂载成功
    if (Outline.Hub.check()) {
        // 遍历打印控制板版本号
        Outline.Hub.boards().stream().forEach((b) -> makeLogcat("[onboard]: %s: %s", b.name(), b.version().toVersion()));
    }
```

## 下发控制信号
```java
    @UiThread
    public void onControlClicked(View view) {
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
```

## 获取传感器数据
```java
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
```

## 监听IC卡/二维码
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

## 监听按钮事件
```java
    private final SerialEvent.Subscriber mRightKeySerialEventSubscriber = new SerialEvent.Subscriber() {

        @Override
        public void onEvent(SerialInode inode, UnitAttribute attr, UnitMeta meta) {
            String inodeName = SerialInode.name(inode);
            UnitIndex attr = M2spLite.index(meta);
            StatusCodes code = M2spLite.valueOf(meta);

            makeLogcat("%s: %s: %s: %s", inodeName, getKeyBox().name(), attr.name(), code.name());
        }
    };
```