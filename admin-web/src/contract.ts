// Generated from the unique OpenAPI source. Run scripts/generate-contract.py.
export const contract = {
  "sha256": "871a29d0dc7a3f049dff4e17272460871665d0a36be50fc6b9290bd90eecfaa8",
  "operations": {
    "staffLogin": {
      "path": "/api/v1/admin/auth/login",
      "method": "POST",
      "summary": "后台员工登录",
      "roles": [],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffLoginDTO"
      }
    },
    "staffRefresh": {
      "path": "/api/v1/admin/auth/refresh",
      "method": "POST",
      "summary": "刷新后台会话",
      "roles": [],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/RefreshTokenDTO"
      }
    },
    "staffLogout": {
      "path": "/api/v1/admin/auth/logout",
      "method": "POST",
      "summary": "撤销后台会话",
      "roles": [],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/RefreshTokenDTO"
      }
    },
    "getAdminDashboard": {
      "path": "/api/v1/admin/dashboard",
      "method": "GET",
      "summary": "业务工作台计数",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [],
      "body": null
    },
    "listAdminCustomers": {
      "path": "/api/v1/admin/customers",
      "method": "GET",
      "summary": "用户档案列表",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "keyword",
          "in": "query",
          "description": "用户编号或昵称",
          "schema": {
            "type": "string",
            "maxLength": 64
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "ENABLED",
              "DISABLED"
            ]
          }
        }
      ],
      "body": null
    },
    "listStaff": {
      "path": "/api/v1/admin/staff",
      "method": "GET",
      "summary": "员工档案分页",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        }
      ],
      "body": null
    },
    "createStaff": {
      "path": "/api/v1/admin/staff",
      "method": "POST",
      "summary": "建立员工档案",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffCreateDTO"
      }
    },
    "listStaffOptions": {
      "path": "/api/v1/admin/staff/options",
      "method": "GET",
      "summary": "可派维修人员选项",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [],
      "body": null
    },
    "setStaffStatus": {
      "path": "/api/v1/admin/staff/{id}/status",
      "method": "POST",
      "summary": "启停员工并协调活动任务",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffStatusDTO"
      }
    },
    "issueStaffInvitation": {
      "path": "/api/v1/admin/staff/{id}/invitations",
      "method": "POST",
      "summary": "生成限时一次性邀请",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffInvitationCreateDTO"
      }
    },
    "revokeStaffInvitation": {
      "path": "/api/v1/admin/staff-invitations/{id}/revoke",
      "method": "POST",
      "summary": "撤销邀请",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ReasonCommandDTO"
      }
    },
    "listStaffBindings": {
      "path": "/api/v1/admin/staff-bindings",
      "method": "GET",
      "summary": "绑定审核列表",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "PENDING",
              "ACTIVE",
              "REJECTED",
              "REVOKED"
            ],
            "default": "PENDING"
          }
        }
      ],
      "body": null
    },
    "confirmStaffBinding": {
      "path": "/api/v1/admin/staff-bindings/{id}/confirm",
      "method": "POST",
      "summary": "核实确认绑定",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffBindingDecisionDTO"
      }
    },
    "rejectStaffBinding": {
      "path": "/api/v1/admin/staff-bindings/{id}/reject",
      "method": "POST",
      "summary": "拒绝绑定申请",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffBindingDecisionDTO"
      }
    },
    "revokeStaffBinding": {
      "path": "/api/v1/admin/staff-bindings/{id}/revoke",
      "method": "POST",
      "summary": "撤销员工绑定",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffBindingDecisionDTO"
      }
    },
    "listAdminRepairRequests": {
      "path": "/api/v1/admin/repair-requests",
      "method": "GET",
      "summary": "待人工受理报修列表",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "required": false,
          "schema": {
            "$ref": "#/components/schemas/RepairRequestStatus"
          }
        },
        {
          "name": "pendingReview",
          "in": "query",
          "description": "仅返回待人工核对且尚无工单、无未解除安全事件的报修。",
          "schema": {
            "type": "boolean",
            "default": false
          }
        },
        {
          "name": "repairNo",
          "in": "query",
          "description": "完整报修单号，查询已提交的报修。",
          "schema": {
            "type": "string",
            "maxLength": 32
          }
        },
        {
          "name": "pendingCustomerConfirmation",
          "in": "query",
          "description": "仅返回已核对待确认生成工单的普通报修，及安全解除后等待客户选上门时段的关联报修。",
          "schema": {
            "type": "boolean",
            "default": false
          }
        }
      ],
      "body": null
    },
    "getAdminRepairRequest": {
      "path": "/api/v1/admin/repair-requests/{id}",
      "method": "GET",
      "summary": "人工核对报修原始详情",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "manualTriageRepairRequest": {
      "path": "/api/v1/admin/repair-requests/{id}/manual-triage",
      "method": "POST",
      "summary": "人工核对完成至TRIAGED",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ManualTriageDTO"
      }
    },
    "listAdminWorkOrders": {
      "path": "/api/v1/admin/work-orders",
      "method": "GET",
      "summary": "工单中心分页",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "required": false,
          "schema": {
            "$ref": "#/components/schemas/WorkOrderStatus"
          }
        },
        {
          "name": "keyword",
          "in": "query",
          "required": false,
          "schema": {
            "type": "string",
            "maxLength": 100
          }
        },
        {
          "name": "technicianId",
          "in": "query",
          "description": "按current_assignment_id指向的当前分派目标筛选，包括PENDING；无分派指针的旧工单才兼容technician_id镜像，不匹配历史分派。",
          "required": false,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "applianceType",
          "in": "query",
          "required": false,
          "schema": {
            "$ref": "#/components/schemas/ApplianceType"
          }
        },
        {
          "name": "brandCode",
          "in": "query",
          "required": false,
          "schema": {
            "type": "string",
            "maxLength": 32
          }
        },
        {
          "name": "riskFlag",
          "in": "query",
          "required": false,
          "schema": {
            "type": "boolean"
          }
        },
        {
          "name": "appointmentDate",
          "in": "query",
          "required": false,
          "schema": {
            "type": "string",
            "format": "date"
          }
        },
        {
          "name": "coverageStatus",
          "in": "query",
          "required": false,
          "schema": {
            "type": "string",
            "enum": [
              "REGISTERED",
              "PENDING_SOURCE",
              "COVERED",
              "CONFLICTED",
              "DISABLED"
            ]
          }
        },
        {
          "name": "queue",
          "in": "query",
          "description": "按员工待办或最新大厅发布记录筛选，在分页前应用。",
          "schema": {
            "type": "string",
            "enum": [
              "DISPATCH",
              "APPOINTMENT",
              "COORDINATION",
              "QUOTE_REJECTED",
              "RETURN",
              "POOL_READY",
              "POOL_LIVE",
              "POOL_BLOCKED",
              "POOL_CLAIMED",
              "POOL_WITHDRAWN",
              "DIRECT_PROGRESS",
              "SELF_CLAIM_PROGRESS",
              "CUSTOMER_CANCELLED"
            ]
          }
        }
      ],
      "body": null
    },
    "getAdminWorkOrder": {
      "path": "/api/v1/admin/work-orders/{id}",
      "method": "GET",
      "summary": "完整业务工单详情",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "assignWorkOrder": {
      "path": "/api/v1/admin/work-orders/{id}/assignments",
      "method": "POST",
      "summary": "定向派单或受控改派",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/AssignmentCreateDTO"
      }
    },
    "listWorkOrderAssignments": {
      "path": "/api/v1/admin/work-orders/{id}/assignments",
      "method": "GET",
      "summary": "授权范围内分派历史，按assignedAt/id倒序",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        }
      ],
      "body": null
    },
    "adminTakeover": {
      "path": "/api/v1/admin/work-orders/{id}/takeover",
      "method": "POST",
      "summary": "管理员审计接管",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCommandDTO"
      }
    },
    "adminCoordination": {
      "path": "/api/v1/admin/work-orders/{id}/coordination",
      "method": "POST",
      "summary": "人工协调处理",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCoordinationDTO"
      }
    },
    "adminCancel": {
      "path": "/api/v1/admin/work-orders/{id}/cancel",
      "method": "POST",
      "summary": "取消尚未现场服务工单",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCommandDTO"
      }
    },
    "adminCloseUnresolved": {
      "path": "/api/v1/admin/work-orders/{id}/close-unresolved",
      "method": "POST",
      "summary": "未解决异常关闭",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCommandDTO"
      }
    },
    "adminStartInspection": {
      "path": "/api/v1/admin/work-orders/{id}/start-inspection",
      "method": "POST",
      "summary": "已接管管理员开始检测",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCommandDTO"
      }
    },
    "adminCompletion": {
      "path": "/api/v1/admin/work-orders/{id}/completion",
      "method": "POST",
      "summary": "已接管管理员提交完工",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/WorkOrderCommandDTO"
      }
    },
    "createWorkOrderAppointment": {
      "path": "/api/v1/admin/work-orders/{id}/appointments",
      "method": "POST",
      "summary": "安排或替换当前预约",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/AppointmentCreateDTO"
      }
    },
    "confirmWorkOrderAppointment": {
      "path": "/api/v1/admin/appointments/{id}/confirm",
      "method": "POST",
      "summary": "确认当前预约",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/AppointmentConfirmDTO"
      }
    },
    "adminCreateServiceRecord": {
      "path": "/api/v1/admin/work-orders/{id}/service-records",
      "method": "POST",
      "summary": "已接管管理员提交检测或服务记录",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ServiceRecordCreateDTO"
      }
    },
    "listSafetyEvents": {
      "path": "/api/v1/admin/safety-events",
      "method": "GET",
      "summary": "安全事件列表",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "ALL",
              "OPEN",
              "RESOLVED"
            ],
            "default": "OPEN"
          }
        }
      ],
      "body": null
    },
    "acknowledgeSafetyEvent": {
      "path": "/api/v1/admin/safety-events/{id}/acknowledge",
      "method": "POST",
      "summary": "安全事件acknowledge",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ReasonCommandDTO"
      }
    },
    "resolveSafetyEvent": {
      "path": "/api/v1/admin/safety-events/{id}/resolve",
      "method": "POST",
      "summary": "安全事件resolve",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ReasonCommandDTO"
      }
    },
    "adminListQuotes": {
      "path": "/api/v1/admin/work-orders/{id}/quotes",
      "method": "GET",
      "summary": "当前资源报价工作视图",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "adminCreateQuote": {
      "path": "/api/v1/admin/work-orders/{id}/quotes",
      "method": "POST",
      "summary": "检测后创建版本化报价",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffQuoteCreateDTO"
      }
    },
    "adminUpdateQuote": {
      "path": "/api/v1/admin/quotes/{id}",
      "method": "PUT",
      "summary": "仅编辑DRAFT并使旧审批失效",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffQuoteUpdateDTO"
      }
    },
    "adminSubmitQuote": {
      "path": "/api/v1/admin/quotes/{id}/submit",
      "method": "POST",
      "summary": "提交报价供客户确认，超区间须先通过审批",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/QuoteSubmitCommandDTO"
      }
    },
    "adminWithdrawQuote": {
      "path": "/api/v1/admin/quotes/{id}/withdraw",
      "method": "POST",
      "summary": "撤回草稿或待确认报价",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/QuoteWithdrawCommandDTO"
      }
    },
    "adminRequestQuoteOverride": {
      "path": "/api/v1/admin/quotes/{id}/override-approvals",
      "method": "POST",
      "summary": "申请超区间独立审批",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/QuoteOverrideRequestDTO"
      }
    },
    "approveQuoteOverride": {
      "path": "/api/v1/admin/quote-override-approvals/{id}/approve",
      "method": "POST",
      "summary": "独立管理员approve超区间报价",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/QuoteOverrideDecisionDTO"
      }
    },
    "rejectQuoteOverride": {
      "path": "/api/v1/admin/quote-override-approvals/{id}/reject",
      "method": "POST",
      "summary": "独立管理员reject超区间报价",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/QuoteOverrideDecisionDTO"
      }
    },
    "listUrges": {
      "path": "/api/v1/admin/urges",
      "method": "GET",
      "summary": "客户催办列表",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "PENDING",
              "HANDLED",
              "INVALIDATED"
            ],
            "default": "PENDING"
          }
        }
      ],
      "body": null
    },
    "resolveUrges": {
      "path": "/api/v1/admin/urges/{id}/acknowledge",
      "method": "POST",
      "summary": "处理待办",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ReasonCommandDTO"
      }
    },
    "listAdminQuoteRecords": {
      "path": "/api/v1/admin/quote-records",
      "method": "GET",
      "summary": "报价与上门费记录",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "WAITING_QUOTE_CONFIRMATION",
              "COMPLETED",
              "REJECTED",
              "WITHDRAWN",
              "EXPIRED",
              "VISIT_FEE_RECORDED"
            ]
          }
        },
        {
          "name": "keyword",
          "in": "query",
          "schema": {
            "type": "string",
            "maxLength": 64
          }
        }
      ],
      "body": null
    },
    "listHumanHandoffs": {
      "path": "/api/v1/admin/human-handoffs",
      "method": "GET",
      "summary": "人工待办列表",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "ALL",
              "PENDING",
              "HANDLED"
            ],
            "default": "PENDING"
          }
        }
      ],
      "body": null
    },
    "resolveHumanHandoffs": {
      "path": "/api/v1/admin/human-handoffs/{id}/resolve",
      "method": "POST",
      "summary": "处理待办",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ReasonCommandDTO"
      }
    },
    "listServiceItems": {
      "path": "/api/v1/admin/service-items",
      "method": "GET",
      "summary": "服务或价格配置分页",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER",
        "TECHNICIAN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        }
      ],
      "body": null
    },
    "createServiceItems": {
      "path": "/api/v1/admin/service-items",
      "method": "POST",
      "summary": "新增服务或价格配置",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ServiceItemInputDTO"
      }
    },
    "getServiceItems": {
      "path": "/api/v1/admin/service-items/{id}",
      "method": "GET",
      "summary": "读取服务或价格配置",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER",
        "TECHNICIAN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "updateServiceItems": {
      "path": "/api/v1/admin/service-items/{id}",
      "method": "PUT",
      "summary": "版本校验修改配置不改变历史快照",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ServiceItemUpdateDTO"
      }
    },
    "statusServiceItems": {
      "path": "/api/v1/admin/service-items/{id}/status",
      "method": "POST",
      "summary": "启停配置不物理删除",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ConfigStatusDTO"
      }
    },
    "listPricingRules": {
      "path": "/api/v1/admin/pricing-rules",
      "method": "GET",
      "summary": "服务或价格配置分页",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER",
        "TECHNICIAN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        }
      ],
      "body": null
    },
    "createPricingRules": {
      "path": "/api/v1/admin/pricing-rules",
      "method": "POST",
      "summary": "新增服务或价格配置",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/PricingRuleInputDTO"
      }
    },
    "getPricingRules": {
      "path": "/api/v1/admin/pricing-rules/{id}",
      "method": "GET",
      "summary": "读取服务或价格配置",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER",
        "TECHNICIAN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "updatePricingRules": {
      "path": "/api/v1/admin/pricing-rules/{id}",
      "method": "PUT",
      "summary": "版本校验修改配置不改变历史快照",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/PricingRuleUpdateDTO"
      }
    },
    "statusPricingRules": {
      "path": "/api/v1/admin/pricing-rules/{id}/status",
      "method": "POST",
      "summary": "启停配置不物理删除",
      "roles": [
        "ADMIN",
        "KNOWLEDGE_MANAGER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ConfigStatusDTO"
      }
    },
    "listAuditLogs": {
      "path": "/api/v1/admin/audit-logs",
      "method": "GET",
      "summary": "按授权范围读取审计摘要",
      "roles": [
        "ADMIN",
        "AUDITOR",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "workOrderId",
          "in": "query",
          "required": false,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "decideAssignmentReturn": {
      "path": "/api/v1/admin/assignments/{id}/return-decision",
      "method": "POST",
      "summary": "后台处理退回申请",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/AssignmentReturnDecisionDTO"
      }
    },
    "listPendingOverrideApprovals": {
      "path": "/api/v1/admin/quote-override-approvals",
      "method": "GET",
      "summary": "独立审批待办，默认PENDING，ADMIN按职责分离过滤",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "PENDING",
              "APPROVED",
              "REJECTED",
              "INVALIDATED"
            ],
            "default": "PENDING"
          }
        }
      ],
      "body": null
    },
    "getAuditLogDetail": {
      "path": "/api/v1/admin/audit-logs/{id}",
      "method": "GET",
      "summary": "授权范围审计详情，凭据及无关PII脱敏",
      "roles": [
        "ADMIN",
        "AUDITOR",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "issueNewTechnicianInvitation": {
      "path": "/api/v1/admin/staff-invitations",
      "method": "POST",
      "summary": "签发新人维修邀请链接",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": null
    },
    "listStaffInvitations": {
      "path": "/api/v1/admin/staff-invitations",
      "method": "GET",
      "summary": "已发邀请分页管理，不返回明文邀请码",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "page",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "default": 1
          }
        },
        {
          "name": "size",
          "in": "query",
          "schema": {
            "type": "integer",
            "minimum": 1,
            "maximum": 100,
            "default": 20
          }
        },
        {
          "name": "staffId",
          "in": "query",
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "status",
          "in": "query",
          "schema": {
            "type": "string",
            "enum": [
              "ISSUED",
              "CONSUMED",
              "REVOKED",
              "EXPIRED"
            ]
          }
        }
      ],
      "body": null
    },
    "getStaffInvitation": {
      "path": "/api/v1/admin/staff-invitations/{id}",
      "method": "GET",
      "summary": "邀请生命周期详情，不提供明文找回",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "getStaffBinding": {
      "path": "/api/v1/admin/staff-bindings/{id}",
      "method": "GET",
      "summary": "管理员查看申请人声明及审核说明",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "getAssignmentHistory": {
      "path": "/api/v1/admin/assignments/{id}",
      "method": "GET",
      "summary": "分派及退回改派交接事件详情",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "changeStaffPassword": {
      "path": "/api/v1/admin/auth/password/change",
      "method": "POST",
      "summary": "验证当前密码并修改本人密码",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "TECHNICIAN",
        "KNOWLEDGE_MANAGER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffPasswordChangeDTO"
      }
    },
    "resetStaffPassword": {
      "path": "/api/v1/admin/staff/{id}/password-reset",
      "method": "POST",
      "summary": "另一管理员核验重置员工密码",
      "roles": [
        "ADMIN"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/StaffPasswordResetDTO"
      }
    },
    "confirmRepairServiceContact": {
      "path": "/api/v1/admin/repair-requests/{id}/service-contact/confirm",
      "method": "POST",
      "summary": "核对补齐旧报修服务联系信息",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/ServiceContactConfirmationDTO"
      }
    },
    "adminGetJobPoolPublication": {
      "path": "/api/v1/admin/work-orders/{id}/job-pool",
      "method": "GET",
      "summary": "读取工单最近发布记录及当前根版本",
      "roles": [
        "ADMIN",
        "DISPATCHER",
        "AUDITOR"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        }
      ],
      "body": null
    },
    "adminPublishJobPool": {
      "path": "/api/v1/admin/work-orders/{id}/job-pool",
      "method": "POST",
      "summary": "发布自主接单订单",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/JobPoolPublishDTO"
      }
    },
    "adminWithdrawJobPool": {
      "path": "/api/v1/admin/job-pool/{id}/withdraw",
      "method": "POST",
      "summary": "撤回未接订单",
      "roles": [
        "ADMIN",
        "DISPATCHER"
      ],
      "params": [
        {
          "name": "id",
          "in": "path",
          "required": true,
          "schema": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        {
          "name": "X-Request-Id",
          "in": "header",
          "required": true,
          "description": "请求追踪 ID，8–64 位字母、数字、下划线或连字符",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        },
        {
          "name": "Idempotency-Key",
          "in": "header",
          "required": true,
          "description": "幂等键，8–64 位字母、数字、下划线或连字符；重试必须复用同一值",
          "schema": {
            "type": "string",
            "pattern": "^[A-Za-z0-9_-]{8,64}$"
          }
        }
      ],
      "body": {
        "$ref": "#/components/schemas/JobPoolWithdrawDTO"
      }
    }
  },
  "schemas": {
    "IdString": {
      "type": "string",
      "pattern": "^[1-9][0-9]{0,18}$",
      "example": "1001"
    },
    "NonNegativeVersion": {
      "type": "integer",
      "minimum": 0,
      "example": 3
    },
    "MoneyAmount": {
      "type": "string",
      "pattern": "^[0-9]{1,10}\\.[0-9]{2}$",
      "description": "DECIMAL(12,2)",
      "example": "380.00"
    },
    "Quantity": {
      "type": "string",
      "pattern": "^(?:0\\.(?!000$)[0-9]{3}|[1-9][0-9]{0,6}\\.[0-9]{3})$",
      "description": "DECIMAL(10,3)，必须大于 0",
      "example": "1.000"
    },
    "Currency": {
      "type": "string",
      "enum": [
        "CNY"
      ]
    },
    "SuccessResponseBase": {
      "type": "object",
      "required": [
        "code",
        "message",
        "data",
        "traceId",
        "timestamp"
      ],
      "properties": {
        "code": {
          "type": "string",
          "enum": [
            "OK"
          ]
        },
        "message": {
          "type": "string",
          "example": "success"
        },
        "data": {
          "nullable": true,
          "description": "业务数据；无数据时显式为 null"
        },
        "traceId": {
          "type": "string"
        },
        "timestamp": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "ApiResponseBase": {
      "type": "object",
      "required": [
        "code",
        "message",
        "data",
        "traceId",
        "timestamp"
      ],
      "properties": {
        "code": {
          "type": "string"
        },
        "message": {
          "type": "string"
        },
        "data": {
          "nullable": true
        },
        "traceId": {
          "type": "string"
        },
        "timestamp": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "ValidationErrorResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "VALIDATION_ERROR"
              ]
            }
          }
        }
      ]
    },
    "UnauthorizedResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "UNAUTHORIZED"
              ]
            }
          }
        }
      ]
    },
    "ForbiddenResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "FORBIDDEN"
              ]
            }
          }
        }
      ]
    },
    "NotFoundResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "RESOURCE_NOT_FOUND"
              ]
            }
          }
        }
      ]
    },
    "CommandConflictResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "STATE_CONFLICT",
                "VERSION_CONFLICT",
                "DUPLICATE_REQUEST"
              ]
            }
          }
        }
      ]
    },
    "QuoteCommandConflictResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "QUOTE_NOT_CURRENT",
                "STATE_CONFLICT",
                "VERSION_CONFLICT",
                "DUPLICATE_REQUEST"
              ]
            }
          }
        }
      ]
    },
    "DuplicateRequestResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "DUPLICATE_REQUEST"
              ]
            }
          }
        }
      ]
    },
    "RateLimitedResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "RATE_LIMITED"
              ]
            }
          }
        }
      ]
    },
    "StorageUnavailableResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "STORAGE_UNAVAILABLE"
              ]
            }
          }
        }
      ]
    },
    "WechatUnavailableResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "WECHAT_UNAVAILABLE"
              ]
            }
          }
        }
      ]
    },
    "HardRuleBlockedResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "required": [
            "data"
          ],
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "HARD_RULE_BLOCKED"
              ]
            },
            "data": {
              "$ref": "#/components/schemas/SafetyBlockVO"
            }
          }
        }
      ],
      "example": {
        "code": "HARD_RULE_BLOCKED",
        "message": "已检测到高风险情况，已转人工处理",
        "traceId": "01J9Z8Y6Q3K4M5N6P7R8S9T0V",
        "timestamp": "2026-10-31T10:33:00+08:00",
        "data": {
          "safetyEventId": "30001",
          "matchedRules": [
            {
              "ruleCode": "ELECTRIC_LEAKAGE",
              "title": "漏电风险",
              "templateText": "已停止接触设备，请勿再次触碰，维修人员将尽快与您联系。"
            }
          ],
          "emergencyWorkOrderId": "80002",
          "emergencyWorkOrderNo": "WO2026103100002",
          "createdAt": "2026-10-31T10:33:00+08:00"
        }
      }
    },
    "PhoneBindUnavailableResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "PHONE_BIND_UNAVAILABLE"
              ]
            }
          }
        }
      ]
    },
    "InternalErrorResponse": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ApiResponseBase"
        },
        {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "enum": [
                "INTERNAL_ERROR"
              ]
            }
          }
        }
      ]
    },
    "WxLoginDTO": {
      "type": "object",
      "required": [
        "code"
      ],
      "properties": {
        "code": {
          "type": "string",
          "minLength": 1,
          "maxLength": 128
        }
      }
    },
    "WxPhoneBindDTO": {
      "type": "object",
      "required": [
        "phoneCode"
      ],
      "properties": {
        "phoneCode": {
          "type": "string",
          "minLength": 1,
          "maxLength": 256
        }
      }
    },
    "RefreshTokenDTO": {
      "type": "object",
      "required": [
        "refreshToken"
      ],
      "properties": {
        "refreshToken": {
          "type": "string",
          "minLength": 16,
          "maxLength": 2048
        }
      }
    },
    "TokenVO": {
      "type": "object",
      "required": [
        "accessToken",
        "refreshToken",
        "expiresIn",
        "user"
      ],
      "properties": {
        "accessToken": {
          "type": "string"
        },
        "refreshToken": {
          "type": "string"
        },
        "expiresIn": {
          "type": "integer",
          "minimum": 1,
          "example": 3600
        },
        "user": {
          "$ref": "#/components/schemas/CurrentUserVO"
        }
      }
    },
    "CurrentUserVO": {
      "type": "object",
      "required": [
        "id",
        "nickname",
        "avatarUrl",
        "phone",
        "phoneVerified"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "nickname": {
          "type": "string",
          "nullable": true
        },
        "avatarUrl": {
          "type": "string",
          "nullable": true
        },
        "phone": {
          "type": "string",
          "nullable": true
        },
        "phoneVerified": {
          "type": "boolean"
        }
      }
    },
    "FileAssetVO": {
      "type": "object",
      "required": [
        "id",
        "url",
        "mimeType",
        "sizeBytes",
        "sha256"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "url": {
          "type": "string"
        },
        "mimeType": {
          "type": "string"
        },
        "sizeBytes": {
          "type": "integer",
          "minimum": 1
        },
        "sha256": {
          "type": "string",
          "pattern": "^[0-9a-f]{64}$"
        }
      }
    },
    "ApplianceType": {
      "type": "string",
      "enum": [
        "AIR_CONDITIONER",
        "REFRIGERATOR",
        "WASHING_MACHINE",
        "AIR_SOURCE_HEAT_PUMP"
      ]
    },
    "BrandCode": {
      "type": "string",
      "enum": [
        "HAIER",
        "MIDEA",
        "SIEMENS"
      ]
    },
    "CoverageStatus": {
      "type": "string",
      "enum": [
        "REGISTERED",
        "PENDING_SOURCE",
        "COVERED",
        "CONFLICTED",
        "DISABLED"
      ]
    },
    "SelectableCoverageStatus": {
      "type": "string",
      "enum": [
        "REGISTERED",
        "PENDING_SOURCE",
        "COVERED"
      ]
    },
    "EnumOption": {
      "type": "object",
      "required": [
        "code",
        "label"
      ],
      "properties": {
        "code": {
          "type": "string"
        },
        "label": {
          "type": "string"
        }
      }
    },
    "BrandOption": {
      "type": "object",
      "required": [
        "id",
        "code",
        "name"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "code": {
          "$ref": "#/components/schemas/BrandCode"
        },
        "name": {
          "type": "string"
        }
      }
    },
    "SelectableModelOption": {
      "type": "object",
      "required": [
        "id",
        "brandId",
        "brandCode",
        "applianceType",
        "modelCode",
        "coverageStatus"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "brandId": {
          "$ref": "#/components/schemas/IdString"
        },
        "brandCode": {
          "$ref": "#/components/schemas/BrandCode"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "modelCode": {
          "type": "string"
        },
        "displayName": {
          "type": "string",
          "nullable": true
        },
        "coverageStatus": {
          "$ref": "#/components/schemas/SelectableCoverageStatus"
        }
      }
    },
    "RepairFormMetaVO": {
      "type": "object",
      "required": [
        "applianceTypes",
        "brands",
        "models",
        "safetyQuestions"
      ],
      "properties": {
        "applianceTypes": {
          "type": "array",
          "minItems": 4,
          "items": {
            "$ref": "#/components/schemas/EnumOption"
          }
        },
        "brands": {
          "type": "array",
          "minItems": 1,
          "items": {
            "$ref": "#/components/schemas/BrandOption"
          }
        },
        "models": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/SelectableModelOption"
          }
        },
        "safetyQuestions": {
          "type": "array",
          "minItems": 5,
          "items": {
            "$ref": "#/components/schemas/EnumOption"
          }
        }
      }
    },
    "AddressCreateDTO": {
      "type": "object",
      "required": [
        "contactName",
        "contactPhone",
        "province",
        "city",
        "district",
        "detail"
      ],
      "properties": {
        "contactName": {
          "type": "string",
          "minLength": 1,
          "maxLength": 32
        },
        "contactPhone": {
          "type": "string",
          "pattern": "^1[3-9]\\d{9}$"
        },
        "province": {
          "type": "string",
          "minLength": 1,
          "maxLength": 32
        },
        "city": {
          "type": "string",
          "minLength": 1,
          "maxLength": 32
        },
        "district": {
          "type": "string",
          "minLength": 1,
          "maxLength": 32
        },
        "detail": {
          "type": "string",
          "minLength": 1,
          "maxLength": 200
        },
        "isDefault": {
          "type": "boolean",
          "default": false
        }
      }
    },
    "AddressUpdateDTO": {
      "allOf": [
        {
          "$ref": "#/components/schemas/AddressCreateDTO"
        },
        {
          "type": "object",
          "required": [
            "version"
          ],
          "properties": {
            "version": {
              "$ref": "#/components/schemas/NonNegativeVersion"
            }
          }
        }
      ]
    },
    "AddressVO": {
      "type": "object",
      "required": [
        "id",
        "contactName",
        "contactPhone",
        "contactPhoneVerified",
        "province",
        "city",
        "district",
        "detail",
        "isDefault",
        "version"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "contactName": {
          "type": "string"
        },
        "contactPhone": {
          "type": "string"
        },
        "contactPhoneVerified": {
          "type": "boolean"
        },
        "province": {
          "type": "string"
        },
        "city": {
          "type": "string"
        },
        "district": {
          "type": "string"
        },
        "detail": {
          "type": "string"
        },
        "isDefault": {
          "type": "boolean"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "SafetyAnswersDTO": {
      "type": "object",
      "required": [
        "smokeOrBurningSmell",
        "electricLeakage",
        "gasSmell",
        "suspectedRefrigerantLeak",
        "severeWaterIngress"
      ],
      "properties": {
        "smokeOrBurningSmell": {
          "type": "boolean"
        },
        "electricLeakage": {
          "type": "boolean"
        },
        "gasSmell": {
          "type": "boolean"
        },
        "suspectedRefrigerantLeak": {
          "type": "boolean"
        },
        "severeWaterIngress": {
          "type": "boolean"
        }
      }
    },
    "RepairDraftUpsertDTO": {
      "type": "object",
      "description": "草稿允许空对象；一致性校验仅在字段已提供时执行",
      "properties": {
        "applianceType": {
          "type": "string",
          "nullable": true,
          "enum": [
            "AIR_CONDITIONER",
            "REFRIGERATOR",
            "WASHING_MACHINE",
            "AIR_SOURCE_HEAT_PUMP"
          ]
        },
        "brandId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "modelId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "modelText": {
          "type": "string",
          "nullable": true,
          "maxLength": 128
        },
        "serialNumber": {
          "type": "string",
          "nullable": true,
          "maxLength": 64
        },
        "symptomDescription": {
          "type": "string",
          "nullable": true,
          "maxLength": 1000
        },
        "errorCodeText": {
          "type": "string",
          "nullable": true,
          "maxLength": 64
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "mediaFileIds": {
          "type": "array",
          "maxItems": 6,
          "items": {
            "$ref": "#/components/schemas/IdString"
          }
        },
        "safetyAnswers": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/SafetyAnswersDTO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "addressId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        }
      }
    },
    "SubmitRequestDTO": {
      "type": "object",
      "required": [
        "expectedVersion"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "VersionedCommandDTO": {
      "type": "object",
      "required": [
        "expectedVersion"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "CancelRequestDTO": {
      "type": "object",
      "required": [
        "expectedVersion"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "maxLength": 200
        }
      }
    },
    "DisableRequestDTO": {
      "type": "object",
      "required": [
        "expectedVersion"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "RepairRequestStatus": {
      "type": "string",
      "enum": [
        "DRAFT",
        "SUBMITTED",
        "COLLECTING",
        "TRIAGED",
        "CONVERTED",
        "HIGH_RISK_BLOCKED",
        "CANCELLED",
        "HUMAN_HANDOFF"
      ]
    },
    "RepairRequestAllowedAction": {
      "type": "string",
      "enum": [
        "EDIT_DRAFT",
        "SUBMIT",
        "CANCEL",
        "VIEW_WORK_ORDER",
        "CONTACT_HUMAN",
        "CONVERT_TO_WORK_ORDER"
      ],
      "description": "阶段 1 不会返回 CONVERT_TO_WORK_ORDER"
    },
    "RepairRequestVO": {
      "type": "object",
      "required": [
        "id",
        "repairNo",
        "status",
        "applianceType",
        "brandId",
        "brandCode",
        "modelId",
        "modelText",
        "serialNumber",
        "symptomDescription",
        "errorCodeText",
        "occurredAt",
        "mediaFileIds",
        "mediaFiles",
        "safetyAnswers",
        "addressId",
        "coverageStatus",
        "workOrderId",
        "allowedActions",
        "version",
        "createdAt"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "repairNo": {
          "type": "string"
        },
        "status": {
          "$ref": "#/components/schemas/RepairRequestStatus"
        },
        "applianceType": {
          "type": "string",
          "nullable": true,
          "description": "草稿阶段可为 null",
          "enum": [
            "AIR_CONDITIONER",
            "REFRIGERATOR",
            "WASHING_MACHINE",
            "AIR_SOURCE_HEAT_PUMP"
          ]
        },
        "brandId": {
          "type": "string",
          "nullable": true
        },
        "brandCode": {
          "type": "string",
          "nullable": true
        },
        "modelId": {
          "type": "string",
          "nullable": true
        },
        "modelCode": {
          "type": "string",
          "nullable": true
        },
        "modelText": {
          "type": "string",
          "nullable": true
        },
        "serialNumber": {
          "type": "string",
          "nullable": true
        },
        "symptomDescription": {
          "type": "string",
          "nullable": true
        },
        "errorCodeText": {
          "type": "string",
          "nullable": true
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "mediaFileIds": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "mediaFiles": {
          "type": "array",
          "description": "该报修关联的故障照片，按上传顺序；无照片时为空数组。url 为 5 分钟短时签名地址",
          "items": {
            "$ref": "#/components/schemas/FileAssetVO"
          }
        },
        "safetyAnswers": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/SafetyAnswersDTO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "addressId": {
          "type": "string",
          "nullable": true
        },
        "coverageStatus": {
          "type": "string",
          "nullable": true,
          "description": "草稿阶段可为 null；有型号后取型号覆盖状态",
          "enum": [
            "REGISTERED",
            "PENDING_SOURCE",
            "COVERED",
            "CONFLICTED",
            "DISABLED"
          ]
        },
        "workOrderId": {
          "type": "string",
          "nullable": true
        },
        "allowedActions": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/RepairRequestAllowedAction"
          }
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "SafetyRuleCode": {
      "type": "string",
      "enum": [
        "ELECTRIC_LEAKAGE",
        "SMOKE_FIRE",
        "GAS_LEAK",
        "REFRIGERANT_LEAK",
        "SEVERE_WATER_INGRESS"
      ]
    },
    "SafetyRuleDetail": {
      "type": "object",
      "required": [
        "ruleCode",
        "title",
        "templateText"
      ],
      "properties": {
        "ruleCode": {
          "$ref": "#/components/schemas/SafetyRuleCode"
        },
        "title": {
          "type": "string"
        },
        "templateText": {
          "type": "string",
          "description": "该规则对应的固定安全指引，页面必须展示"
        }
      }
    },
    "SafetyBlockVO": {
      "type": "object",
      "required": [
        "safetyEventId",
        "matchedRules",
        "emergencyWorkOrderId",
        "emergencyWorkOrderNo",
        "createdAt"
      ],
      "properties": {
        "safetyEventId": {
          "$ref": "#/components/schemas/IdString"
        },
        "matchedRules": {
          "type": "array",
          "minItems": 1,
          "maxItems": 5,
          "uniqueItems": true,
          "description": "全部命中规则的唯一事实源，页面必须全部展示",
          "items": {
            "$ref": "#/components/schemas/SafetyRuleDetail"
          }
        },
        "emergencyWorkOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "emergencyWorkOrderNo": {
          "type": "string"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "WorkOrderStatus": {
      "type": "string",
      "enum": [
        "WAITING_MANUAL_REVIEW",
        "WAITING_CUSTOMER_VISIT",
        "WAITING_ASSIGNMENT",
        "WAITING_APPOINTMENT",
        "APPOINTED",
        "IN_INSPECTION",
        "WAITING_QUOTE",
        "WAITING_QUOTE_CONFIRMATION",
        "IN_REPAIR",
        "PAUSED_FOR_REQUOTE",
        "WAITING_ACCEPTANCE",
        "COMPLETED",
        "CANCELLED",
        "CLOSED_UNRESOLVED"
      ]
    },
    "CancelWorkOrderDTO": {
      "type": "object",
      "required": [
        "expectedVersion"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedVisitFee": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}(\\.[0-9]{1,2})?$",
          "description": "客户确认的上门费，收费取消时必须提供。"
        }
      }
    },
    "AdminQuoteRecordVO": {
      "type": "object",
      "required": [
        "id",
        "workOrderId",
        "workOrderNo",
        "quoteType",
        "status",
        "amount",
        "recordedAt"
      ],
      "properties": {
        "id": {
          "type": "string"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string"
        },
        "quoteNo": {
          "type": "string",
          "nullable": true
        },
        "quoteType": {
          "type": "string"
        },
        "status": {
          "type": "string"
        },
        "amount": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "visitFeeAmount": {
          "type": "string",
          "nullable": true
        },
        "recordedAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "AdminQuoteRecordVOPageVO": {
      "type": "object",
      "required": [
        "items",
        "page",
        "size",
        "total"
      ],
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AdminQuoteRecordVO"
          }
        },
        "page": {
          "type": "integer"
        },
        "size": {
          "type": "integer"
        },
        "total": {
          "type": "integer",
          "format": "int64"
        }
      }
    },
    "WorkOrderAllowedAction": {
      "type": "string",
      "enum": [
        "RESCHEDULE",
        "SELECT_PREFERRED_VISIT",
        "URGE",
        "HUMAN_HANDOFF",
        "ACCEPT_QUOTE",
        "REJECT_QUOTE",
        "ACCEPTANCE",
        "RAISE_ACCEPTANCE_ISSUE",
        "CANCEL_WORK_ORDER",
        "CANCEL_WITH_VISIT_FEE"
      ]
    },
    "WorkOrderListItemVO": {
      "type": "object",
      "required": [
        "id",
        "workOrderNo",
        "status",
        "applianceType",
        "brandCode",
        "modelCode",
        "appointmentAt",
        "createdAt"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string"
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "brandCode": {
          "type": "string",
          "nullable": true
        },
        "modelCode": {
          "type": "string",
          "nullable": true
        },
        "appointmentAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "WorkOrderDetailVO": {
      "type": "object",
      "required": [
        "id",
        "workOrderNo",
        "status",
        "applianceType",
        "brandCode",
        "modelCode",
        "symptomDescription",
        "errorCodeText",
        "occurredAt",
        "mediaFiles",
        "contactName",
        "contactPhone",
        "addressText",
        "appointment",
        "technicianName",
        "currentQuote",
        "quoteHistory",
        "serviceRecords",
        "acceptance",
        "timeline",
        "allowedActions",
        "version"
      ],
      "properties": {
        "cancellationFeeAmount": {
          "type": "string",
          "nullable": true,
          "description": "确认取消时需支付或已登记应付的上门费。"
        },
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string"
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "brandCode": {
          "type": "string",
          "nullable": true
        },
        "modelCode": {
          "type": "string",
          "nullable": true
        },
        "symptomDescription": {
          "type": "string"
        },
        "errorCodeText": {
          "type": "string",
          "nullable": true,
          "description": "用户填写的故障码原文；来自关联的报修记录，未填时为 null"
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true,
          "description": "用户填写的故障发生时间；来自关联的报修记录，未填时为 null"
        },
        "mediaFiles": {
          "type": "array",
          "description": "该工单关联报修的故障照片，按上传顺序；无照片时为空数组。url 为 5 分钟短时签名地址",
          "items": {
            "$ref": "#/components/schemas/FileAssetVO"
          }
        },
        "contactName": {
          "type": "string"
        },
        "contactPhone": {
          "type": "string"
        },
        "addressText": {
          "type": "string"
        },
        "appointment": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/AppointmentVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "technicianName": {
          "type": "string",
          "nullable": true
        },
        "currentQuote": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/QuoteVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "quoteHistory": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/QuoteVO"
          }
        },
        "serviceRecords": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/ServiceRecordVO"
          }
        },
        "acceptance": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/AcceptanceVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "timeline": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/TimelineEventVO"
          }
        },
        "allowedActions": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/WorkOrderAllowedAction"
          }
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "AppointmentStatus": {
      "type": "string",
      "enum": [
        "PENDING",
        "CONFIRMED",
        "RESCHEDULE_REQUESTED",
        "COMPLETED",
        "CANCELLED",
        "MISSED"
      ]
    },
    "AppointmentVO": {
      "type": "object",
      "required": [
        "id",
        "status",
        "scheduledStartAt",
        "scheduledEndAt",
        "technicianName"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "status": {
          "$ref": "#/components/schemas/AppointmentStatus"
        },
        "scheduledStartAt": {
          "type": "string",
          "format": "date-time"
        },
        "scheduledEndAt": {
          "type": "string",
          "format": "date-time"
        },
        "technicianName": {
          "type": "string",
          "nullable": true
        },
        "note": {
          "type": "string",
          "nullable": true
        }
      }
    },
    "AppointmentRescheduleDTO": {
      "type": "object",
      "required": [
        "expectedWorkOrderVersion",
        "startAt",
        "endAt",
        "reason"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "工单聚合根版本，对应 WorkOrderDetailVO.version"
        },
        "startAt": {
          "type": "string",
          "format": "date-time"
        },
        "endAt": {
          "type": "string",
          "format": "date-time"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 200
        }
      }
    },
    "UrgeCreateDTO": {
      "type": "object",
      "required": [
        "expectedWorkOrderVersion"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "maxLength": 200
        }
      }
    },
    "HumanHandoffCreateDTO": {
      "type": "object",
      "required": [
        "expectedWorkOrderVersion",
        "reason"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 500
        }
      }
    },
    "WorkOrderAcceptanceDTO": {
      "type": "object",
      "required": [
        "expectedWorkOrderVersion",
        "accepted"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "accepted": {
          "type": "boolean",
          "enum": [
            true
          ],
          "description": "只允许 true；false 返回 400 VALIDATION_ERROR"
        },
        "note": {
          "type": "string",
          "maxLength": 500
        }
      }
    },
    "QuoteStatus": {
      "type": "string",
      "enum": [
        "DRAFT",
        "SUBMITTED",
        "ACCEPTED",
        "REJECTED",
        "WITHDRAWN",
        "EXPIRED"
      ]
    },
    "QuoteType": {
      "type": "string",
      "enum": [
        "INITIAL",
        "ADDITIONAL"
      ]
    },
    "FeeType": {
      "type": "string",
      "enum": [
        "VISIT",
        "INSPECTION",
        "LABOR",
        "PART",
        "MATERIAL",
        "OTHER"
      ]
    },
    "QuoteItemVO": {
      "type": "object",
      "required": [
        "id",
        "feeType",
        "itemName",
        "specification",
        "unit",
        "unitPrice",
        "quantity",
        "amount",
        "pricingClauseNo",
        "pricingClauseTitle",
        "pricingRuleVersion",
        "pricingBasisSnapshot",
        "partSourceNote",
        "additionReason"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "feeType": {
          "$ref": "#/components/schemas/FeeType"
        },
        "itemName": {
          "type": "string"
        },
        "specification": {
          "type": "string",
          "nullable": true
        },
        "unit": {
          "type": "string"
        },
        "unitPrice": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "quantity": {
          "$ref": "#/components/schemas/Quantity"
        },
        "amount": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "pricingClauseNo": {
          "type": "string"
        },
        "pricingClauseTitle": {
          "type": "string"
        },
        "pricingRuleVersion": {
          "type": "string"
        },
        "pricingBasisSnapshot": {
          "type": "string"
        },
        "partSourceNote": {
          "type": "string",
          "nullable": true
        },
        "additionReason": {
          "type": "string",
          "nullable": true
        }
      }
    },
    "QuoteVO": {
      "type": "object",
      "required": [
        "id",
        "quoteNo",
        "quoteType",
        "versionNo",
        "version",
        "previousQuoteId",
        "baseQuoteId",
        "status",
        "currency",
        "subtotalAmount",
        "confirmedAmountBefore",
        "currentAdditionalAmount",
        "payableTotalAfterAcceptance",
        "changeReason",
        "expiresAt",
        "submittedAt",
        "items"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "quoteNo": {
          "type": "string"
        },
        "quoteType": {
          "$ref": "#/components/schemas/QuoteType"
        },
        "versionNo": {
          "type": "integer",
          "minimum": 1,
          "description": "报价业务版本号"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "乐观锁版本"
        },
        "previousQuoteId": {
          "type": "string",
          "nullable": true
        },
        "baseQuoteId": {
          "type": "string",
          "nullable": true
        },
        "status": {
          "$ref": "#/components/schemas/QuoteStatus"
        },
        "currency": {
          "$ref": "#/components/schemas/Currency"
        },
        "subtotalAmount": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "confirmedAmountBefore": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "currentAdditionalAmount": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "payableTotalAfterAcceptance": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "changeReason": {
          "type": "string",
          "nullable": true
        },
        "expiresAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "submittedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "items": {
          "type": "array",
          "minItems": 1,
          "items": {
            "$ref": "#/components/schemas/QuoteItemVO"
          }
        }
      }
    },
    "QuoteAcceptDTO": {
      "type": "object",
      "required": [
        "expectedQuoteVersion"
      ],
      "properties": {
        "expectedQuoteVersion": {
          "type": "integer",
          "minimum": 1,
          "description": "报价业务版本号 versionNo"
        }
      }
    },
    "QuoteRejectDTO": {
      "type": "object",
      "required": [
        "expectedQuoteVersion",
        "reason"
      ],
      "properties": {
        "expectedQuoteVersion": {
          "type": "integer",
          "minimum": 1
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 200
        }
      }
    },
    "ServiceRecordVO": {
      "type": "object",
      "required": [
        "id",
        "inspectedAt",
        "technicianName",
        "summary"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "inspectedAt": {
          "type": "string",
          "format": "date-time"
        },
        "technicianName": {
          "type": "string"
        },
        "summary": {
          "type": "string"
        }
      }
    },
    "AcceptanceVO": {
      "type": "object",
      "required": [
        "accepted",
        "note",
        "acceptedAt"
      ],
      "properties": {
        "accepted": {
          "type": "boolean"
        },
        "note": {
          "type": "string",
          "nullable": true
        },
        "acceptedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        }
      }
    },
    "TimelineEventVO": {
      "type": "object",
      "required": [
        "eventType",
        "title",
        "detail",
        "occurredAt"
      ],
      "properties": {
        "eventType": {
          "type": "string"
        },
        "title": {
          "type": "string"
        },
        "detail": {
          "type": "string",
          "nullable": true
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "PageOfWorkOrderListItem": {
      "type": "object",
      "required": [
        "items",
        "page",
        "size",
        "total",
        "hasNext"
      ],
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/WorkOrderListItemVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1
        },
        "total": {
          "type": "integer",
          "minimum": 0
        },
        "hasNext": {
          "type": "boolean"
        }
      }
    },
    "VersionCommandDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "expectedVersion"
      ],
      "additionalProperties": false
    },
    "ReasonCommandDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "RepairRecordListItemVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "repairNo": {
          "type": "string",
          "maxLength": 64
        },
        "repairStatus": {
          "$ref": "#/components/schemas/RepairRequestStatus"
        },
        "applianceType": {
          "type": "string",
          "nullable": true,
          "enum": [
            "AIR_CONDITIONER",
            "REFRIGERATOR",
            "WASHING_MACHINE",
            "AIR_SOURCE_HEAT_PUMP",
            null
          ]
        },
        "brandCode": {
          "type": "string",
          "maxLength": 32,
          "nullable": true
        },
        "modelText": {
          "type": "string",
          "maxLength": 128,
          "nullable": true
        },
        "symptomSummary": {
          "type": "string",
          "maxLength": 120,
          "nullable": true
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "workOrderId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "workOrderStatus": {
          "type": "string",
          "nullable": true,
          "enum": [
            "WAITING_MANUAL_REVIEW",
            "WAITING_CUSTOMER_VISIT",
            "WAITING_ASSIGNMENT",
            "WAITING_APPOINTMENT",
            "APPOINTED",
            "IN_INSPECTION",
            "WAITING_QUOTE",
            "WAITING_QUOTE_CONFIRMATION",
            "IN_REPAIR",
            "PAUSED_FOR_REQUOTE",
            "WAITING_ACCEPTANCE",
            "COMPLETED",
            "CANCELLED",
            "CLOSED_UNRESOLVED",
            null
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "id",
        "repairNo",
        "repairStatus",
        "applianceType",
        "brandCode",
        "modelText",
        "symptomSummary",
        "createdAt",
        "workOrderId",
        "workOrderStatus",
        "version"
      ]
    },
    "RepairRecordListItemVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/RepairRecordListItemVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "RepairRecordDetailVO": {
      "type": "object",
      "properties": {
        "cancellationFeeAmount": {
          "type": "string",
          "nullable": true,
          "description": "确认取消时需支付或已登记应付的上门费。"
        },
        "preferredVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "visitPolicy": {
          "type": "string",
          "enum": [
            "LEGACY_PLATFORM",
            "CUSTOMER_SLOT"
          ]
        },
        "repairRequest": {
          "$ref": "#/components/schemas/RepairRequestSnapshotVO"
        },
        "workOrderId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "workOrderStatus": {
          "type": "string",
          "nullable": true,
          "enum": [
            "WAITING_MANUAL_REVIEW",
            "WAITING_CUSTOMER_VISIT",
            "WAITING_ASSIGNMENT",
            "WAITING_APPOINTMENT",
            "APPOINTED",
            "IN_INSPECTION",
            "WAITING_QUOTE",
            "WAITING_QUOTE_CONFIRMATION",
            "IN_REPAIR",
            "PAUSED_FOR_REQUOTE",
            "WAITING_ACCEPTANCE",
            "COMPLETED",
            "CANCELLED",
            "CLOSED_UNRESOLVED",
            null
          ]
        },
        "workOrderVersion": {
          "type": "integer",
          "minimum": 0,
          "nullable": true
        },
        "allowedActions": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "CONTINUE_DRAFT",
              "CANCEL",
              "VIEW_WORK_ORDER",
              "SELECT_PREFERRED_VISIT",
              "CONVERT_TO_WORK_ORDER",
              "RAISE_ACCEPTANCE_ISSUE",
              "CANCEL_WORK_ORDER",
              "CANCEL_WITH_VISIT_FEE"
            ]
          }
        }
      },
      "required": [
        "preferredVisit",
        "visitPolicy",
        "repairRequest",
        "workOrderId",
        "workOrderStatus",
        "workOrderVersion",
        "allowedActions"
      ],
      "description": "repairRequest只读快照不带动作；当前页面仅外层allowedActions为权威。原RepairRequestVO及原页面不改。"
    },
    "WorkspaceAccessVO": {
      "type": "object",
      "properties": {
        "availableWorkspaces": {
          "type": "array",
          "minItems": 1,
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "CUSTOMER",
              "TECHNICIAN"
            ]
          }
        },
        "staffId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "staffName": {
          "type": "string",
          "maxLength": 64,
          "nullable": true
        },
        "staffPhone": {
          "type": "string",
          "maxLength": 20,
          "nullable": true
        },
        "staffRole": {
          "type": "string",
          "nullable": true,
          "enum": [
            "TECHNICIAN",
            null
          ]
        },
        "bindingId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "bindingVersion": {
          "type": "integer",
          "nullable": true,
          "minimum": 0
        }
      },
      "required": [
        "availableWorkspaces",
        "staffId",
        "staffName",
        "staffPhone",
        "staffRole",
        "bindingId",
        "bindingVersion"
      ],
      "description": "首次绑定进入维修，后续偏好仅本机按customerId保存；实时核验，不能用于授予权限。"
    },
    "StaffProfileVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "username": {
          "type": "string",
          "maxLength": 64
        },
        "name": {
          "type": "string",
          "maxLength": 64
        },
        "phone": {
          "type": "string",
          "maxLength": 20,
          "nullable": true
        },
        "role": {
          "type": "string",
          "enum": [
            "ADMIN",
            "DISPATCHER",
            "TECHNICIAN",
            "KNOWLEDGE_MANAGER",
            "AUDITOR"
          ]
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "id",
        "username",
        "name",
        "phone",
        "role",
        "status",
        "version"
      ]
    },
    "AdminCustomerVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "nickname": {
          "type": "string",
          "nullable": true
        },
        "maskedPhone": {
          "type": "string",
          "nullable": true
        },
        "phoneVerified": {
          "type": "boolean"
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "lastLoginAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        }
      },
      "required": [
        "id",
        "nickname",
        "maskedPhone",
        "phoneVerified",
        "status",
        "createdAt",
        "lastLoginAt"
      ]
    },
    "AdminCustomerVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AdminCustomerVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "StaffDirectoryItemVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "username": {
          "type": "string",
          "maxLength": 64
        },
        "name": {
          "type": "string",
          "maxLength": 64
        },
        "phone": {
          "type": "string",
          "maxLength": 20,
          "nullable": true
        },
        "role": {
          "type": "string",
          "enum": [
            "ADMIN",
            "DISPATCHER",
            "TECHNICIAN",
            "KNOWLEDGE_MANAGER",
            "AUDITOR"
          ]
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "bindingStatus": {
          "type": "string",
          "enum": [
            "PENDING",
            "ACTIVE",
            null
          ],
          "nullable": true
        },
        "bindingApplicationPhone": {
          "type": "string",
          "maxLength": 20,
          "nullable": true,
          "description": "有效维修身份申请时填写的号码，来源独立于员工档案电话。"
        }
      },
      "required": [
        "id",
        "username",
        "name",
        "phone",
        "role",
        "status",
        "version",
        "bindingStatus",
        "bindingApplicationPhone"
      ]
    },
    "StaffProfileVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffDirectoryItemVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "StaffTokenVO": {
      "type": "object",
      "properties": {
        "accessToken": {
          "type": "string",
          "maxLength": 4096
        },
        "refreshToken": {
          "type": "string",
          "maxLength": 4096
        },
        "expiresIn": {
          "type": "integer",
          "minimum": 1
        },
        "staff": {
          "$ref": "#/components/schemas/StaffProfileVO"
        },
        "principalType": {
          "type": "string",
          "enum": [
            "STAFF"
          ]
        },
        "audience": {
          "type": "string",
          "enum": [
            "fixmate-admin"
          ]
        },
        "passwordChangeRequired": {
          "type": "boolean"
        }
      },
      "required": [
        "accessToken",
        "refreshToken",
        "expiresIn",
        "staff",
        "principalType",
        "audience",
        "passwordChangeRequired"
      ],
      "description": "STAFF域令牌，不得被微信CUSTOMER域接口接受。敏感幂等结果短期加密存储。"
    },
    "StaffLoginDTO": {
      "type": "object",
      "properties": {
        "username": {
          "type": "string",
          "minLength": 1,
          "maxLength": 64
        },
        "password": {
          "type": "string",
          "minLength": 1,
          "maxLength": 128,
          "format": "password"
        }
      },
      "required": [
        "username",
        "password"
      ],
      "additionalProperties": false
    },
    "StaffCreateDTO": {
      "type": "object",
      "properties": {
        "username": {
          "type": "string",
          "minLength": 3,
          "maxLength": 64
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "maxLength": 64
        },
        "phone": {
          "type": "string",
          "maxLength": 20,
          "nullable": true
        },
        "role": {
          "type": "string",
          "enum": [
            "ADMIN",
            "DISPATCHER",
            "TECHNICIAN",
            "KNOWLEDGE_MANAGER",
            "AUDITOR"
          ]
        },
        "initialPassword": {
          "type": "string",
          "minLength": 6,
          "maxLength": 6,
          "pattern": "^[0-9]{6}$",
          "format": "password",
          "nullable": true
        }
      },
      "required": [
        "username",
        "name",
        "role"
      ],
      "description": "仅ADMIN建档；TECHNICIAN允许无密码，生成不可登录随机hash；其他角色必须提供强密码，禁止默认密码。",
      "additionalProperties": false
    },
    "StaffStatusDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "status",
        "reason"
      ],
      "additionalProperties": false
    },
    "StaffInvitationCreateDTO": {
      "type": "object",
      "properties": {
        "expectedStaffVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "expectedStaffVersion"
      ],
      "additionalProperties": false
    },
    "StaffInvitationVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "staffId": {
          "type": "string",
          "pattern": "^[1-9][0-9]{0,18}$",
          "nullable": true,
          "x-java-type": "Long"
        },
        "status": {
          "type": "string",
          "enum": [
            "ISSUED",
            "CONSUMED",
            "REVOKED",
            "EXPIRED"
          ]
        },
        "expiresAt": {
          "type": "string",
          "format": "date-time"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "createdBy": {
          "$ref": "#/components/schemas/StaffActorVO"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "consumedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "consumedBindingId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "revokedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "revokedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "revocationReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "id",
        "staffId",
        "status",
        "expiresAt",
        "version",
        "createdBy",
        "createdAt",
        "consumedAt",
        "consumedBindingId",
        "revokedBy",
        "revokedAt",
        "revocationReason"
      ],
      "description": "ADMIN邀请管理读模型，不含code_hash或邀请码明文；消费关系通过bindingId定位申请。"
    },
    "StaffInvitationIssuedVO": {
      "type": "object",
      "properties": {
        "invitation": {
          "$ref": "#/components/schemas/StaffInvitationVO"
        },
        "invitationCode": {
          "type": "string",
          "minLength": 16,
          "maxLength": 64
        },
        "entryPage": {
          "type": "string",
          "maxLength": 128
        }
      },
      "required": [
        "invitation",
        "invitationCode",
        "entryPage"
      ],
      "description": "明文邀请码仅创建/同授权同键重放返回，短期密文保存，不写审计；邀请不授予角色。"
    },
    "NewTechnicianInvitationIssuedVO": {
      "type": "object",
      "properties": {
        "invitation": {
          "$ref": "#/components/schemas/StaffInvitationVO"
        },
        "invitationUrl": {
          "type": "string",
          "format": "uri",
          "nullable": true
        },
        "invitationCode": {
          "type": "string",
          "minLength": 43,
          "maxLength": 43,
          "nullable": true
        }
      },
      "required": [
        "invitation"
      ],
      "description": "链接和邀请码仅签发回执可见，两者只返回一种。"
    },
    "StaffBindingApplyDTO": {
      "type": "object",
      "properties": {
        "invitationCode": {
          "type": "string",
          "minLength": 16,
          "maxLength": 64
        },
        "applicantName": {
          "type": "string",
          "minLength": 1,
          "maxLength": 64
        },
        "applicantContactPhone": {
          "type": "string",
          "pattern": "^[0-9+() -]{6,20}$"
        }
      },
      "required": [
        "invitationCode",
        "applicantName",
        "applicantContactPhone"
      ],
      "additionalProperties": false
    },
    "StaffBindingVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "staffId": {
          "type": "string",
          "pattern": "^[1-9][0-9]{0,18}$",
          "nullable": true,
          "x-java-type": "Long"
        },
        "customerId": {
          "$ref": "#/components/schemas/IdString"
        },
        "staffName": {
          "type": "string",
          "maxLength": 64
        },
        "status": {
          "type": "string",
          "enum": [
            "PENDING",
            "ACTIVE",
            "REJECTED",
            "REVOKED"
          ]
        },
        "appliedAt": {
          "type": "string",
          "format": "date-time"
        },
        "confirmedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "invitationId": {
          "$ref": "#/components/schemas/IdString"
        },
        "applicantName": {
          "type": "string",
          "maxLength": 64
        },
        "applicantContactPhone": {
          "type": "string",
          "maxLength": 20
        },
        "contactPhoneVerified": {
          "type": "boolean",
          "enum": [
            false
          ]
        },
        "decisionReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "confirmedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "rejectedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "rejectedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "revokedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "revokedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        }
      },
      "required": [
        "id",
        "staffId",
        "customerId",
        "staffName",
        "status",
        "appliedAt",
        "confirmedAt",
        "version",
        "invitationId",
        "applicantName",
        "applicantContactPhone",
        "contactPhoneVerified",
        "decisionReason",
        "confirmedBy",
        "rejectedBy",
        "rejectedAt",
        "revokedBy",
        "revokedAt"
      ],
      "description": "申请人自行填写的姓名/联系号码仅作待核验声明，不回填customer_user或标为微信验证；本人和授权ADMIN可见；审核拒绝/撤销说明直接返回，不依赖另查审计。"
    },
    "StaffBindingVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffBindingVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "StaffBindingDecisionDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "AssignmentVO": {
      "type": "object",
      "properties": {
        "allocationSource": {
          "type": "string",
          "enum": [
            "DIRECT",
            "SELF_CLAIM"
          ]
        },
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "technicianId": {
          "$ref": "#/components/schemas/IdString"
        },
        "status": {
          "type": "string",
          "enum": [
            "PENDING",
            "ACTIVE",
            "RETURN_REQUESTED",
            "DECLINED",
            "REVOKED",
            "REASSIGNED",
            "FINISHED"
          ]
        },
        "assignedAt": {
          "type": "string",
          "format": "date-time"
        },
        "acceptedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "endedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "assignedBy": {
          "$ref": "#/components/schemas/StaffActorVO"
        },
        "endedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "endReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "allocationSource",
        "id",
        "workOrderId",
        "technicianId",
        "status",
        "assignedAt",
        "acceptedAt",
        "endedAt",
        "version",
        "assignedBy",
        "endedBy",
        "endReason"
      ]
    },
    "AssignmentCreateDTO": {
      "type": "object",
      "properties": {
        "technicianId": {
          "$ref": "#/components/schemas/IdString"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "nullable": true,
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "technicianId",
        "expectedWorkOrderVersion"
      ],
      "additionalProperties": false
    },
    "AssignmentActionDTO": {
      "type": "object",
      "properties": {
        "expectedAssignmentVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedAssignmentVersion",
        "expectedWorkOrderVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "WorkOrderCommandDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "work_order.version乐观锁，非报价业务versionNo。"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "WorkOrderCoordinationDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "resolution": {
          "type": "string",
          "enum": [
            "REQUOTE",
            "ABANDON_ADDITIONAL",
            "RESUME_REPAIR",
            "RETURN_TO_ASSIGNMENT",
            "CONTINUE_AFTER_SAFETY",
            "ESCALATE_TO_MANUAL",
            "CANCEL_REJECTED_QUOTE",
            "SCHEDULE_REWORK"
          ]
        },
        "reason": {
          "type": "string",
          "nullable": true,
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "resolution"
      ],
      "description": "只允许状态/安全/报价链守卫通过的动作；不是任意targetStatus。",
      "additionalProperties": false
    },
    "AcceptanceIssueDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "ManualTriageDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reviewNote": {
          "type": "string",
          "nullable": true,
          "minLength": 2,
          "maxLength": 1000,
          "description": "可选的补充说明。常规核对记录实际操作者、时间及两项确认结果。"
        },
        "informationConfirmed": {
          "type": "boolean",
          "enum": [
            true
          ]
        },
        "safetyReviewed": {
          "type": "boolean",
          "enum": [
            true
          ]
        },
        "expectedAddressVersion": {
          "type": "integer",
          "minimum": 0,
          "nullable": true,
          "description": "旧报修无私有服务联系快照时必填，须等于详情CURRENT_ADDRESS候选版本；地址改变返回409。"
        }
      },
      "required": [
        "expectedVersion",
        "informationConfirmed",
        "safetyReviewed"
      ],
      "description": "不修改用户原文，安全未解除/已有工单禁止，原版本核对完成才到TRIAGED。",
      "additionalProperties": false
    },
    "StaffServiceRecordVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "kind": {
          "type": "string",
          "enum": [
            "INSPECTION",
            "REPAIR"
          ]
        },
        "technicianId": {
          "$ref": "#/components/schemas/IdString"
        },
        "submittedBy": {
          "$ref": "#/components/schemas/IdString"
        },
        "submittedAt": {
          "type": "string",
          "format": "date-time"
        },
        "facts": {
          "type": "string",
          "maxLength": 4000
        },
        "summary": {
          "type": "string",
          "maxLength": 1000
        },
        "correctsRecordId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        }
      },
      "required": [
        "id",
        "workOrderId",
        "kind",
        "technicianId",
        "submittedBy",
        "submittedAt",
        "facts",
        "summary",
        "correctsRecordId"
      ],
      "description": "现场事实文本由人工提交；不得AI冒充；已提交不可覆盖。"
    },
    "ServiceRecordCreateDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "kind": {
          "type": "string",
          "enum": [
            "INSPECTION",
            "REPAIR"
          ]
        },
        "technicianId": {
          "$ref": "#/components/schemas/IdString"
        },
        "facts": {
          "type": "string",
          "minLength": 10,
          "maxLength": 4000
        },
        "summary": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        },
        "correctsRecordId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "kind",
        "technicianId",
        "facts",
        "summary"
      ],
      "additionalProperties": false
    },
    "ReworkSummaryVO": {
      "type": "object",
      "description": "返修任务详情中的脱敏摘要，仅提供给当前分派且参与上一轮完工的维修人员。",
      "properties": {
        "issueReason": {
          "type": "string",
          "nullable": true
        },
        "issueAt": {
          "type": "string",
          "format": "date-time"
        },
        "originalFault": {
          "type": "string",
          "nullable": true
        },
        "previousVisit": {
          "nullable": true,
          "allOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            }
          ]
        },
        "previousCompletedAt": {
          "type": "string",
          "format": "date-time"
        },
        "previousRepairSummary": {
          "type": "string",
          "nullable": true
        }
      }
    },
    "StaffTaskListItemVO": {
      "type": "object",
      "properties": {
        "requoteRequested": {
          "type": "boolean",
          "description": "客户拒绝报价且平台已要求当前维修人员重报。"
        },
        "reworkVisit": {
          "type": "boolean",
          "description": "当前任务属于验收异议后的返修。"
        },
        "reworkSummary": {
          "nullable": true,
          "allOf": [
            {
              "$ref": "#/components/schemas/ReworkSummaryVO"
            }
          ]
        },
        "preferredVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "visitPolicy": {
          "type": "string",
          "enum": [
            "LEGACY_PLATFORM",
            "CUSTOMER_SLOT"
          ]
        },
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string",
          "maxLength": 64
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "symptomSummary": {
          "type": "string",
          "maxLength": 120
        },
        "serviceArea": {
          "type": "string",
          "maxLength": 100,
          "nullable": true,
          "description": "服务端只拼接地址快照中独立city与district字段（市+区/县），不含province/street/detail/contact；任一缺失或无法确定为结构化区域则返回null并显示区域未提供，禁止截取或回退原始详细地址。"
        },
        "riskFlag": {
          "type": "boolean"
        },
        "appointmentStartAt": {
          "description": "CUSTOMER_SLOT改约待确认时仍为原已确认时段起点；无原预约证据为null，新请求从preferredVisit读取。",
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "assignment": {
          "$ref": "#/components/schemas/AssignmentVO"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "workOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "当前工单根版本；接单、拒接和退回原样提交expectedWorkOrderVersion，不读取完整客户资料来补版本。"
        },
        "allowedActions": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "ACCEPT_ASSIGNMENT",
              "DECLINE_ASSIGNMENT",
              "REQUEST_RETURN"
            ]
          },
          "description": "唯一任务动作集；按实时身份/分派/安全/接管及业务状态计算，写事务内再次核验，无动作返回空数组。"
        },
        "rescheduleVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        }
      },
      "required": [
        "preferredVisit",
        "visitPolicy",
        "id",
        "workOrderNo",
        "status",
        "applianceType",
        "symptomSummary",
        "serviceArea",
        "riskFlag",
        "appointmentStartAt",
        "assignment",
        "createdAt",
        "allowedActions",
        "workOrderVersion"
      ],
      "description": "仅当前本人PENDING/ACTIVE/RETURN_REQUESTED任务；接单前不含联系人/电话/详细地址/照片；待接任务详情使用tasks/{id}，不是完整工单详情。"
    },
    "StaffTaskListItemVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffTaskListItemVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "StaffParticipationVO": {
      "type": "object",
      "properties": {
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string",
          "maxLength": 64
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "participatedAt": {
          "type": "string",
          "format": "date-time"
        },
        "endedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "participationSummary": {
          "type": "string",
          "maxLength": 1000
        },
        "latestAssignmentResult": {
          "type": "string",
          "nullable": true,
          "description": "当前维修人员最近一次分派的结果。RESCHEDULE_DECLINED 表示接单后拒绝客户改约，该次分派已结束。无接单结果时为空。",
          "enum": [
            "DECLINED",
            "ACCEPTED",
            "RESCHEDULE_DECLINED",
            null
          ]
        },
        "cancellationFeeAmount": {
          "type": "string",
          "nullable": true,
          "description": "确认取消时需支付或已登记应付的上门费。"
        }
      },
      "required": [
        "workOrderId",
        "workOrderNo",
        "applianceType",
        "status",
        "participatedAt",
        "endedAt",
        "participationSummary"
      ],
      "description": "本人历史摘要，无客户姓名/电话/地址/图片或其他人内部备注；撤权/离职后拒绝。"
    },
    "StaffParticipationVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffParticipationVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "StaffWorkOrderDetailVO": {
      "type": "object",
      "properties": {
        "coordinationCause": {
          "type": "string",
          "nullable": true
        },
        "coordinationReason": {
          "type": "string",
          "nullable": true
        },
        "reworkTechnicianId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "reworkVisit": {
          "type": "boolean"
        },
        "preferredVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "visitPolicy": {
          "type": "string",
          "enum": [
            "LEGACY_PLATFORM",
            "CUSTOMER_SLOT"
          ]
        },
        "workOrder": {
          "$ref": "#/components/schemas/StaffWorkOrderProjectionVO"
        },
        "assignment": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/AssignmentVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "serviceRecords": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffServiceRecordVO"
          }
        },
        "allowedActions": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "ACCEPT_ASSIGNMENT",
              "DECLINE_ASSIGNMENT",
              "REQUEST_RETURN",
              "CONFIRM_VISIT",
              "DECLINE_RESCHEDULE",
              "START_INSPECTION",
              "SUBMIT_INSPECTION",
              "CREATE_QUOTE",
              "SUBMIT_QUOTE",
              "WITHDRAW_QUOTE",
              "RECORD_SERVICE",
              "CREATE_ADDITIONAL_QUOTE",
              "SUBMIT_COMPLETION",
              "ASSIGN",
              "APPOINT",
              "TAKEOVER",
              "COORDINATE",
              "CANCEL",
              "CLOSE_UNRESOLVED",
              "CONFIRM_APPOINTMENT",
              "ACKNOWLEDGE_SAFETY",
              "RESOLVE_SAFETY",
              "RESOLVE_TODO",
              "DECIDE_RETURN",
              "APPROVE_RETURN_TO_POOL"
            ]
          }
        },
        "allowedCoordinationResolutions": {
          "type": "array",
          "uniqueItems": true,
          "description": "当前工单和员工岗位可执行的协调结论。",
          "items": {
            "type": "string",
            "enum": [
              "REQUOTE",
              "ABANDON_ADDITIONAL",
              "RESUME_REPAIR",
              "RETURN_TO_ASSIGNMENT",
              "CONTINUE_AFTER_SAFETY",
              "ESCALATE_TO_MANUAL",
              "CANCEL_REJECTED_QUOTE",
              "SCHEDULE_REWORK"
            ]
          }
        },
        "appointmentVersion": {
          "type": "integer",
          "minimum": 0,
          "nullable": true
        },
        "repairRequestId": {
          "$ref": "#/components/schemas/IdString"
        },
        "repairNo": {
          "type": "string",
          "nullable": true
        },
        "safetyBlocked": {
          "type": "boolean",
          "nullable": true
        },
        "appointmentStatus": {
          "type": "string",
          "nullable": true
        },
        "assigneeName": {
          "type": "string",
          "nullable": true
        },
        "serviceContact": {
          "$ref": "#/components/schemas/ServiceContactVO"
        }
      },
      "required": [
        "preferredVisit",
        "visitPolicy",
        "workOrder",
        "assignment",
        "serviceRecords",
        "allowedActions",
        "allowedCoordinationResolutions",
        "appointmentVersion",
        "repairRequestId",
        "serviceContact"
      ],
      "description": "当前已接任务或管理业务范围详情；workOrder为无客户动作投影，外层allowedActions是本用例唯一工单动作集；报价子资源动作由其独立用例返回。"
    },
    "AppointmentCreateDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "startAt": {
          "type": "string",
          "format": "date-time"
        },
        "endAt": {
          "type": "string",
          "format": "date-time"
        },
        "note": {
          "type": "string",
          "maxLength": 1000
        },
        "confirmed": {
          "type": "boolean"
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "startAt",
        "endAt",
        "confirmed"
      ],
      "description": "start<end且开始不早于数据库时间；替换旧当前预约需事务取消，confirmed=false保持PENDING。",
      "additionalProperties": false
    },
    "AppointmentConfirmDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedAppointmentVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "expectedAppointmentVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "StaffAppointmentVO": {
      "type": "object",
      "properties": {
        "appointment": {
          "$ref": "#/components/schemas/AppointmentVO"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "appointment",
        "version"
      ]
    },
    "QuoteItemInputDTO": {
      "type": "object",
      "properties": {
        "feeType": {
          "$ref": "#/components/schemas/FeeType"
        },
        "serviceItemId": {
          "$ref": "#/components/schemas/IdString"
        },
        "pricingRuleId": {
          "$ref": "#/components/schemas/IdString"
        },
        "quantity": {
          "$ref": "#/components/schemas/Quantity"
        },
        "selectedUnitPrice": {
          "$ref": "#/components/schemas/MoneyAmount"
        },
        "pricingReason": {
          "type": "string",
          "maxLength": 1000
        },
        "itemName": {
          "type": "string",
          "maxLength": 200
        },
        "specification": {
          "type": "string",
          "maxLength": 200,
          "nullable": true
        },
        "partSourceNote": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "additionReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "feeType",
        "serviceItemId",
        "pricingRuleId",
        "quantity"
      ],
      "description": "selectedUnitPrice仅区间规则人工选价；固定价不允许更改；PART规格来源、OTHER依据、ADDITIONAL逐项追加原因由后端条件校验；不接收金额合计。",
      "additionalProperties": false
    },
    "StaffQuoteCreateDTO": {
      "type": "object",
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "quoteType": {
          "$ref": "#/components/schemas/QuoteType"
        },
        "previousQuoteId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "baseQuoteId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "serviceRecordId": {
          "$ref": "#/components/schemas/IdString"
        },
        "submissionSource": {
          "type": "string",
          "enum": [
            "TECHNICIAN_SELF",
            "DISPATCHER_PROXY",
            "ADMIN_TAKEOVER"
          ]
        },
        "changeReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "expiresAt": {
          "type": "string",
          "format": "date-time"
        },
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/QuoteItemInputDTO"
          },
          "minItems": 1,
          "maxItems": 100
        },
        "technicianId": {
          "$ref": "#/components/schemas/IdString"
        }
      },
      "required": [
        "expectedWorkOrderVersion",
        "quoteType",
        "serviceRecordId",
        "technicianId",
        "submissionSource",
        "expiresAt",
        "items"
      ],
      "additionalProperties": false,
      "description": "服务端以真实身份验证submissionSource和technicianId，不能信任自报来源；检测必须同工单已提交。INITIAL/ADDITIONAL及两条链由后端校验；同一工单锁内串行分配versionNo。 TECHNICIAN_SELF必须等于当前staffId，代理必须引用同工单有效检测及授权，不能信任前端自报。"
    },
    "StaffQuoteUpdateDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "changeReason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "expiresAt": {
          "type": "string",
          "format": "date-time"
        },
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/QuoteItemInputDTO"
          },
          "minItems": 1,
          "maxItems": 100
        }
      },
      "required": [
        "expectedVersion",
        "expectedWorkOrderVersion",
        "expiresAt",
        "items"
      ],
      "additionalProperties": false
    },
    "QuoteSubmitCommandDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "quote.version乐观锁；expectedWorkOrderVersion校验工单聚合。"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedContentHash": {
          "type": "string",
          "pattern": "^[a-f0-9]{64}$"
        }
      },
      "required": [
        "expectedVersion",
        "expectedWorkOrderVersion",
        "expectedContentHash"
      ],
      "additionalProperties": false
    },
    "QuoteOverrideRequestDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedContentHash": {
          "type": "string",
          "pattern": "^[a-f0-9]{64}$"
        },
        "overrideReason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "expectedContentHash",
        "overrideReason"
      ],
      "additionalProperties": false
    },
    "QuoteOverrideDecisionDTO": {
      "type": "object",
      "properties": {
        "expectedApprovalVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reviewComment": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedApprovalVersion",
        "reviewComment"
      ],
      "additionalProperties": false
    },
    "QuoteOverrideApprovalVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "quoteId": {
          "$ref": "#/components/schemas/IdString"
        },
        "requestedBy": {
          "$ref": "#/components/schemas/IdString"
        },
        "reviewedBy": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "quoteContentHash": {
          "type": "string",
          "pattern": "^[a-f0-9]{64}$"
        },
        "status": {
          "type": "string",
          "enum": [
            "PENDING",
            "APPROVED",
            "REJECTED",
            "INVALIDATED"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "overrideReason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        },
        "reviewComment": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "id",
        "quoteId",
        "requestedBy",
        "reviewedBy",
        "quoteContentHash",
        "status",
        "version",
        "overrideReason",
        "reviewComment"
      ]
    },
    "StaffQuoteVO": {
      "type": "object",
      "properties": {
        "quote": {
          "$ref": "#/components/schemas/QuoteVO"
        },
        "contentHash": {
          "type": "string",
          "pattern": "^[a-f0-9]{64}$"
        },
        "submissionSource": {
          "type": "string",
          "nullable": true,
          "enum": [
            "TECHNICIAN_SELF",
            "DISPATCHER_PROXY",
            "ADMIN_TAKEOVER"
          ]
        },
        "createdBy": {
          "$ref": "#/components/schemas/IdString"
        },
        "submittedBy": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "serviceRecordId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "approval": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/QuoteOverrideApprovalVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "allowedActions": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "EDIT",
              "SUBMIT",
              "WITHDRAW",
              "REQUEST_OVERRIDE",
              "APPROVE_OVERRIDE",
              "REJECT_OVERRIDE"
            ]
          }
        },
        "technicianId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$",
          "description": "本报价归属的实际维修人员；TECHNICIAN_SELF为本人，代理来源为被代理人，真实操作者另见createdBy/submittedBy。"
        }
      },
      "required": [
        "quote",
        "contentHash",
        "submissionSource",
        "technicianId",
        "createdBy",
        "submittedBy",
        "serviceRecordId",
        "approval",
        "allowedActions"
      ]
    },
    "QuoteSubmitResultVO": {
      "type": "object",
      "properties": {
        "submissionOutcome": {
          "type": "string",
          "enum": [
            "SUBMITTED",
            "APPROVAL_REQUIRED"
          ]
        },
        "quote": {
          "$ref": "#/components/schemas/StaffQuoteVO"
        }
      },
      "required": [
        "submissionOutcome",
        "quote"
      ],
      "description": "超区间报价须由提交方主动申请审批，本版审批通过后才能提交。APPROVAL_REQUIRED用于兼容既有操作回执。"
    },
    "SafetyEventStaffVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "status": {
          "type": "string",
          "enum": [
            "OPEN",
            "RESOLVED"
          ]
        },
        "matchedRules": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/SafetyRuleDetail"
          }
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "acknowledgedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "acknowledgedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "acknowledgementNote": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "resolvedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "resolvedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "resolutionBasis": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "id",
        "workOrderId",
        "status",
        "matchedRules",
        "version",
        "createdAt",
        "acknowledgedAt",
        "acknowledgedBy",
        "acknowledgementNote",
        "resolvedBy",
        "resolvedAt",
        "resolutionBasis"
      ],
      "description": "确认和解除分别返回操作者/时间/依据；OPEN未确认时确认字段null，RESOLVED必须有解除字段，由服务端跨字段校验。"
    },
    "SafetyEventStaffVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/SafetyEventStaffVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "BusinessTodoVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "kind": {
          "type": "string",
          "enum": [
            "URGE",
            "HUMAN_HANDOFF"
          ]
        },
        "status": {
          "type": "string",
          "enum": [
            "PENDING",
            "HANDLED",
            "INVALIDATED"
          ]
        },
        "reason": {
          "type": "string",
          "maxLength": 1000
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "handledBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "handledAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "handlingConclusion": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        }
      },
      "required": [
        "id",
        "workOrderId",
        "kind",
        "status",
        "reason",
        "createdAt",
        "version",
        "handledBy",
        "handledAt",
        "handlingConclusion"
      ],
      "description": "PENDING 的处理元数据均为 null。HANDLED 表示员工已处理。INVALIDATED 仅用于客户催办，handledBy 为 null，handledAt 与 handlingConclusion 记录系统失效时间和原因。"
    },
    "BusinessTodoVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/BusinessTodoVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "AdminFollowUpVO": {
      "type": "object",
      "properties": {
        "kind": {
          "type": "string",
          "enum": [
            "REPAIR_REVIEW",
            "CUSTOMER_CONFIRM",
            "DISPATCH",
            "APPOINTMENT",
            "QUOTE_CONFIRM",
            "ACCEPTANCE"
          ]
        },
        "targetId": {
          "$ref": "#/components/schemas/IdString"
        },
        "referenceNo": {
          "type": "string"
        },
        "waitingFor": {
          "type": "string",
          "enum": [
            "STAFF",
            "CUSTOMER",
            "TECHNICIAN"
          ]
        },
        "waitingSince": {
          "type": "string",
          "format": "date-time"
        },
        "categoryTotal": {
          "type": "integer",
          "format": "int64",
          "minimum": 1
        }
      },
      "required": [
        "kind",
        "targetId",
        "referenceNo",
        "waitingFor",
        "waitingSince",
        "categoryTotal"
      ]
    },
    "DashboardVO": {
      "type": "object",
      "properties": {
        "manualReviewCount": {
          "type": "integer",
          "minimum": 0
        },
        "unresolvedSafetyCount": {
          "type": "integer",
          "minimum": 0
        },
        "humanHandoffCount": {
          "type": "integer",
          "minimum": 0
        },
        "urgeCount": {
          "type": "integer",
          "minimum": 0
        },
        "assignmentCount": {
          "type": "integer",
          "minimum": 0
        },
        "quoteConfirmationCount": {
          "type": "integer",
          "minimum": 0
        },
        "overrideApprovalCount": {
          "type": "integer",
          "minimum": 0
        },
        "upcomingAppointmentCount": {
          "type": "integer",
          "minimum": 0
        },
        "missedAppointmentCount": {
          "type": "integer",
          "minimum": 0
        },
        "returnRequestCount": {
          "type": "integer",
          "minimum": 0
        },
        "requoteCount": {
          "type": "integer",
          "minimum": 0
        },
        "statusDistribution": {
          "type": "object",
          "additionalProperties": {
            "type": "integer",
            "minimum": 0
          }
        },
        "generatedAt": {
          "type": "string",
          "format": "date-time"
        },
        "aiEnabled": {
          "type": "boolean",
          "enum": [
            false
          ]
        },
        "followUps": {
          "type": "array",
          "description": "超时跟进，每类按等待时间取最早的五条。",
          "items": {
            "$ref": "#/components/schemas/AdminFollowUpVO"
          }
        }
      },
      "required": [
        "manualReviewCount",
        "unresolvedSafetyCount",
        "humanHandoffCount",
        "urgeCount",
        "assignmentCount",
        "quoteConfirmationCount",
        "overrideApprovalCount",
        "upcomingAppointmentCount",
        "missedAppointmentCount",
        "returnRequestCount",
        "requoteCount",
        "statusDistribution",
        "generatedAt",
        "aiEnabled"
      ],
      "description": "30秒轮询与手动刷新，包含工作台统计和只读超时跟进。"
    },
    "ServiceItemInputDTO": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "name": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 1000
        }
      },
      "required": [
        "code",
        "applianceType",
        "name"
      ],
      "additionalProperties": false
    },
    "ServiceItemStaffVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "code": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "name": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 1000
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "id",
        "code",
        "applianceType",
        "name",
        "description",
        "status",
        "version"
      ]
    },
    "ServiceItemStaffVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/ServiceItemStaffVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "ServiceItemUpdateDTO": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "name": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 1000
        },
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "expectedVersion",
        "code",
        "applianceType",
        "name"
      ],
      "additionalProperties": false
    },
    "ConfigStatusDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "status",
        "reason"
      ],
      "additionalProperties": false
    },
    "PricingRuleInputDTO": {
      "type": "object",
      "properties": {
        "serviceItemId": {
          "$ref": "#/components/schemas/IdString"
        },
        "feeType": {
          "$ref": "#/components/schemas/FeeType"
        },
        "clauseNo": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "ruleVersion": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "title": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 2000
        },
        "unit": {
          "type": "string",
          "maxLength": 32,
          "minLength": 1
        },
        "fixedPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "minPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "maxPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "refundPolicy": {
          "type": "string",
          "maxLength": 1000
        },
        "effectiveFrom": {
          "type": "string",
          "format": "date-time"
        },
        "effectiveTo": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        }
      },
      "required": [
        "serviceItemId",
        "feeType",
        "clauseNo",
        "ruleVersion",
        "title",
        "unit",
        "fixedPrice",
        "minPrice",
        "maxPrice",
        "refundPolicy",
        "effectiveFrom",
        "effectiveTo"
      ],
      "description": "固定价与区间二选一；min<=max；生效截止晚于开始；同范围生效规则不重叠，关联报价只用快照。",
      "additionalProperties": false
    },
    "PricingRuleStaffVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "serviceItemId": {
          "$ref": "#/components/schemas/IdString"
        },
        "feeType": {
          "$ref": "#/components/schemas/FeeType"
        },
        "clauseNo": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "ruleVersion": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "title": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 2000
        },
        "unit": {
          "type": "string",
          "maxLength": 32,
          "minLength": 1
        },
        "fixedPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "minPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "maxPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "refundPolicy": {
          "type": "string",
          "maxLength": 1000
        },
        "effectiveFrom": {
          "type": "string",
          "format": "date-time"
        },
        "effectiveTo": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "status": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "id",
        "serviceItemId",
        "feeType",
        "clauseNo",
        "ruleVersion",
        "title",
        "description",
        "unit",
        "fixedPrice",
        "minPrice",
        "maxPrice",
        "refundPolicy",
        "effectiveFrom",
        "effectiveTo",
        "status",
        "version"
      ]
    },
    "PricingRuleStaffVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/PricingRuleStaffVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "PricingRuleUpdateDTO": {
      "type": "object",
      "properties": {
        "serviceItemId": {
          "$ref": "#/components/schemas/IdString"
        },
        "feeType": {
          "$ref": "#/components/schemas/FeeType"
        },
        "clauseNo": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "ruleVersion": {
          "type": "string",
          "maxLength": 64,
          "minLength": 1
        },
        "title": {
          "type": "string",
          "maxLength": 200,
          "minLength": 1
        },
        "description": {
          "type": "string",
          "maxLength": 2000
        },
        "unit": {
          "type": "string",
          "maxLength": 32,
          "minLength": 1
        },
        "fixedPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "minPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "maxPrice": {
          "type": "string",
          "nullable": true,
          "pattern": "^[0-9]{1,10}\\.[0-9]{2}$"
        },
        "refundPolicy": {
          "type": "string",
          "maxLength": 1000
        },
        "effectiveFrom": {
          "type": "string",
          "format": "date-time"
        },
        "effectiveTo": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "serviceItemId",
        "feeType",
        "clauseNo",
        "ruleVersion",
        "title",
        "unit",
        "fixedPrice",
        "minPrice",
        "maxPrice",
        "refundPolicy",
        "effectiveFrom",
        "effectiveTo",
        "expectedVersion"
      ],
      "additionalProperties": false
    },
    "AuditSummaryVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "operatorType": {
          "type": "string",
          "enum": [
            "CUSTOMER",
            "STAFF",
            "SYSTEM"
          ]
        },
        "operatorId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "action": {
          "type": "string",
          "maxLength": 128
        },
        "resourceType": {
          "type": "string",
          "maxLength": 64
        },
        "resourceId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "reason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "traceId": {
          "type": "string",
          "maxLength": 64
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "required": [
        "id",
        "operatorType",
        "operatorId",
        "action",
        "resourceType",
        "resourceId",
        "reason",
        "traceId",
        "occurredAt"
      ],
      "description": "脱敏摘要，无原始凭据；明细只允许审计范围。"
    },
    "AuditSummaryVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AuditSummaryVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "AssignmentReturnDecisionDTO": {
      "type": "object",
      "properties": {
        "expectedAssignmentVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "approved": {
          "type": "boolean"
        },
        "replacementTechnicianId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedAssignmentVersion",
        "expectedWorkOrderVersion",
        "approved",
        "reason"
      ],
      "description": "批准须replacementTechnicianId并原子改派；拒绝恢复ACTIVE，禁止无负责人丢单。",
      "additionalProperties": false
    },
    "AdminWorkOrderListItemVO": {
      "type": "object",
      "properties": {
        "coordinationCause": {
          "type": "string",
          "nullable": true
        },
        "coordinationReason": {
          "type": "string",
          "nullable": true
        },
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string",
          "maxLength": 64
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "symptomSummary": {
          "type": "string",
          "maxLength": 120
        },
        "riskFlag": {
          "type": "boolean"
        },
        "technicianId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        },
        "appointmentStartAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        },
        "currentAssigneeId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$",
          "description": "权威当前分派的目标员工，包括待接候选；不代表已经接单。"
        },
        "currentAssigneeName": {
          "type": "string",
          "nullable": true
        },
        "assignmentStatus": {
          "type": "string",
          "nullable": true,
          "enum": [
            "PENDING",
            "ACTIVE",
            "RETURN_REQUESTED",
            "DECLINED",
            "REVOKED",
            "REASSIGNED",
            "FINISHED",
            null
          ]
        },
        "preferredVisit": {
          "nullable": true,
          "allOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            }
          ]
        },
        "appointmentStatus": {
          "nullable": true,
          "allOf": [
            {
              "$ref": "#/components/schemas/AppointmentStatus"
            }
          ]
        },
        "latestAssigneeName": {
          "type": "string",
          "nullable": true,
          "description": "接单进度队列中的当前分派对象，当前指针为空时取最近一次分派。"
        },
        "latestAssignmentStatus": {
          "type": "string",
          "nullable": true,
          "description": "接单进度队列中对应分派的状态，可展示拒接和已撤回等历史结果。"
        },
        "poolPublished": {
          "type": "boolean",
          "nullable": true,
          "description": "定向派单进度中，表示此工单当前已有大厅发布。"
        },
        "repairNo": {
          "type": "string",
          "nullable": true
        }
      },
      "required": [
        "currentAssigneeId",
        "currentAssigneeName",
        "assignmentStatus",
        "id",
        "workOrderNo",
        "status",
        "applianceType",
        "symptomSummary",
        "riskFlag",
        "technicianId",
        "appointmentStartAt",
        "createdAt"
      ]
    },
    "AdminWorkOrderListItemVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AdminWorkOrderListItemVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ]
    },
    "QuoteWithdrawCommandDTO": {
      "type": "object",
      "properties": {
        "expectedQuoteVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion",
          "description": "quote.version乐观锁；不是用户接受接口的versionNo。"
        },
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedQuoteVersion",
        "expectedWorkOrderVersion",
        "reason"
      ],
      "additionalProperties": false
    },
    "AuditDetailVO": {
      "type": "object",
      "required": [
        "summary",
        "beforeSummary",
        "afterSummary",
        "proxyTechnicianId"
      ],
      "properties": {
        "summary": {
          "$ref": "#/components/schemas/AuditSummaryVO"
        },
        "beforeSummary": {
          "type": "string",
          "nullable": true,
          "maxLength": 4000
        },
        "afterSummary": {
          "type": "string",
          "nullable": true,
          "maxLength": 4000
        },
        "proxyTechnicianId": {
          "type": "string",
          "nullable": true,
          "pattern": "^[1-9][0-9]{0,18}$"
        }
      }
    },
    "StaffOptionVO": {
      "type": "object",
      "required": [
        "id",
        "name",
        "activeTaskCount"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "name": {
          "type": "string",
          "maxLength": 64
        },
        "activeTaskCount": {
          "type": "integer",
          "minimum": 0
        }
      },
      "description": "仅ENABLED TECHNICIAN；不向调度选项泄露账户用户名/手机号或口令。"
    },
    "StaffWorkOrderProjectionVO": {
      "type": "object",
      "required": [
        "id",
        "workOrderNo",
        "status",
        "applianceType",
        "brandCode",
        "modelCode",
        "symptomDescription",
        "errorCodeText",
        "occurredAt",
        "mediaFiles",
        "contactName",
        "contactPhone",
        "addressText",
        "appointment",
        "technicianName",
        "currentQuote",
        "quoteHistory",
        "serviceRecords",
        "acceptance",
        "timeline",
        "version"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string"
        },
        "status": {
          "$ref": "#/components/schemas/WorkOrderStatus"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "brandCode": {
          "type": "string",
          "nullable": true
        },
        "modelCode": {
          "type": "string",
          "nullable": true
        },
        "symptomDescription": {
          "type": "string"
        },
        "errorCodeText": {
          "type": "string",
          "nullable": true,
          "description": "用户填写的故障码原文；来自关联的报修记录，未填时为 null"
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true,
          "description": "用户填写的故障发生时间；来自关联的报修记录，未填时为 null"
        },
        "mediaFiles": {
          "type": "array",
          "description": "AUDITOR和终态为空；微信技师须ACTIVE当前分派且服务联系已确认才签发5分钟URL；按上传顺序，无照片为空数组。",
          "items": {
            "$ref": "#/components/schemas/FileAssetVO"
          }
        },
        "contactName": {
          "description": "无履约披露权限时为空字符串，禁止回填原用户联系方式。",
          "type": "string"
        },
        "contactPhone": {
          "description": "无履约披露权限时为空字符串，禁止回填原用户联系方式。",
          "type": "string"
        },
        "addressText": {
          "description": "无履约披露权限时为空字符串，禁止回填原用户联系方式。",
          "type": "string"
        },
        "appointment": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/AppointmentVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "technicianName": {
          "type": "string",
          "nullable": true
        },
        "currentQuote": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/QuoteVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "quoteHistory": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/QuoteVO"
          }
        },
        "serviceRecords": {
          "description": "兼容字段，员工投影始终为空；完整服务事实仅由StaffWorkOrderDetailVO.serviceRecords返回。",
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/ServiceRecordVO"
          }
        },
        "acceptance": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/AcceptanceVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "timeline": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/TimelineEventVO"
          }
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "additionalProperties": false,
      "description": "专用只读数据投影，无客户allowedActions；动作仅由外层当前用例allowedActions控制。"
    },
    "RepairRequestSnapshotVO": {
      "type": "object",
      "required": [
        "id",
        "repairNo",
        "status",
        "applianceType",
        "brandId",
        "brandCode",
        "modelId",
        "modelText",
        "serialNumber",
        "symptomDescription",
        "errorCodeText",
        "occurredAt",
        "mediaFileIds",
        "mediaFiles",
        "safetyAnswers",
        "addressId",
        "coverageStatus",
        "workOrderId",
        "version",
        "createdAt"
      ],
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "repairNo": {
          "type": "string"
        },
        "status": {
          "$ref": "#/components/schemas/RepairRequestStatus"
        },
        "applianceType": {
          "type": "string",
          "nullable": true,
          "description": "草稿阶段可为 null",
          "enum": [
            "AIR_CONDITIONER",
            "REFRIGERATOR",
            "WASHING_MACHINE",
            "AIR_SOURCE_HEAT_PUMP",
            null
          ]
        },
        "brandId": {
          "type": "string",
          "nullable": true
        },
        "brandCode": {
          "type": "string",
          "nullable": true
        },
        "modelId": {
          "type": "string",
          "nullable": true
        },
        "modelCode": {
          "type": "string",
          "nullable": true
        },
        "modelText": {
          "type": "string",
          "nullable": true
        },
        "serialNumber": {
          "type": "string",
          "nullable": true
        },
        "symptomDescription": {
          "type": "string",
          "nullable": true
        },
        "errorCodeText": {
          "type": "string",
          "nullable": true
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "mediaFileIds": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "mediaFiles": {
          "type": "array",
          "description": "该报修关联的故障照片，按上传顺序；无照片时为空数组。url 为 5 分钟短时签名地址",
          "items": {
            "$ref": "#/components/schemas/FileAssetVO"
          }
        },
        "safetyAnswers": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/SafetyAnswersDTO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "addressId": {
          "type": "string",
          "nullable": true
        },
        "coverageStatus": {
          "type": "string",
          "nullable": true,
          "description": "草稿阶段可为 null；有型号后取型号覆盖状态",
          "enum": [
            "REGISTERED",
            "PENDING_SOURCE",
            "COVERED",
            "CONFLICTED",
            "DISABLED",
            null
          ]
        },
        "workOrderId": {
          "type": "string",
          "nullable": true
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "additionalProperties": false,
      "description": "专用只读数据投影，无客户allowedActions；动作仅由外层当前用例allowedActions控制。"
    },
    "StaffActorVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "name": {
          "type": "string",
          "maxLength": 64
        }
      },
      "required": [
        "id",
        "name"
      ],
      "additionalProperties": false
    },
    "StaffInvitationVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/StaffInvitationVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ],
      "additionalProperties": false
    },
    "AssignmentEventVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "assignmentId": {
          "$ref": "#/components/schemas/IdString"
        },
        "eventType": {
          "type": "string",
          "enum": [
            "ASSIGNED",
            "ACCEPTED",
            "DECLINED",
            "RETURN_REQUESTED",
            "RETURN_REJECTED",
            "REASSIGNED",
            "REVOKED",
            "FINISHED"
          ]
        },
        "operator": {
          "$ref": "#/components/schemas/StaffActorVO"
        },
        "reason": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "required": [
        "id",
        "assignmentId",
        "eventType",
        "operator",
        "reason",
        "occurredAt"
      ],
      "additionalProperties": false,
      "description": "从已有结构化审计记录按assignmentId生成只读事件，不让前端解析audit_log原始JSON。"
    },
    "AssignmentHistoryDetailVO": {
      "type": "object",
      "properties": {
        "assignment": {
          "$ref": "#/components/schemas/AssignmentVO"
        },
        "events": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AssignmentEventVO"
          }
        }
      },
      "required": [
        "assignment",
        "events"
      ],
      "additionalProperties": false
    },
    "AssignmentVOPageVO": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/AssignmentVO"
          }
        },
        "page": {
          "type": "integer",
          "minimum": 1
        },
        "size": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100
        },
        "total": {
          "type": "integer",
          "format": "int64",
          "minimum": 0
        }
      },
      "required": [
        "items",
        "page",
        "size",
        "total"
      ],
      "additionalProperties": false
    },
    "ServiceContactConfirmationDTO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "expectedVersion",
        "expectedAddressVersion",
        "reviewNote"
      ],
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedAddressVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reviewNote": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "description": "补齐含紧急工单的旧报修私有联系快照，不解除安全、不改变业务状态。"
    },
    "ServiceContactVO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "contactName",
        "contactPhone",
        "addressText",
        "contactSource",
        "phoneMasked",
        "addressVersion"
      ],
      "properties": {
        "contactName": {
          "type": "string",
          "nullable": true,
          "maxLength": 64
        },
        "contactPhone": {
          "type": "string",
          "nullable": true,
          "maxLength": 20
        },
        "addressText": {
          "type": "string",
          "nullable": true,
          "maxLength": 1000
        },
        "contactSource": {
          "type": "string",
          "enum": [
            "SUBMISSION_SNAPSHOT",
            "MANUAL_REVIEW_SNAPSHOT",
            "CURRENT_ADDRESS",
            "UNAVAILABLE"
          ]
        },
        "phoneMasked": {
          "type": "boolean"
        },
        "addressVersion": {
          "type": "integer",
          "nullable": true,
          "minimum": 0
        }
      },
      "description": "受理联系信息；CURRENT_ADDRESS是遗留候选，须版本化人工核对。无披露权限（含AUDITOR和终态）时姓名、电话、详细地址及候选地址版本均为null，phoneMasked=true；未确认联系的微信技师不得回退原用户资料。"
    },
    "AdminRepairRequestDetailVO": {
      "type": "object",
      "properties": {
        "repairRequest": {
          "$ref": "#/components/schemas/RepairRequestSnapshotVO"
        },
        "manualReviewedBy": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/StaffActorVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "manualReviewedAt": {
          "type": "string",
          "format": "date-time",
          "nullable": true
        },
        "manualReviewNote": {
          "type": "string",
          "maxLength": 1000,
          "nullable": true
        },
        "allowedActions": {
          "type": "array",
          "uniqueItems": true,
          "items": {
            "type": "string",
            "enum": [
              "MANUAL_TRIAGE",
              "CONFIRM_SERVICE_CONTACT"
            ]
          }
        },
        "serviceContact": {
          "$ref": "#/components/schemas/ServiceContactVO"
        }
      },
      "required": [
        "repairRequest",
        "manualReviewedBy",
        "manualReviewedAt",
        "manualReviewNote",
        "allowedActions",
        "serviceContact"
      ],
      "additionalProperties": false,
      "description": "AUDITOR只读，allowedActions必须为空；原始报修及人工核对依据在授权审计范围可查，无openid/session或无关PII。"
    },
    "StaffPasswordChangeDTO": {
      "type": "object",
      "properties": {
        "currentPassword": {
          "type": "string",
          "minLength": 1,
          "maxLength": 128,
          "format": "password"
        },
        "newPassword": {
          "type": "string",
          "minLength": 6,
          "maxLength": 6,
          "pattern": "^[0-9]{6}$",
          "format": "password"
        }
      },
      "required": [
        "currentPassword",
        "newPassword"
      ],
      "additionalProperties": false,
      "description": "新密码恰好6位ASCII数字且不同于当前密码；原密码错误统一401。成功递增credentialVersion并撤销全部Web会话，需要重新登录。"
    },
    "StaffPasswordResetDTO": {
      "type": "object",
      "properties": {
        "expectedVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "temporaryPassword": {
          "type": "string",
          "minLength": 6,
          "maxLength": 6,
          "pattern": "^[0-9]{6}$",
          "format": "password"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      },
      "required": [
        "expectedVersion",
        "temporaryPassword",
        "reason"
      ],
      "additionalProperties": false,
      "description": "仅另一ADMIN核实后重置，不允许自重置；成功撤销目标全部Web会话并置passwordChangeRequired。密码不入响应/审计。"
    },
    "StaffAccessTokenClaims": {
      "type": "object",
      "properties": {
        "iss": {
          "type": "string",
          "enum": [
            "fixmate"
          ]
        },
        "sub": {
          "$ref": "#/components/schemas/IdString"
        },
        "aud": {
          "type": "string",
          "enum": [
            "fixmate-admin"
          ]
        },
        "principalType": {
          "type": "string",
          "enum": [
            "STAFF"
          ]
        },
        "tokenUse": {
          "type": "string",
          "enum": [
            "ACCESS"
          ]
        },
        "jti": {
          "type": "string",
          "minLength": 16
        },
        "familyId": {
          "type": "string",
          "minLength": 16
        },
        "credentialVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "iat": {
          "type": "integer",
          "format": "int64"
        },
        "exp": {
          "type": "integer",
          "format": "int64"
        }
      },
      "required": [
        "iss",
        "sub",
        "aud",
        "principalType",
        "tokenUse",
        "jti",
        "familyId",
        "credentialVersion",
        "iat",
        "exp"
      ],
      "additionalProperties": false,
      "description": "JWT声明规范，不是额外令牌签发端点；STAFF不得作为CUSTOMER使用，Refresh同域但tokenUse=REFRESH且不可代替Access。签名算法固定允许列表，不信token声明选择任意算法。"
    },
    "UpdateCurrentUserDTO": {
      "type": "object",
      "required": [
        "nickname"
      ],
      "additionalProperties": false,
      "properties": {
        "nickname": {
          "type": "string",
          "minLength": 1,
          "maxLength": 64,
          "pattern": ".*\\S.*"
        },
        "avatarFileId": {
          "$ref": "#/components/schemas/IdString"
        }
      }
    },
    "JobPoolPublishDTO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "expectedWorkOrderVersion"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedAssignmentVersion": {
          "allOf": [
            {
              "$ref": "#/components/schemas/NonNegativeVersion"
            }
          ],
          "description": "批准师傅退回并发布到大厅时提交原分派版本。退回和发布在同一事务完成。"
        },
        "reason": {
          "type": "string",
          "nullable": true,
          "minLength": 2,
          "maxLength": 1000
        }
      }
    },
    "JobPoolWithdrawDTO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "expectedWorkOrderVersion",
        "expectedPublicationVersion",
        "reason"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedPublicationVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "reason": {
          "type": "string",
          "minLength": 2,
          "maxLength": 1000
        }
      }
    },
    "JobPoolClaimDTO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "expectedWorkOrderVersion",
        "expectedPublicationVersion"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "expectedPublicationVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      }
    },
    "JobPoolEntryVO": {
      "type": "object",
      "description": "抢单前脱敏摘要；不含客户ID、姓名、电话、细地址、照片。仅当前有效发布可读取。",
      "properties": {
        "preferredVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "visitPolicy": {
          "type": "string",
          "enum": [
            "LEGACY_PLATFORM",
            "CUSTOMER_SLOT"
          ]
        },
        "allowedActions": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "CLAIM_JOB"
            ]
          }
        },
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderNo": {
          "type": "string"
        },
        "applianceType": {
          "$ref": "#/components/schemas/ApplianceType"
        },
        "symptomSummary": {
          "type": "string"
        },
        "serviceAreaText": {
          "type": "string",
          "nullable": true
        },
        "publishedAt": {
          "type": "string",
          "format": "date-time"
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "workOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        }
      },
      "required": [
        "preferredVisit",
        "visitPolicy",
        "allowedActions",
        "id",
        "workOrderId",
        "workOrderNo",
        "applianceType",
        "symptomSummary",
        "serviceAreaText",
        "publishedAt",
        "version",
        "workOrderVersion"
      ]
    },
    "JobPoolPublicationVO": {
      "type": "object",
      "properties": {
        "id": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "status": {
          "type": "string",
          "enum": [
            "PUBLISHED",
            "CLAIMED",
            "WITHDRAWN"
          ]
        },
        "version": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "workOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "publishedAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "required": [
        "id",
        "workOrderId",
        "status",
        "version",
        "workOrderVersion",
        "publishedAt"
      ]
    },
    "JobPoolEntryVOPageVO": {
      "type": "object",
      "required": [
        "items",
        "page",
        "size",
        "total"
      ],
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/JobPoolEntryVO"
          }
        },
        "page": {
          "type": "integer"
        },
        "size": {
          "type": "integer"
        },
        "total": {
          "type": "integer",
          "format": "int64"
        }
      }
    },
    "PreferredVisitVO": {
      "type": "object",
      "required": [
        "startAt",
        "endAt"
      ],
      "properties": {
        "startAt": {
          "type": "string",
          "format": "date-time"
        },
        "endAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "description": "用户期望时段；接单确认前不等于正式预约。"
    },
    "PreferredVisitDTO": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "expectedWorkOrderVersion",
        "startAt",
        "endAt"
      ],
      "properties": {
        "expectedWorkOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "startAt": {
          "type": "string",
          "format": "date-time"
        },
        "endAt": {
          "type": "string",
          "format": "date-time"
        }
      }
    },
    "PreferredVisitResultVO": {
      "type": "object",
      "required": [
        "workOrderId",
        "workOrderVersion",
        "visitPolicy",
        "preferredVisit",
        "allowedActions"
      ],
      "properties": {
        "workOrderId": {
          "$ref": "#/components/schemas/IdString"
        },
        "workOrderVersion": {
          "$ref": "#/components/schemas/NonNegativeVersion"
        },
        "visitPolicy": {
          "type": "string",
          "enum": [
            "LEGACY_PLATFORM",
            "CUSTOMER_SLOT"
          ]
        },
        "preferredVisit": {
          "oneOf": [
            {
              "$ref": "#/components/schemas/PreferredVisitVO"
            },
            {
              "type": "object",
              "nullable": true,
              "enum": [
                null
              ]
            }
          ]
        },
        "allowedActions": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "SELECT_PREFERRED_VISIT"
            ]
          }
        }
      }
    }
  }
} as const;
