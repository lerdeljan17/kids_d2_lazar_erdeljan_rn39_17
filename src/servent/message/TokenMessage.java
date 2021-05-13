package servent.message;

import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;

import java.util.List;
import java.util.Map;

public class TokenMessage extends BasicMessage  {

    private static final long serialVersionUID = 7714221016296501275L;


    public TokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, int initId, Map<Integer,Integer> vectorClock) {
        super(MessageType.AB_TOKEN, originalSenderInfo, receiverInfo);
        super.snapshotInitiatorId = initId;
        super.senderVectorClock = vectorClock;
    }

    public TokenMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(type, originalSenderInfo, receiverInfo);
    }

    public TokenMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, String messageText) {
        super(type, originalSenderInfo, receiverInfo, messageText);
    }

    protected TokenMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, boolean white, List<ServentInfo> routeList, String messageText, int messageId) {
        super(type, originalSenderInfo, receiverInfo, white, routeList, messageText, messageId);
    }

}
