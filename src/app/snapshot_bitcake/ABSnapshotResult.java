package app.snapshot_bitcake;

import app.CausalBroadcastShared;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ABSnapshotResult implements Serializable {

    private final int serventId;
    private final int recordedAmount;
    private static final long serialVersionUID = 4935599605017116701L;
    private Map<Integer, List<Integer>> SENT;
    private Map<Integer, List<Integer>> RECD;


    public ABSnapshotResult(int serventId, int recordedAmount) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        SENT = CausalBroadcastShared.SENT;
        RECD = CausalBroadcastShared.RECD;
    }
    public int getServentId() { return serventId; }
    public int getRecordedAmount() { return recordedAmount; }

    public Map<Integer, List<Integer>> getSENT() {
        return SENT;
    }

    public void setSENT(Map<Integer, List<Integer>> SENT) {
        this.SENT = SENT;
    }

    public Map<Integer, List<Integer>> getRECD() {
        return RECD;
    }

    public void setRECD(Map<Integer, List<Integer>> RECD) {
        this.RECD = RECD;
    }

    @Override
    public String toString() {
        return "ABSnapshotResult{" +
                "serventId=" + serventId +
                ", recordedAmount=" + recordedAmount +
                ", SENT=" + SENT +
                ", RECD=" + RECD +
                '}';
    }
}
