package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

public class CrashRecoveryPacket extends CrtpPacket {

    public CrashRecoveryPacket() {
        super(0, CrtpPort.PLATFORM);
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte) 2); // CRASH_RECOVERY_COMMAND
    }

    @Override
    protected int getDataByteCount() {
        return 1;
    }
}
