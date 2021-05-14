package servent.message;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import app.snapshot_bitcake.BitcakeManager;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 * @author bmilojkovic
 *
 */
public class BasicMessage implements Message {

	private static final long serialVersionUID = -9075856313609777945L;
	private final MessageType type;
	private final ServentInfo originalSenderInfo;
	private final ServentInfo receiverInfo;
	private final List<ServentInfo> routeList;
	private String messageText;
	private final boolean white;

	private ABSnapshotResult snapshotResult;
	protected transient BitcakeManager bitcakeManager;

	protected int snapshotInitiatorId;

	protected Map<Integer,Integer> senderVectorClock;
	
	//This gives us a unique id - incremented in every natural constructor.
	private static AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = AppConfig.isWhite.get();
		this.routeList = new ArrayList<>();
		this.messageText = "";
		this.senderVectorClock = new HashMap<>();
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			String messageText) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = AppConfig.isWhite.get();
		this.routeList = new ArrayList<>();
		this.messageText = messageText;
		this.senderVectorClock = new HashMap<>();



		this.messageId = messageCounter.getAndIncrement();
	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}

	@Override
	public ServentInfo getOriginalSenderInfo() {
		return originalSenderInfo;
	}

	@Override
	public ServentInfo getReceiverInfo() {
		return receiverInfo;
	}
	
	@Override
	public boolean isWhite() {
		return white;
	}
	
	@Override
	public List<ServentInfo> getRoute() {
		return routeList;
	}
	
	@Override
	public String getMessageText() {
		return messageText;
	}

	@Override
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}



	@Override
	public int getMessageId() {
		return messageId;
	}
	
	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			boolean white, List<ServentInfo> routeList, String messageText, int messageId) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = white;
		this.routeList = routeList;
		this.messageText = messageText;
		
		this.messageId = messageId;
	}
	
	/**
	 * Used when resending a message. It will not change the original owner
	 * (so equality is not affected), but will add us to the route list, so
	 * message path can be retraced later.
	 */
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(routeList);
		newRouteList.add(newRouteItem);
		BasicMessage toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), isWhite(), newRouteList, getMessageText(), getMessageId());

		if (getMessageType() == MessageType.TRANSACTION) {
			toReturn.setSenderVectorClock(getSenderVectorClock());
			toReturn.setBitcakeManager(getBitcakeManager());
		}

		if (getMessageType() == MessageType.AB_TOKEN) {
			toReturn.setSenderVectorClock(getSenderVectorClock());
		}

		if (getMessageType() == MessageType.AB_TELL_AMOUNT) {
			toReturn.setSnapshotInitiatorId(snapshotInitiatorId);
			toReturn.setSnapshotResult(snapshotResult);
		}

		return toReturn;
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId) || newReceiverId == AppConfig.myServentInfo.getId()) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			
			BasicMessage toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
					newReceiverInfo, isWhite(), getRoute(), getMessageText(), getMessageId());

			if (getMessageType() == MessageType.TRANSACTION) {
				toReturn.setSenderVectorClock(getSenderVectorClock());
				toReturn.setBitcakeManager(getBitcakeManager());
			}

			if (getMessageType() == MessageType.AB_TOKEN) {
				toReturn.setSenderVectorClock(getSenderVectorClock());
			}

			if (getMessageType() == MessageType.AB_TELL_AMOUNT) {
				toReturn.setSnapshotInitiatorId(snapshotInitiatorId);
				toReturn.setSnapshotResult(snapshotResult);
			}

			return toReturn;




		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
		
	}
	
	@Override
	public Message setRedColor() {
		Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), false, getRoute(), getMessageText(), getMessageId());
		
		return toReturn;
	}
	
	@Override
	public Message setWhiteColor() {
		Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), true, getRoute(), getMessageText(), getMessageId());
		
		return toReturn;
	}
	
	/**
	 * Comparing messages is based on their unique id and the original sender id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage) {
			BasicMessage other = (BasicMessage)obj;
			
			if (getMessageId() == other.getMessageId() &&
				getOriginalSenderInfo().getId() == other.getOriginalSenderInfo().getId()) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are gonna keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getOriginalSenderInfo().getId());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
					getMessageText() + "|" + getMessageType() + "|" +
					getReceiverInfo().getId() + "]";
	}

	/**
	 * Empty implementation, which will be suitable for most messages.
	 */
	@Override
	public void sendEffect() {

		if(type == MessageType.TRANSACTION){
			int amount = Integer.parseInt(getMessageText());
//			AppConfig.timestampedStandardPrint("send effect  " + amount);

			bitcakeManager.takeSomeBitcakes(amount);
		}

	}

	@Override
	public MessageType getType() {
		return type;
	}
	@Override
	public List<ServentInfo> getRouteList() {
		return routeList;
	}
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	@Override
	public void setBitcakeManager(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}
	@Override
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}
	@Override
	public void setSenderVectorClock(Map<Integer, Integer> senderVectorClock) {
		this.senderVectorClock = senderVectorClock;
	}
	@Override
	public int getSnapshotInitiatorId() {
		return snapshotInitiatorId;
	}
	@Override
	public void setSnapshotInitiatorId(int snapshotInitiatorId) {
		this.snapshotInitiatorId = snapshotInitiatorId;
	}
	@Override
	public ABSnapshotResult getSnapshotResult() {
		return snapshotResult;
	}
	@Override
	public void setSnapshotResult(ABSnapshotResult snapshotResult) {
		this.snapshotResult = snapshotResult;
	}

	public static AtomicInteger getMessageCounter() {
		return messageCounter;
	}

	public static void setMessageCounter(AtomicInteger messageCounter) {
		BasicMessage.messageCounter = messageCounter;
	}


}
