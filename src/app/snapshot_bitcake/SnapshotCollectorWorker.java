package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<Integer, ABSnapshotResult>();

	
	private SnapshotType snapshotType ;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
//		this.bitcakeManager = bitcakeManager;

		switch(snapshotType) {
		case AB:
			bitcakeManager = new ABitcakeManager();
			break;
		case NONE:
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			switch (snapshotType) {
//			case NAIVE:
//				Message askMessage = new NaiveAskAmountMessage(AppConfig.myServentInfo, null);
//
//				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
//					askMessage = askMessage.changeReceiver(neighbor);
//
//					MessageUtil.sendMessage(askMessage);
//				}
//				collectedNaiveValues.put("node"+AppConfig.myServentInfo.getId(), bitcakeManager.getCurrentBitcakeAmount());
//				break;
			case AB:
				// TODO: 13.5.2021. iniciraj snapshot
				((ABitcakeManager)bitcakeManager).doSnapshot();
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case AB:
					AppConfig.timestampedStandardPrint("broj odgovora " + collectedABValues.size());
					if (collectedABValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case NONE:
					//Shouldn't be able to come here. See constructor. 
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			//print
			int sum;
			switch (snapshotType) {
			case AB:
				sum = 0;
				for (Entry<Integer, ABSnapshotResult> itemAmount : collectedABValues.entrySet()) {

					sum += itemAmount.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Info for " + itemAmount.getKey() + " = " + itemAmount.getValue().getRecordedAmount() + " bitcake");
				}


				sum += sumABChannel();



				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);


				collectedABValues.clear(); //reset for next invocation
				break;

			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			collecting.set(false);
		}

	}

	private int sumABChannel(){
		int sum=0;
		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			for (int j = 0; j < AppConfig.getServentCount(); j++) {
				if (i!=j){
					if (AppConfig.getInfoById(i).getNeighbors().contains(j)){
						int lower = collectedABValues.get(i).getSENT().get(j).size();
						AppConfig.timestampedErrorPrint("i = " + i + "  j = " + j + " primio " + collectedABValues.get(j).getRECD().get(i));
						AppConfig.timestampedErrorPrint(collectedABValues.toString());
						int upper = collectedABValues.get(j).getRECD().get(i).size();

						for (int k = upper+1; k <=lower; k++) {
							int add = collectedABValues.get(i).getSENT().get(j).get(k);
							sum+=add;


						}
					}
				}
			}
		}
		return sum;
	}
	
	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
//		collectedNaiveValues.put(snapshotSubject, amount);
	}

	@Override
	public void addABSnapshotInfo(int snapshotSubject, ABSnapshotResult snapshotResult) {
		collectedABValues.putIfAbsent(snapshotSubject,snapshotResult);
	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
