package com.fixmate.dto.user;

public class RepairSubmitResult {

    private final RepairRequestVO request;
    private final SafetyBlockVO block;

    private RepairSubmitResult(RepairRequestVO request, SafetyBlockVO block) {
        this.request = request;
        this.block = block;
    }

    public static RepairSubmitResult submitted(RepairRequestVO request) {
        return new RepairSubmitResult(request, null);
    }

    public static RepairSubmitResult blocked(SafetyBlockVO block) {
        return new RepairSubmitResult(null, block);
    }

    public boolean isBlocked() {
        return block != null;
    }

    public RepairRequestVO getRequest() {
        return request;
    }

    public SafetyBlockVO getBlock() {
        return block;
    }
}
