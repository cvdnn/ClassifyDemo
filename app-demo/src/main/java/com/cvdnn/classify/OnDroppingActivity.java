package com.cvdnn.classify;

import android.concurrent.Threads;
import android.edge.classify.onboard.ClassifyOnboard;
import android.edge.classify.onboard.KegBox;
import android.edge.classify.onboard.MotionPending;
import android.edge.classify.onboard.Outline;
import android.serialport.api.SerialInode;

import androidx.annotation.WorkerThread;

import com.ztone.Loople;

import java.util.concurrent.atomic.AtomicReference;

import iot.proto.DefiningDomain.StatusCodes;

import static android.edge.classify.onboard.ClassifyOnboard.VALUE_BLUR;
import static android.edge.classify.onboard.Outline.ErrFlag.ERR_INVOKE;
import static android.edge.classify.onboard.Outline.ErrFlag.ERR_REMOTE;
import static com.cvdnn.classify.OnDroppingActivity.State.BUSY;
import static com.cvdnn.classify.OnDroppingActivity.State.IDLE;
import static iot.proto.DefiningDomain.StatusCodes.OFF;
import static iot.proto.DefiningDomain.StatusCodes.ON;
import static iot.proto.DefiningDomain.StatusCodes.TERMINATE;

/**
 * 模拟垃圾投递过程
 */
public abstract class OnDroppingActivity extends OnEventActivity {

    public enum State {
        IDLE, BUSY, PRESS, SHUTDOWN
    }

    public static final int START_WEIGHT = 5;
    private static final int DELAY_WEIGHTING_MILLIS = 1500;

    public final AtomicReference<State> mOptState = new AtomicReference<>(IDLE);

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

    /**
     * 按键/推送事件监听
     */
    @WorkerThread
    protected final void onBoxSchemeHandle(SerialInode inode, KegBox box) {
        if (box != KegBox.NONE && mOptState.compareAndSet(IDLE, BUSY)) {

            // 装载board对象
            ClassifyOnboard board = Outline.Hub.mapping(inode);
            if (board != null && board.isBind()) {
                // 计重
                final long baseWeight = board.getWeighing(box.weighing);

                makeLogcat("垃圾分类舱门开启中，请耐心等待");
                boolean result = board.Pend.waitingPlateChangeStatus(box.plate, ON);
                if (result) {

                    // 等待投递垃圾，示例5s超时
                    StatusCodes status = board.Pend.handleWaitDropping(box, 5, mMotionDroppingListener);
                    if (status == TERMINATE) {
                        // 投放物件结束关闭舱门，等待舱门关闭成功，超时10秒
                        makeLogcat("垃圾分类舱门即将关闭，请将手移开投递门");

                        result = board.Pend.waitingPlateChangeStatus(box.plate, OFF);
                        if (result) {
                            // 需要称重
                            if (baseWeight != VALUE_BLUR) {
                                makeLogcat("垃圾分类舱门已关闭，请等待计重");

                                Threads.sleep(DELAY_WEIGHTING_MILLIS);

                                final long weight = board.getWeighing(box.weighing);
                                if (weight != VALUE_BLUR) {
                                    final long goodsWeight = weight - baseWeight >= 0 ? weight - baseWeight : 0;
                                    if (goodsWeight > START_WEIGHT) {
                                        makeLogcat("本次投放%d克", goodsWeight);
                                    } else {
                                        makeLogcat("本次没有投放垃圾，暂无积分累加，请继续操作");
                                    }
                                } else {
                                    makeLogcat("称重失败，请联系管理员！！！");
                                }

                                // 无需称重
                            } else {
                                makeLogcat("垃圾分类舱门已关闭，本次垃圾投放结束");
                            }
                        }
                    }
                }

                board.Pend.onRemovePlateEvent();

                if (result) {
                    // 满溢检测
                    handleCapacityStatus(board, box);

                } else {
                    makeLogcat("%s，请和管理员联系！！！", ERR_INVOKE.message);

                    board.set(box.plate, OFF);
                }
            } else {
                makeLogcat("%s，请和管理员联系！！！", ERR_REMOTE.message);
            }

            box = KegBox.NONE;
            mOptState.set(IDLE);
        } else if (mOptState.get() == BUSY) {
            makeToast("正在投递垃圾中，请稍等再投递垃圾！");
        }
    }

    /**
     * 等待投递过程监听
     */
    private final MotionPending.OnDroppingListener mMotionDroppingListener = (second -> makeLogcat("DROPING: %ds", second));

    /**
     * 满溢检测
     *
     * @param board
     * @param box
     */
    private void handleCapacityStatus(ClassifyOnboard board, KegBox box) {
        Loople.Task.schedule(() -> makeLogcat("CapacityStatus: %d", board.getTrashCapacity(box.capacity)));
    }
}
