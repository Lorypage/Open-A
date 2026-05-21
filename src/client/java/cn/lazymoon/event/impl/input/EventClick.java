package cn.lazymoon.event.impl.input;

import cn.lazymoon.event.api.event.CancellableEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EventClick extends CancellableEvent {
    public BlockPos clickedBlock;
    public Direction direction;

    public EventClick(BlockPos clickedBlock,Direction direction) {
        this.clickedBlock = clickedBlock;
        this.direction = direction;
    }
}
