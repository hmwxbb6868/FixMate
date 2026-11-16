package com.fixmate.controller.technician;

import com.fixmate.controller.admin.*;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.admin.StaffWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/technician")
public class TechnicianWorkOrderController {
    private final StaffWorkOrderService service;
    private final com.fixmate.service.common.VisitSchedulingService visits;
    private final WorkOrderCommandExecutor commands;
    public TechnicianWorkOrderController(StaffWorkOrderService service,WorkOrderCommandExecutor commands,com.fixmate.service.common.VisitSchedulingService visits){this.visits=visits;this.service=service;this.commands=commands;}
    @GetMapping("/work-orders/{id}") public ResponseEntity<Object> detail(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.technicianDetail(actor.userId(),id));}
    @GetMapping("/participations") public ResponseEntity<Object> participations(@AuthenticationPrincipal JwtPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return AdminCommandExecutor.ok(service.participations(actor.userId(),page,size));}
    @GetMapping("/participations/{id}") public ResponseEntity<Object> participation(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.participation(actor.userId(),id));}
    @PostMapping("/work-orders/{id}/confirm-visit") public ResponseEntity<Object> confirmVisit(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){
        return commands.technician(actor,id,"technicianConfirmVisit",key,"/api/v1/technician/work-orders/"+id+"/confirm-visit",input,()->{visits.confirm(actor.userId(),id,input);return service.technicianDetail(actor.userId(),id);});}
    @PostMapping("/work-orders/{id}/decline-reschedule") public ResponseEntity<Object> declineReschedule(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody VersionCommandDTO input){
        return commands.technician(actor,id,"technicianDeclineReschedule",key,"/api/v1/technician/work-orders/"+id+"/decline-reschedule",input,
                ()->{visits.declineReschedule(actor.userId(),id,input);return null;});}
    @PostMapping("/work-orders/{id}/start-inspection") public ResponseEntity<Object> start(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){
        return commands.technician(actor,id,"technicianStartInspection",key,"/api/v1/technician/work-orders/"+id+"/start-inspection",input,()->service.technicianStart(actor.userId(),id,input));}
    @PostMapping("/work-orders/{id}/service-records") public ResponseEntity<Object> record(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ServiceRecordCreateDTO input){
        return commands.technician(actor,id,"technicianCreateServiceRecord",key,"/api/v1/technician/work-orders/"+id+"/service-records",input,()->service.technicianRecord(actor.userId(),id,input));}
    @PostMapping("/work-orders/{id}/completion") public ResponseEntity<Object> complete(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody WorkOrderCommandDTO input){return commands.technician(actor,id,"technicianCompletion",key,"/api/v1/technician/work-orders/"+id+"/completion",input,()->service.technicianComplete(actor.userId(),id,input));}
}
