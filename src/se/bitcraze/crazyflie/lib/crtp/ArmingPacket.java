package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

public class ArmingPacket extends CrtpPacket {

    private final boolean mArmed;

    public ArmingPacket(boolean armed) {
        super(0, CrtpPort.PLATFORM);
        this.mArmed = armed;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte) 1); // ARMING_CHANNEL_COMMAND
        buffer.put((byte) (mArmed ? 1 : 0));
    }

    @Override
    protected int getDataByteCount() {
        return 2;
    }

    @Override
    public String toString() {
        return "ArmingPacket: " + (mArmed ? "ARM" : "DISARM");
    }
}
