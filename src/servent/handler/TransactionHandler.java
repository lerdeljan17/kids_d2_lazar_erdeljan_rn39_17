package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	public static Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<Message, Boolean>());

	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {

		//Sanity check.
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
					clientMessage.getOriginalSenderInfo() :
					clientMessage.getRoute().get(clientMessage.getRoute().size()-1);

			/*
			 * The standard read message already prints out that we got a msg.
			 * However, we also want to see who sent this to us directly, besides from
			 * seeing the original owner - if we are not in a clique, this might
			 * not be the same node.
			 */
			String text = String.format("Got %s from %s broadcast by %s",
					clientMessage.getMessageText(), lastSenderInfo, senderInfo);

			AppConfig.timestampedStandardPrint(text);

			TransactionMessage transactionMessage = (TransactionMessage) clientMessage;
				if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
					//We are the sender :o someone bounced this back to us. /ignore
					AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
				} else {
					//Try to put in the set. Thread safe add ftw.
					boolean didPut = receivedBroadcasts.add(clientMessage);

					if (didPut) {
						//New message for us. Rebroadcast it.


						transactionMessage.setBitcakeManager(bitcakeManager);
						CausalBroadcastShared.addPendingMessage(transactionMessage);
						CausalBroadcastShared.checkPendingMessages();

						AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

						for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
							//Same message, different receiver, and add us to the route table.
							MessageUtil.sendMessage(transactionMessage.changeReceiver(neighbor).makeMeASender());
						}

					} else {
						//We already got this from somewhere else. /ignore
						AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
					}
				}



		}

	}

	public static void handleTransaction(Message clientMessage){



		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			BitcakeManager bitcakeManager = clientMessage.getBitcakeManager();

			String amountString = clientMessage.getMessageText();

			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}

			bitcakeManager.addSomeBitcakes(amountNumber);



		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}

	}

}
