package app.snapshot_bitcake;

import java.io.Serializable;

public class ABSnapshotResult implements Serializable {

    private final int serventId;
    private final int recordedAmount;
    private static final long serialVersionUID = 4935599605017116701L;


    public ABSnapshotResult(int serventId, int recordedAmount) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
    }
    public int getServentId() { return serventId; }
    public int getRecordedAmount() { return recordedAmount; }


}
