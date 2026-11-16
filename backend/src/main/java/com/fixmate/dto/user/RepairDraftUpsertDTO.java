package com.fixmate.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 草稿允许字段缺失；一致性校验只在字段已提供时执行。
 *
 * nullable 字段必须区分"未提供"和"显式 null"：前者保持不变，后者表示清空。
 * 因此每个 setter 都记录字段是否出现在请求体里（Jackson 对显式 null 同样会调用 setter）。
 */
public class RepairDraftUpsertDTO {

    private final Set<String> presentFields = new HashSet<>();

    private String applianceType;

    private Long brandId;

    private Long modelId;

    @Size(max = 128)
    private String modelText;

    @Size(max = 64)
    private String serialNumber;

    @Size(max = 1000)
    private String symptomDescription;

    @Size(max = 64)
    private String errorCodeText;

    /** 契约要求 date-time 带偏移；无偏移的字符串按非法参数拒绝。 */
    private OffsetDateTime occurredAt;

    @Size(max = 6)
    private List<Long> mediaFileIds;

    @Valid
    private SafetyAnswersDTO safetyAnswers;

    private Long addressId;

    /** 仅修改草稿时必填；缺失即 400，不得由服务端补当前版本。 */
    @Min(0)
    private Integer expectedVersion;

    public boolean hasField(String field) {
        return presentFields.contains(field);
    }

    /** 参与幂等请求哈希，使"字段缺失"与"显式 null"不再被判为同一个请求。 */
    @JsonProperty("presentFields")
    public List<String> getPresentFields() {
        return presentFields.stream().sorted().toList();
    }

    public String getApplianceType() {
        return applianceType;
    }

    public void setApplianceType(String applianceType) {
        this.applianceType = applianceType;
        presentFields.add("applianceType");
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
        presentFields.add("brandId");
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
        presentFields.add("modelId");
    }

    public String getModelText() {
        return modelText;
    }

    public void setModelText(String modelText) {
        this.modelText = modelText;
        presentFields.add("modelText");
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        presentFields.add("serialNumber");
    }

    public String getSymptomDescription() {
        return symptomDescription;
    }

    public void setSymptomDescription(String symptomDescription) {
        this.symptomDescription = symptomDescription;
        presentFields.add("symptomDescription");
    }

    public String getErrorCodeText() {
        return errorCodeText;
    }

    public void setErrorCodeText(String errorCodeText) {
        this.errorCodeText = errorCodeText;
        presentFields.add("errorCodeText");
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
        presentFields.add("occurredAt");
    }

    public List<Long> getMediaFileIds() {
        return mediaFileIds;
    }

    public void setMediaFileIds(List<Long> mediaFileIds) {
        this.mediaFileIds = mediaFileIds;
        presentFields.add("mediaFileIds");
    }

    public SafetyAnswersDTO getSafetyAnswers() {
        return safetyAnswers;
    }

    public void setSafetyAnswers(SafetyAnswersDTO safetyAnswers) {
        this.safetyAnswers = safetyAnswers;
        presentFields.add("safetyAnswers");
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
        presentFields.add("addressId");
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public boolean hasMediaFileIds() {
        return presentFields.contains("mediaFileIds") && Objects.nonNull(mediaFileIds);
    }
}
