ALTER TABLE staff_invitation DROP FOREIGN KEY fk_invitation_staff;
ALTER TABLE staff_invitation MODIFY COLUMN staff_id BIGINT NULL;
ALTER TABLE staff_invitation ADD CONSTRAINT fk_invitation_staff FOREIGN KEY(staff_id) REFERENCES staff_user(id);

ALTER TABLE staff_wechat_binding DROP FOREIGN KEY fk_binding_staff;
ALTER TABLE staff_wechat_binding MODIFY COLUMN staff_id BIGINT NULL;
ALTER TABLE staff_wechat_binding ADD CONSTRAINT fk_binding_staff FOREIGN KEY(staff_id) REFERENCES staff_user(id);
