package com.fixmate.controller.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.StaffWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/admin")
public class AdminWorkOrderController {
    private final StaffWorkOrderService service;
    private final WorkOrderCommandExecutor commands;
    public AdminWorkOrderController(StaffWorkOrderService service,WorkOrderCommandExecutor commands){this.service=service;this.commands=commands;}
    @GetMapping("/work-orders") public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) String status,@RequestParam(required=false) @Size(max=100) String keyword,
            @RequestParam(required=false) @Positive Long technicianId,@RequestParam(required=false) String applianceType,
            @RequestParam(required=false) @Size(max=32) String brandCode,@RequestParam(required=false) Boolean riskFlag,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam(required=false) String coverageStatus,@RequestParam(required=false) String queue){
        return AdminCommandExecutor.ok(service.list(actor,page,size,status,keyword,technicianId,applianceType,brandCode,riskFlag,appointmentDate,coverageStatus,queue));}
    @GetMapping("/work-orders/{id}") public ResponseEntity<Object> detail(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.adminDetail(actor,id));}
    @PostMapping("/work-orders/{id}/takeover") public ResponseEntity<Object> takeover(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){
        return commands.admin(actor,id,"adminTakeover",key,path(id,"takeover"),input,()->service.takeover(actor,id,input));}
    @PostMapping("/work-orders/{id}/coordination") public ResponseEntity<Object> coordinate(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCoordinationDTO input){
        return commands.admin(actor,id,"adminCoordination",key,path(id,"coordination"),input,()->service.coordinate(actor,id,input));}
    @PostMapping("/work-orders/{id}/start-inspection") public ResponseEntity<Object> start(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){
        return commands.admin(actor,id,"adminStartInspection",key,path(id,"start-inspection"),input,()->service.adminStart(actor,id,input));}
    @PostMapping("/work-orders/{id}/appointments") public ResponseEntity<Object> appointment(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AppointmentCreateDTO input){
        return commands.admin(actor,id,"createWorkOrderAppointment",key,path(id,"appointments"),input,()->service.createAppointment(actor,id,input));}
    @PostMapping("/appointments/{id}/confirm") public ResponseEntity<Object> confirm(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AppointmentConfirmDTO input){
        return commands.admin(actor,service.appointmentOrderId(id),"confirmWorkOrderAppointment",key,"/api/v1/admin/appointments/"+id+"/confirm",input,()->service.confirmAppointment(actor,id,input));}
    @PostMapping("/work-orders/{id}/service-records") public ResponseEntity<Object> record(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ServiceRecordCreateDTO input){
        return commands.admin(actor,id,"adminCreateServiceRecord",key,path(id,"service-records"),input,()->service.adminRecord(actor,id,input));}
    @PostMapping("/work-orders/{id}/completion") public ResponseEntity<Object> complete(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){return commands.admin(actor,id,"adminCompletion",key,path(id,"completion"),input,()->service.adminComplete(actor,id,input));}
    @PostMapping("/work-orders/{id}/cancel") public ResponseEntity<Object> cancel(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){return commands.admin(actor,id,"adminCancel",key,path(id,"cancel"),input,()->service.close(actor,id,input,false));}
    @PostMapping("/work-orders/{id}/close-unresolved") public ResponseEntity<Object> close(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){return commands.admin(actor,id,"adminCloseUnresolved",key,path(id,"close-unresolved"),input,()->service.close(actor,id,input,true));}
    private String path(Long id,String suffix){return "/api/v1/admin/work-orders/"+id+"/"+suffix;}
}
