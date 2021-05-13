package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;


public class ABTellAmountMessage extends BasicMessage {

    ABSnapshotResult snapshotResult;

    private static final long serialVersionUID = -4371298535886624175L;

    public ABTellAmountMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,ABSnapshotResult snapshotResult
    ,int initId) {
        super(MessageType.AB_TELL_AMOUNT, originalSenderInfo, receiverInfo);
        super.setSnapshotResult(snapshotResult);
        super.snapshotInitiatorId = initId;
    }

}
